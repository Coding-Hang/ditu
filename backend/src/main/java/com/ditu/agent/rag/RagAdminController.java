package com.ditu.agent.rag;

import com.ditu.agent.auth.SecurityUtils;
import com.ditu.agent.common.ApiResponse;
import com.ditu.agent.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端 RAG 知识库接口。
 *
 * <p>只有 ADMIN 和 RAG_ADMIN 可以维护知识库；用户端只通过 Agent 检索结果间接看到摘要。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/rag")
@PreAuthorize("hasAnyRole('ADMIN','RAG_ADMIN')")
public class RagAdminController {
  private final RagService ragService;

  public RagAdminController(RagService ragService) {
    this.ragService = ragService;
  }

  @GetMapping("/collections")
  public ApiResponse<List<RagService.CollectionDto>> collections() {
    return ApiResponse.ok(ragService.listCollections());
  }

  @PostMapping("/collections")
  public ApiResponse<RagService.CollectionDto> createCollection(@Valid @RequestBody CreateCollectionRequest request) {
    return ApiResponse.ok(ragService.createCollection(request.scope(), request.ownerUserId(), request.name(),
        request.description(), SecurityUtils.currentUser().id()));
  }

  @PostMapping("/collections/{collectionId}/documents")
  public ApiResponse<RagService.DocumentDto> upload(@PathVariable Long collectionId,
                                                   @RequestPart("file") MultipartFile file,
                                                   @RequestPart(value = "metadata", required = false) String metadata) {
    return ApiResponse.ok(ragService.uploadDocument(collectionId, file, metadata, SecurityUtils.currentUser().id()));
  }

  @GetMapping("/collections/{collectionId}/documents")
  public ApiResponse<PageResponse<RagService.DocumentDto>> documents(@PathVariable Long collectionId,
                                                                    @RequestParam(defaultValue = "1") int page,
                                                                    @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.ok(ragService.pageDocuments(collectionId, page, pageSize));
  }

  @PostMapping("/documents/{documentId}/reindex")
  public ApiResponse<Void> reindex(@PathVariable Long documentId) {
    ragService.reindex(documentId, SecurityUtils.currentUser().id());
    return ApiResponse.ok(null);
  }

  @PostMapping("/documents/{documentId}/disable")
  public ApiResponse<Void> disable(@PathVariable Long documentId) {
    ragService.disable(documentId, SecurityUtils.currentUser().id());
    return ApiResponse.ok(null);
  }

  public record CreateCollectionRequest(@NotBlank String scope, Long ownerUserId, @NotBlank String name,
                                        String description) {
  }
}
