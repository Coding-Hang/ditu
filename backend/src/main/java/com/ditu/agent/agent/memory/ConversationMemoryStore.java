package com.ditu.agent.agent.memory;

import com.ditu.agent.agent.core.AgentRunCommand.MemoryMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 会话 Memory 读取服务。
 *
 * <p>Agent run 只读取同一 conversation_id 下的历史消息，并由 chat 服务先做用户归属校验，满足多轮上下文和数据隔离。</p>
 */
@Service
public class ConversationMemoryStore {
  private final JdbcTemplate jdbcTemplate;

  public ConversationMemoryStore(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<MemoryMessage> loadMemory(Long conversationId, int limit) {
    List<MemoryMessage> newestFirst = jdbcTemplate.query("""
        /* AGENT-003: 按 sequence_no 读取最近历史消息，构造同一会话的多轮上下文 Memory。 */
        SELECT role, content
        FROM chat_message
        WHERE conversation_id = ?
        ORDER BY sequence_no DESC
        LIMIT ?
        """, (rs, rowNum) -> new MemoryMessage(rs.getString("role"), rs.getString("content")),
        conversationId, limit);
    List<MemoryMessage> chronological = new ArrayList<>(newestFirst);
    Collections.reverse(chronological);
    return chronological;
  }
}
