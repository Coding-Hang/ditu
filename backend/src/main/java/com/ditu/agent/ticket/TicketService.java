package com.ditu.agent.ticket;

import com.ditu.agent.common.BusinessException;
import com.ditu.agent.common.ErrorCode;
import com.ditu.agent.common.PageResponse;
import com.ditu.agent.infra.AuditService;
import com.ditu.agent.ticket.TicketDtos.CustomerServiceProfileDto;
import com.ditu.agent.ticket.TicketDtos.TicketDetailDto;
import com.ditu.agent.ticket.TicketDtos.TicketDto;
import com.ditu.agent.ticket.TicketDtos.TicketMessageDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 专属客服与工单服务。
 *
 * <p>工单状态流转、关闭后禁止追加、用户归属校验和管理端审计都集中在此服务。</p>
 */
@Service
public class TicketService {
  private final JdbcTemplate jdbcTemplate;
  private final AuditService auditService;

  public TicketService(JdbcTemplate jdbcTemplate, AuditService auditService) {
    this.jdbcTemplate = jdbcTemplate;
    this.auditService = auditService;
  }

  public List<CustomerServiceProfileDto> profiles() {
    return jdbcTemplate.query("""
        /* CS-001: 只展示启用客服，并按 sort_order 保持商标、专利、版权、综合咨询的稳定顺序。 */
        SELECT id, service_type, name, role_name, positioning, intro, avatar_url
        FROM customer_service_profile
        WHERE enabled = true
        ORDER BY sort_order ASC, id ASC
        """, (rs, rowNum) -> new CustomerServiceProfileDto(rs.getLong("id"), rs.getString("service_type"),
        rs.getString("name"), rs.getString("role_name"), rs.getString("positioning"), rs.getString("intro"),
        rs.getString("avatar_url")));
  }

  @Transactional
  public TicketDetailDto create(Long userId, Long serviceProfileId, Long conversationId, String title, String content) {
    Long ticketId = jdbcTemplate.queryForObject("""
        INSERT INTO support_ticket(user_id, service_profile_id, conversation_id, title, content, status, last_message_at)
        VALUES (?, ?, ?, ?, ?, 'OPEN', now())
        RETURNING id
        """, Long.class, userId, serviceProfileId, conversationId, title, content);
    addMessageInternal(ticketId, userId, "USER", content);
    return detailForUser(userId, ticketId);
  }

  public PageResponse<TicketDto> pageUserTickets(Long userId, int page, int pageSize) {
    Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM support_ticket WHERE user_id = ?", Long.class,
        userId);
    var records = jdbcTemplate.query("""
        /* TICKET-002/SEC-003: 用户端工单列表只按当前 user_id 查询，不能通过参数查看他人工单。 */
        SELECT id, user_id, service_profile_id, conversation_id, title, content, status, priority, assigned_to,
               last_message_at, closed_at, created_at, updated_at
        FROM support_ticket
        WHERE user_id = ?
        ORDER BY updated_at DESC
        LIMIT ? OFFSET ?
        """, this::mapTicket, userId, pageSize, (page - 1) * pageSize);
    return new PageResponse<>(records, page, pageSize, total == null ? 0 : total);
  }

  public TicketDetailDto detailForUser(Long userId, Long ticketId) {
    TicketDto ticket = findTicketForUser(userId, ticketId);
    return new TicketDetailDto(ticket, messages(ticketId));
  }

  @Transactional
  public TicketDetailDto userReply(Long userId, Long ticketId, String content) {
    TicketDto ticket = findTicketForUser(userId, ticketId);
    if ("CLOSED".equals(ticket.status())) {
      throw new BusinessException(ErrorCode.TICKET_NOT_FOUND, "工单已关闭，不能追加消息");
    }
    addMessageInternal(ticketId, userId, "USER", content);
    if ("RESOLVED".equals(ticket.status())) {
      jdbcTemplate.update("UPDATE support_ticket SET status = 'PROCESSING', updated_at = now() WHERE id = ?", ticketId);
    }
    return detailForUser(userId, ticketId);
  }

  public PageResponse<TicketDto> pageAdminTickets(String status, String serviceType, String keyword, int page,
                                                  int pageSize) {
    String serviceJoin = " LEFT JOIN customer_service_profile p ON p.id = t.service_profile_id ";
    StringBuilder where = new StringBuilder(" WHERE 1=1 ");
    List<Object> params = new ArrayList<>();
    if (status != null && !status.isBlank()) {
      where.append(" AND t.status = ? ");
      params.add(status);
    }
    if (serviceType != null && !serviceType.isBlank()) {
      where.append(" AND p.service_type = ? ");
      params.add(serviceType);
    }
    if (keyword != null && !keyword.isBlank()) {
      where.append(" AND (t.title ILIKE ? OR t.content ILIKE ?) ");
      params.add("%" + keyword + "%");
      params.add("%" + keyword + "%");
    }
    Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM support_ticket t" + serviceJoin + where,
        Long.class, params.toArray());
    params.add(pageSize);
    params.add((page - 1) * pageSize);
    var records = jdbcTemplate.query("""
        /* ADMIN-TICKET-001: 管理端按状态、客服类型和关键词筛选工单，客服处理不受用户归属限制。 */
        SELECT t.id, t.user_id, t.service_profile_id, t.conversation_id, t.title, t.content, t.status, t.priority,
               t.assigned_to, t.last_message_at, t.closed_at, t.created_at, t.updated_at
        FROM support_ticket t
        """ + serviceJoin + where + " ORDER BY t.updated_at DESC LIMIT ? OFFSET ?", this::mapTicket,
        params.toArray());
    return new PageResponse<>(records, page, pageSize, total == null ? 0 : total);
  }

  public TicketDetailDto detailForAdmin(Long ticketId) {
    return new TicketDetailDto(findTicket(ticketId), messages(ticketId));
  }

  @Transactional
  public TicketDetailDto assign(Long ticketId, Long assignedTo, Long actorUserId) {
    jdbcTemplate.update("""
        /* ADMIN-TICKET-003: 分配处理人后进入 PROCESSING，表示客服已接入处理。 */
        UPDATE support_ticket SET assigned_to = ?, status = 'PROCESSING', updated_at = now() WHERE id = ?
        """, assignedTo, ticketId);
    auditService.record(actorUserId, "TICKET_ASSIGN", "SUPPORT_TICKET", ticketId, Map.of("assignedTo", assignedTo));
    return detailForAdmin(ticketId);
  }

  @Transactional
  public TicketDetailDto adminReply(Long ticketId, Long actorUserId, String roleCode, String content) {
    TicketDto ticket = findTicket(ticketId);
    if ("CLOSED".equals(ticket.status())) {
      throw new BusinessException(ErrorCode.TICKET_NOT_FOUND, "工单已关闭，不能追加消息");
    }
    addMessageInternal(ticketId, actorUserId, roleCode, content);
    jdbcTemplate.update("UPDATE support_ticket SET status = 'PROCESSING', updated_at = now() WHERE id = ?", ticketId);
    auditService.record(actorUserId, "TICKET_REPLY", "SUPPORT_TICKET", ticketId, Map.of("role", roleCode));
    return detailForAdmin(ticketId);
  }

  @Transactional
  public TicketDetailDto changeStatus(Long ticketId, String status, String reason, Long actorUserId) {
    TicketDto ticket = findTicket(ticketId);
    if (!allowed(ticket.status(), status)) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "非法工单状态流转");
    }
    jdbcTemplate.update("""
        UPDATE support_ticket
        SET status = ?, closed_at = CASE WHEN ? = 'CLOSED' THEN now() ELSE closed_at END, updated_at = now()
        WHERE id = ?
        """, status, status, ticketId);
    auditService.record(actorUserId, "TICKET_STATUS_CHANGE", "SUPPORT_TICKET", ticketId,
        Map.of("from", ticket.status(), "to", status, "reason", reason == null ? "" : reason));
    return detailForAdmin(ticketId);
  }

  private boolean allowed(String from, String to) {
    if (from.equals(to)) {
      return true;
    }
    return Set.of("OPEN:PENDING", "PENDING:PROCESSING", "OPEN:PROCESSING", "PROCESSING:RESOLVED",
        "RESOLVED:CLOSED", "RESOLVED:PROCESSING", "PROCESSING:CLOSED", "OPEN:CLOSED").contains(from + ":" + to);
  }

  private TicketDto findTicketForUser(Long userId, Long ticketId) {
    return jdbcTemplate.query("""
        /* TICKET-003/SEC-003: 工单详情必须限定 user_id，用户不能访问其他用户工单。 */
        SELECT id, user_id, service_profile_id, conversation_id, title, content, status, priority, assigned_to,
               last_message_at, closed_at, created_at, updated_at
        FROM support_ticket
        WHERE id = ? AND user_id = ?
        """, this::mapTicket, ticketId, userId).stream().findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND, "工单不存在或无权访问"));
  }

  private TicketDto findTicket(Long ticketId) {
    return jdbcTemplate.query("""
        SELECT id, user_id, service_profile_id, conversation_id, title, content, status, priority, assigned_to,
               last_message_at, closed_at, created_at, updated_at
        FROM support_ticket
        WHERE id = ?
        """, this::mapTicket, ticketId).stream().findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND, "工单不存在"));
  }

  private void addMessageInternal(Long ticketId, Long senderUserId, String senderRole, String content) {
    jdbcTemplate.update("""
        INSERT INTO support_ticket_message(ticket_id, sender_user_id, sender_role, content)
        VALUES (?, ?, ?, ?)
        """, ticketId, senderUserId, senderRole, content);
    jdbcTemplate.update("UPDATE support_ticket SET last_message_at = now(), updated_at = now() WHERE id = ?", ticketId);
  }

  private List<TicketMessageDto> messages(Long ticketId) {
    return jdbcTemplate.query("""
        SELECT id, ticket_id, sender_user_id, sender_role, content, created_at
        FROM support_ticket_message
        WHERE ticket_id = ?
        ORDER BY created_at ASC
        """, (rs, rowNum) -> new TicketMessageDto(rs.getLong("id"), rs.getLong("ticket_id"),
        rs.getLong("sender_user_id"), rs.getString("sender_role"), rs.getString("content"),
        rs.getObject("created_at", OffsetDateTime.class)), ticketId);
  }

  private TicketDto mapTicket(ResultSet rs, int rowNum) throws SQLException {
    return new TicketDto(rs.getLong("id"), rs.getLong("user_id"),
        rs.getObject("service_profile_id") == null ? null : rs.getLong("service_profile_id"),
        rs.getObject("conversation_id") == null ? null : rs.getLong("conversation_id"), rs.getString("title"),
        rs.getString("content"), rs.getString("status"), rs.getString("priority"),
        rs.getObject("assigned_to") == null ? null : rs.getLong("assigned_to"),
        rs.getObject("last_message_at", OffsetDateTime.class), rs.getObject("closed_at", OffsetDateTime.class),
        rs.getObject("created_at", OffsetDateTime.class), rs.getObject("updated_at", OffsetDateTime.class));
  }
}
