package com.ditu.agent.chat;

import com.ditu.agent.chat.ChatDtos.ConversationDto;
import com.ditu.agent.chat.ChatDtos.MessageDto;
import com.ditu.agent.chat.ChatDtos.RunInfo;
import com.ditu.agent.chat.ChatDtos.StoredEvent;
import com.ditu.agent.common.PageResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 会话、消息、Agent run 与事件仓储。
 *
 * <p>所有 SQL 都带 user_id 或 conversation_id 约束，服务层据此保证用户只能访问自己的聊天数据。</p>
 */
@Repository
public class ChatRepository {
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public ChatRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  public long createConversation(Long userId, String title) {
    return jdbcTemplate.queryForObject("""
        INSERT INTO conversation(user_id, title, last_message_at)
        VALUES (?, ?, now())
        RETURNING id
        """, Long.class, userId, title == null || title.isBlank() ? "新会话" : title);
  }

  public Optional<ConversationDto> findConversationForUser(Long conversationId, Long userId) {
    return jdbcTemplate.query("""
        /* CHAT-005/SEC-003: 查询会话时同时限定 conversation.id 和当前 user_id，阻断跨用户读取。 */
        SELECT id, user_id, title, status, last_message_at, created_at, updated_at
        FROM conversation
        WHERE id = ? AND user_id = ? AND status <> 'DELETED'
        """, this::mapConversation, conversationId, userId).stream().findFirst();
  }

  public PageResponse<ConversationDto> pageConversations(Long userId, int page, int pageSize) {
    Long total = jdbcTemplate.queryForObject("""
        SELECT count(*) FROM conversation WHERE user_id = ? AND status <> 'DELETED'
        """, Long.class, userId);
    List<ConversationDto> records = jdbcTemplate.query("""
        /* CHAT-005: 会话列表按更新时间倒序，仅返回当前用户自己的会话。 */
        SELECT id, user_id, title, status, last_message_at, created_at, updated_at
        FROM conversation
        WHERE user_id = ? AND status <> 'DELETED'
        ORDER BY updated_at DESC
        LIMIT ? OFFSET ?
        """, this::mapConversation, userId, pageSize, (page - 1) * pageSize);
    return new PageResponse<>(records, page, pageSize, total == null ? 0 : total);
  }

  public PageResponse<MessageDto> pageMessages(Long conversationId, Long userId, int page, int pageSize) {
    Long total = jdbcTemplate.queryForObject("""
        SELECT count(*)
        FROM chat_message m JOIN conversation c ON c.id = m.conversation_id
        WHERE m.conversation_id = ? AND c.user_id = ?
        """, Long.class, conversationId, userId);
    List<MessageDto> records = jdbcTemplate.query("""
        /* CHAT-006/SEC-003: 消息历史必须经 conversation.user_id 过滤，按 sequence_no 升序恢复完整上下文。 */
        SELECT m.id, m.conversation_id, m.user_id, m.role, m.sequence_no, m.content, m.agent_run_id,
               m.quota_cost, m.created_at
        FROM chat_message m
        JOIN conversation c ON c.id = m.conversation_id
        WHERE m.conversation_id = ? AND c.user_id = ?
        ORDER BY m.sequence_no ASC
        LIMIT ? OFFSET ?
        """, this::mapMessage, conversationId, userId, pageSize, (page - 1) * pageSize);
    return new PageResponse<>(records, page, pageSize, total == null ? 0 : total);
  }

  public int nextSequence(Long conversationId) {
    Integer current = jdbcTemplate.queryForObject("""
        SELECT COALESCE(max(sequence_no), 0) FROM chat_message WHERE conversation_id = ?
        """, Integer.class, conversationId);
    return (current == null ? 0 : current) + 1;
  }

  public long insertMessage(Long conversationId, Long userId, String role, int sequenceNo, String content,
                            Long agentRunId, int quotaCost) {
    Long id = jdbcTemplate.queryForObject("""
        INSERT INTO chat_message(conversation_id, user_id, role, sequence_no, content, agent_run_id, quota_cost)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """, Long.class, conversationId, userId, role, sequenceNo, content, agentRunId, quotaCost);
    jdbcTemplate.update("""
        UPDATE conversation SET last_message_at = now(), updated_at = now() WHERE id = ?
        """, conversationId);
    return id == null ? 0 : id;
  }

  public long createRun(Long conversationId, Long userId, Long userMessageId, Long modelConfigId, String modelName) {
    Long id = jdbcTemplate.queryForObject("""
        /* AGENT-002/AGENT-007: run 记录实际使用的模型配置和模型名，便于追踪用户级模型链接生效情况。 */
        INSERT INTO agent_run(conversation_id, user_id, user_message_id, model_config_id, model_name, status)
        VALUES (?, ?, ?, ?, ?, 'RUNNING')
        RETURNING id
        """, Long.class, conversationId, userId, userMessageId, modelConfigId, modelName);
    return id == null ? 0 : id;
  }

  public Optional<RunInfo> findRunForUser(Long conversationId, Long runId, Long userId) {
    return jdbcTemplate.query("""
        /* CHAT-008/SEC-003: SSE 订阅也必须校验 run、conversation 和当前用户三者一致。 */
        SELECT id, conversation_id, user_id, user_message_id, model_config_id, model_name, status
        FROM agent_run
        WHERE id = ? AND conversation_id = ? AND user_id = ?
        """, this::mapRun, runId, conversationId, userId).stream().findFirst();
  }

  public String findUserMessage(Long messageId) {
    return jdbcTemplate.queryForObject("SELECT content FROM chat_message WHERE id = ?", String.class, messageId);
  }

  public void updateRunStatus(Long runId, String status, String errorCode, String errorMessage) {
    jdbcTemplate.update("""
        UPDATE agent_run
        SET status = ?, error_code = ?, error_message = ?, completed_at = CASE WHEN ? <> 'RUNNING' THEN now() ELSE NULL END
        WHERE id = ?
        """, status, errorCode, errorMessage, status, runId);
  }

  public long insertEvent(Long runId, Long conversationId, String eventType, boolean visibleToUser,
                          Map<String, Object> payload) {
    Integer next = jdbcTemplate.queryForObject("""
        SELECT COALESCE(max(sequence_no), 0) + 1 FROM agent_run_event WHERE run_id = ?
        """, Integer.class, runId);
    Long id = jdbcTemplate.queryForObject("""
        INSERT INTO agent_run_event(run_id, conversation_id, event_type, sequence_no, visible_to_user, payload)
        VALUES (?, ?, ?, ?, ?, ?::jsonb)
        RETURNING id
        """, Long.class, runId, conversationId, eventType, next, visibleToUser, toJson(payload));
    return id == null ? 0 : id;
  }

  public List<StoredEvent> listEventsAfter(Long runId, int lastEventId) {
    return jdbcTemplate.query("""
        /* CHAT-008: Last-Event-ID 补发只返回未消费事件，顺序与首次 SSE 完全一致。 */
        SELECT id, run_id, conversation_id, event_type, sequence_no, visible_to_user, payload::text, created_at
        FROM agent_run_event
        WHERE run_id = ? AND sequence_no > ?
        ORDER BY sequence_no ASC
        """, this::mapEvent, runId, lastEventId);
  }

  public boolean hasEvents(Long runId) {
    Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM agent_run_event WHERE run_id = ?", Integer.class,
        runId);
    return count != null && count > 0;
  }

  private String toJson(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (Exception ex) {
      return "{}";
    }
  }

  private Map<String, Object> toMap(String json) throws SQLException {
    try {
      return objectMapper.readValue(json, new TypeReference<>() {
      });
    } catch (Exception ex) {
      throw new SQLException("Agent 事件 JSON 解析失败", ex);
    }
  }

  private ConversationDto mapConversation(ResultSet rs, int rowNum) throws SQLException {
    return new ConversationDto(rs.getLong("id"), rs.getLong("user_id"), rs.getString("title"),
        rs.getString("status"), rs.getObject("last_message_at", OffsetDateTime.class),
        rs.getObject("created_at", OffsetDateTime.class), rs.getObject("updated_at", OffsetDateTime.class));
  }

  private MessageDto mapMessage(ResultSet rs, int rowNum) throws SQLException {
    return new MessageDto(rs.getLong("id"), rs.getLong("conversation_id"), rs.getLong("user_id"),
        rs.getString("role"), rs.getInt("sequence_no"), rs.getString("content"),
        rs.getObject("agent_run_id") == null ? null : rs.getLong("agent_run_id"), rs.getInt("quota_cost"),
        rs.getObject("created_at", OffsetDateTime.class));
  }

  private RunInfo mapRun(ResultSet rs, int rowNum) throws SQLException {
    return new RunInfo(rs.getLong("id"), rs.getLong("conversation_id"), rs.getLong("user_id"),
        rs.getLong("user_message_id"), rs.getObject("model_config_id") == null ? null : rs.getLong("model_config_id"),
        rs.getString("model_name"), rs.getString("status"));
  }

  private StoredEvent mapEvent(ResultSet rs, int rowNum) throws SQLException {
    return new StoredEvent(rs.getLong("id"), rs.getLong("run_id"), rs.getLong("conversation_id"),
        rs.getString("event_type"), rs.getInt("sequence_no"), rs.getBoolean("visible_to_user"),
        toMap(rs.getString("payload")), rs.getObject("created_at", OffsetDateTime.class));
  }
}
