package com.ditu.agent.user;

import com.ditu.agent.user.UserDtos.PlanDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 套餐配置仓储。
 *
 * <p>BASIC、PRO、PLUS 是文档规定的固定套餐，用户端和管理端都从数据库读取，避免页面写死权益。</p>
 */
@Repository
public class PlanRepository {
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public PlanRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  public List<PlanDto> listEnabled() {
    return jdbcTemplate.query("""
        /* USER-001/ADMIN-PLAN-001: 仅展示启用套餐，按 level_order 保持基础、Pro、Plus 顺序。 */
        SELECT code, name, level_order, monthly_quota, rag_enabled, priority_support, description, benefits::text
        FROM subscription_plan
        WHERE enabled = true
        ORDER BY level_order ASC
        """, this::mapPlan);
  }

  public Optional<PlanDto> findByCode(String code) {
    return jdbcTemplate.query("""
        /* ADMIN-PLAN-002: 套餐调整前确认目标套餐存在，避免用户进入无效权益状态。 */
        SELECT code, name, level_order, monthly_quota, rag_enabled, priority_support, description, benefits::text
        FROM subscription_plan
        WHERE code = ?
        """, this::mapPlan, code).stream().findFirst();
  }

  private PlanDto mapPlan(ResultSet rs, int rowNum) throws SQLException {
    try {
      List<String> benefits = objectMapper.readValue(rs.getString("benefits"), new TypeReference<>() {
      });
      return new PlanDto(rs.getString("code"), rs.getString("name"), rs.getInt("level_order"),
          rs.getInt("monthly_quota"), rs.getBoolean("rag_enabled"), rs.getBoolean("priority_support"),
          rs.getString("description"), benefits);
    } catch (Exception ex) {
      throw new SQLException("套餐权益 JSON 解析失败", ex);
    }
  }
}
