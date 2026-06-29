package com.ditu.agent.chat;

import com.ditu.agent.auth.SecurityUtils;
import com.ditu.agent.chat.ChatDtos.ConversationDto;
import com.ditu.agent.chat.ChatDtos.MessageDto;
import com.ditu.agent.chat.ChatDtos.SendMessageResponse;
import com.ditu.agent.common.ApiResponse;
import com.ditu.agent.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 用户端会话接口。
 *
 * <p>所有会话和消息接口都从 Token 读取 userId，路径中的 conversationId 只作为资源定位，不能决定归属。</p>
 */
@RestController
@RequestMapping("/api/v1/conversations")
public class ChatController {
  private final ChatService chatService;

  public ChatController(ChatService chatService) {
    this.chatService = chatService;
  }

  @GetMapping
  public ApiResponse<PageResponse<ConversationDto>> conversations(@RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.ok(chatService.pageConversations(SecurityUtils.currentUser().id(), page, pageSize));
  }

  @PostMapping
  public ApiResponse<ConversationDto> create(@RequestBody(required = false) CreateConversationRequest request) {
    return ApiResponse.ok(chatService.createConversation(SecurityUtils.currentUser().id(),
        request == null ? "新会话" : request.title()));
  }

  @GetMapping("/{conversationId}/messages")
  public ApiResponse<PageResponse<MessageDto>> messages(@PathVariable Long conversationId,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "50") int pageSize) {
    return ApiResponse.ok(chatService.pageMessages(SecurityUtils.currentUser().id(), conversationId, page, pageSize));
  }

  @PostMapping("/{conversationId}/messages")
  public ApiResponse<SendMessageResponse> send(@PathVariable Long conversationId,
                                               @Valid @RequestBody SendMessageRequest request) {
    return ApiResponse.ok(chatService.sendMessage(SecurityUtils.currentUser().id(), conversationId, request.content()));
  }

  @GetMapping(value = "/{conversationId}/runs/{runId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<Map<String, Object>>> events(@PathVariable Long conversationId, @PathVariable Long runId,
      @RequestHeader(value = "Last-Event-ID", required = false) Integer lastEventId) {
    return chatService.streamEvents(SecurityUtils.currentUser().id(), conversationId, runId, lastEventId);
  }

  public record CreateConversationRequest(String title) {
  }

  public record SendMessageRequest(@NotBlank String content, String clientMessageId) {
  }
}
