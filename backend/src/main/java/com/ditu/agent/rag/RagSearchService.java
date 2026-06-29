package com.ditu.agent.rag;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * RAG 权限过滤检索服务。
 *
 * <p>检索 SQL 必须同时允许 GLOBAL 知识和当前用户 USER 知识，严禁召回其他用户的专属知识。</p>
 */
@Service
public class RagSearchService {
  private final JdbcTemplate jdbcTemplate;

  public RagSearchService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Map<String, Object>> search(Long currentUserId, String query, int topK) {
    String like = "%" + (query == null ? "" : query.strip()) + "%";
    return jdbcTemplate.queryForList("""
        /* RAG-006/SEC-003: 权限条件必须包含 GLOBAL 或当前用户 USER 知识库，防止跨用户知识泄露。 */
        SELECT rc.id AS "collectionId", d.id AS "documentId", c.id AS "chunkId",
               d.file_name AS "documentName", 1.0 AS score,
               left(c.content, 240) AS snippet
        FROM rag_chunk c
        JOIN rag_collection rc ON rc.id = c.collection_id
        JOIN rag_document d ON d.id = c.document_id
        WHERE rc.enabled = true
          AND d.status = 'READY'
          AND (
            rc.scope = 'GLOBAL'
            OR (rc.scope = 'USER' AND rc.owner_user_id = ?)
          )
          AND (? = '%%' OR c.content ILIKE ?)
        ORDER BY c.id DESC
        LIMIT ?
        """, currentUserId, like, like, topK);
  }
}
