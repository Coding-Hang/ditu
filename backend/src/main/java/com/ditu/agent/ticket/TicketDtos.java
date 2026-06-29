package com.ditu.agent.ticket;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 工单域 DTO 集合。
 *
 * <p>用户端和管理端共用工单数据结构，但用户端列表只能由服务层限定为当前用户。</p>
 */
public final class TicketDtos {
  private TicketDtos() {
  }

  public record CustomerServiceProfileDto(Long id, String serviceType, String name, String roleName,
                                          String positioning, String intro, String avatarUrl) {
  }

  public record TicketDto(Long id, Long userId, Long serviceProfileId, Long conversationId, String title,
                          String content, String status, String priority, Long assignedTo,
                          OffsetDateTime lastMessageAt, OffsetDateTime closedAt, OffsetDateTime createdAt,
                          OffsetDateTime updatedAt) {
  }

  public record TicketMessageDto(Long id, Long ticketId, Long senderUserId, String senderRole, String content,
                                 OffsetDateTime createdAt) {
  }

  public record TicketDetailDto(TicketDto ticket, List<TicketMessageDto> messages) {
  }
}
