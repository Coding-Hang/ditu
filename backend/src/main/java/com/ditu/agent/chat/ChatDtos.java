package com.ditu.agent.chat;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 聊天域 DTO 集合。
 *
 * <p>用户端历史、消息和 SSE 入口共用这些模型，所有查询都由服务层限定为当前用户数据。</p>
 */
public final class ChatDtos {
  private ChatDtos() {
  }

  public record ConversationDto(Long id, Long userId, String title, String status, OffsetDateTime lastMessageAt,
                                OffsetDateTime createdAt, OffsetDateTime updatedAt) {
  }

  public record MessageDto(Long id, Long conversationId, Long userId, String role, int sequenceNo, String content,
                           Long agentRunId, int quotaCost, OffsetDateTime createdAt) {
  }

  public record SendMessageResponse(Long messageId, Long runId, Long conversationId, int quotaReserved) {
  }

  public record RunInfo(Long id, Long conversationId, Long userId, Long userMessageId, Long modelConfigId,
                        String modelName, String status) {
  }

  public record StoredEvent(Long id, Long runId, Long conversationId, String eventType, int sequenceNo,
                            boolean visibleToUser, Map<String, Object> payload, OffsetDateTime createdAt) {
  }
}
