package com.ditu.agent.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 管理端敏感操作审计服务。
 *
 * <p>用户状态、次数、模型链接、工单和 RAG 操作都通过此服务写入 audit_log，满足 SEC-004 的追踪要求。</p>
 */
@Service
public class AuditService {
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public AuditService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  public void record(Long actorUserId, String action, String targetType, Long targetId, Map<String, ?> detail) {
    jdbcTemplate.update("""
        INSERT INTO audit_log(actor_user_id, action, target_type, target_id, detail)
        VALUES (?, ?, ?, ?, ?::jsonb)
        """, actorUserId, action, targetType, targetId, toJson(detail));
  }

  private String toJson(Map<String, ?> detail) {
    try {
      return objectMapper.writeValueAsString(detail == null ? Map.of() : detail);
    } catch (JsonProcessingException ex) {
      return "{}";
    }
  }
}
