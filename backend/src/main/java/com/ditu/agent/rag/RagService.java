package com.ditu.agent.rag;

import com.ditu.agent.common.BusinessException;
import com.ditu.agent.common.ErrorCode;
import com.ditu.agent.common.PageResponse;
import com.ditu.agent.infra.AuditService;
import com.ditu.agent.infra.DituProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * RAG 知识库管理服务。
 *
 * <p>文档上传后立即执行轻量解析、切片和 mock embedding 入库，状态为 READY 的文档才参与检索。</p>
 */
@Service
public class RagService {
  private final JdbcTemplate jdbcTemplate;
  private final AuditService auditService;
  private final DituProperties properties;

  public RagService(JdbcTemplate jdbcTemplate, AuditService auditService, DituProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.auditService = auditService;
    this.properties = properties;
  }

  @Transactional
  public CollectionDto createCollection(String scope, Long ownerUserId, String name, String description, Long actorUserId) {
    Long id = jdbcTemplate.queryForObject("""
        INSERT INTO rag_collection(scope, owner_user_id, name, description, created_by)
        VALUES (?, ?, ?, ?, ?)
        RETURNING id
        """, Long.class, scope, ownerUserId, name, description, actorUserId);
    auditService.record(actorUserId, "RAG_COLLECTION_CREATE", "RAG_COLLECTION", id,
        Map.of("scope", scope, "ownerUserId", ownerUserId == null ? "" : ownerUserId, "name", name));
    return getCollection(id);
  }

  public List<CollectionDto> listCollections() {
    return jdbcTemplate.query("""
        /* RAG-001/RAG-002: 管理端展示通用和用户专属知识库，USER 知识必须携带 owner_user_id。 */
        SELECT id, scope, owner_user_id, name, description, enabled, created_at, updated_at
        FROM rag_collection
        ORDER BY id DESC
        """, (rs, rowNum) -> new CollectionDto(rs.getLong("id"), rs.getString("scope"),
        rs.getObject("owner_user_id") == null ? null : rs.getLong("owner_user_id"), rs.getString("name"),
        rs.getString("description"), rs.getBoolean("enabled"), rs.getObject("created_at", OffsetDateTime.class),
        rs.getObject("updated_at", OffsetDateTime.class)));
  }

  @Transactional
  public DocumentDto uploadDocument(Long collectionId, MultipartFile file, String metadata, Long actorUserId) {
    validateFile(file);
    try {
      byte[] bytes = file.getBytes();
      String checksum = checksum(bytes);
      Path target = storeFile(collectionId, checksum, file.getOriginalFilename(), bytes);
      Long documentId = jdbcTemplate.queryForObject("""
          INSERT INTO rag_document(collection_id, file_name, file_path, mime_type, checksum_sha256, status,
                                   metadata, uploaded_by)
          VALUES (?, ?, ?, ?, ?, 'PARSING', ?::jsonb, ?)
          RETURNING id
          """, Long.class, collectionId, file.getOriginalFilename(), target.toString(), file.getContentType(),
          checksum, metadata == null || metadata.isBlank() ? "{}" : metadata, actorUserId);
      ingest(documentId, collectionId, file.getOriginalFilename(), bytes);
      auditService.record(actorUserId, "RAG_DOCUMENT_UPLOAD", "RAG_DOCUMENT", documentId,
          Map.of("collectionId", collectionId, "fileName", file.getOriginalFilename()));
      return getDocument(documentId);
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.RAG_DOCUMENT_FAILED, "文档保存失败");
    }
  }

  @Transactional
  public void reindex(Long documentId, Long actorUserId) {
    DocumentDto doc = getDocument(documentId);
    jdbcTemplate.update("DELETE FROM rag_chunk WHERE document_id = ?", documentId);
    Path path = Path.of(doc.filePath());
    try {
      ingest(documentId, doc.collectionId(), doc.fileName(), Files.readAllBytes(path));
      auditService.record(actorUserId, "RAG_DOCUMENT_REINDEX", "RAG_DOCUMENT", documentId,
          Map.of("collectionId", doc.collectionId()));
    } catch (IOException ex) {
      jdbcTemplate.update("UPDATE rag_document SET status = 'FAILED', updated_at = now() WHERE id = ?", documentId);
      throw new BusinessException(ErrorCode.RAG_DOCUMENT_FAILED, "文档重建索引失败");
    }
  }

  @Transactional
  public void disable(Long documentId, Long actorUserId) {
    jdbcTemplate.update("""
        /* RAG-008: 禁用文档保留历史切片，但状态不再参与检索。 */
        UPDATE rag_document SET status = 'DISABLED', updated_at = now() WHERE id = ?
        """, documentId);
    auditService.record(actorUserId, "RAG_DOCUMENT_DISABLE", "RAG_DOCUMENT", documentId, Map.of());
  }

  public PageResponse<DocumentDto> pageDocuments(Long collectionId, int page, int pageSize) {
    Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM rag_document WHERE collection_id = ?", Long.class,
        collectionId);
    var records = jdbcTemplate.query("""
        /* RAG-003/RAG-008: 管理端不返回 file_path，避免服务器路径泄露给前端。 */
        SELECT id, collection_id, file_name, file_path, mime_type, checksum_sha256, status, created_at, updated_at
        FROM rag_document
        WHERE collection_id = ?
        ORDER BY id DESC
        LIMIT ? OFFSET ?
        """, this::mapDocument, collectionId, pageSize, (page - 1) * pageSize);
    return new PageResponse<>(records.stream().map(DocumentDto::withoutPath).toList(), page, pageSize,
        total == null ? 0 : total);
  }

  private void ingest(Long documentId, Long collectionId, String fileName, byte[] bytes) {
    jdbcTemplate.update("INSERT INTO rag_ingest_job(document_id, status, started_at) VALUES (?, 'RUNNING', now())",
        documentId);
    String text = extractText(fileName, bytes);
    List<String> chunks = chunk(text);
    for (int i = 0; i < chunks.size(); i++) {
      String content = chunks.get(i);
      jdbcTemplate.update("""
          /* RAG-004/RAG-005: 切片保存 mock embedding，生产可替换为真实 Embedding 客户端。 */
          INSERT INTO rag_chunk(document_id, collection_id, chunk_index, content, content_hash, embedding)
          VALUES (?, ?, ?, ?, ?, ?::vector)
          """, documentId, collectionId, i, content, checksum(content.getBytes(StandardCharsets.UTF_8)), zeroVector());
    }
    jdbcTemplate.update("UPDATE rag_document SET status = 'READY', updated_at = now() WHERE id = ?", documentId);
    jdbcTemplate.update("""
        UPDATE rag_ingest_job SET status = 'COMPLETED', completed_at = now()
        WHERE document_id = ? AND status = 'RUNNING'
        """, documentId);
  }

  private void validateFile(MultipartFile file) {
    String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
    boolean allowed = name.endsWith(".pdf") || name.endsWith(".docx") || name.endsWith(".txt") || name.endsWith(".md");
    if (!allowed || file.getSize() > 20L * 1024 * 1024) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "仅支持 20MB 内的 PDF、DOCX、TXT、MD 文件");
    }
  }

  private Path storeFile(Long collectionId, String checksum, String fileName, byte[] bytes) throws IOException {
    Path dir = Path.of(properties.fileStorageRoot(), "rag", String.valueOf(collectionId));
    Files.createDirectories(dir);
    Path target = dir.resolve(checksum + "-" + fileName);
    Files.write(target, bytes);
    return target;
  }

  private String extractText(String fileName, byte[] bytes) {
    String lower = fileName.toLowerCase();
    if (lower.endsWith(".txt") || lower.endsWith(".md")) {
      return new String(bytes, StandardCharsets.UTF_8);
    }
    return "文件 " + fileName + " 已上传。请在生产环境接入 PDF/DOCX 解析器后生成完整知识切片。";
  }

  private List<String> chunk(String text) {
    List<String> chunks = new ArrayList<>();
    String normalized = text == null || text.isBlank() ? "空文档" : text.strip();
    for (int i = 0; i < normalized.length(); i += 500) {
      chunks.add(normalized.substring(i, Math.min(normalized.length(), i + 500)));
    }
    return chunks.isEmpty() ? List.of("空文档") : chunks;
  }

  private String zeroVector() {
    return "[" + "0,".repeat(Math.max(0, properties.embedding().dimension() - 1)) + "0]";
  }

  private String checksum(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (Exception ex) {
      throw new IllegalStateException("文档校验和计算失败", ex);
    }
  }

  private CollectionDto getCollection(Long id) {
    return listCollections().stream().filter(c -> c.id().equals(id)).findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
  }

  private DocumentDto getDocument(Long documentId) {
    return jdbcTemplate.query("""
        SELECT id, collection_id, file_name, file_path, mime_type, checksum_sha256, status, created_at, updated_at
        FROM rag_document WHERE id = ?
        """, this::mapDocument, documentId).stream().findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));
  }

  private DocumentDto mapDocument(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
    return new DocumentDto(rs.getLong("id"), rs.getLong("collection_id"), rs.getString("file_name"),
        rs.getString("file_path"), rs.getString("mime_type"), rs.getString("checksum_sha256"),
        rs.getString("status"), rs.getObject("created_at", OffsetDateTime.class),
        rs.getObject("updated_at", OffsetDateTime.class));
  }

  public record CollectionDto(Long id, String scope, Long ownerUserId, String name, String description, boolean enabled,
                              OffsetDateTime createdAt, OffsetDateTime updatedAt) {
  }

  public record DocumentDto(Long id, Long collectionId, String fileName, String filePath, String mimeType,
                            String checksumSha256, String status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public DocumentDto withoutPath() {
      return new DocumentDto(id, collectionId, fileName, null, mimeType, checksumSha256, status, createdAt, updatedAt);
    }
  }
}
