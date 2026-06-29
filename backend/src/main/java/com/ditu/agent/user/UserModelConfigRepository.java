package com.ditu.agent.user;

import com.ditu.agent.user.UserDtos.ModelConfigDto;
import com.ditu.agent.user.UserDtos.ModelConfigInternal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 用户级大模型链接仓储。
 *
 * <p>一个用户最多一条配置；查询给管理端时只返回 hasApiKey，运行时内部查询才读取密文用于解密。</p>
 */
@Repository
public class UserModelConfigRepository {
  private final JdbcTemplate jdbcTemplate;

  public UserModelConfigRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<ModelConfigInternal> findInternalByUserId(Long userId) {
    return jdbcTemplate.query("""
        /* ADMIN-MODEL-001/AGENT-002: 内部读取密文只用于运行时解析模型链接，不进入普通接口响应。 */
        SELECT id, user_id, config_name, provider_code, base_url, model_name, auth_type, api_key_ciphertext,
               enabled, last_test_status, last_test_message, last_test_at
        FROM app_user_model_config
        WHERE user_id = ?
        """, this::mapInternal, userId).stream().findFirst();
  }

  public Optional<ModelConfigInternal> findEnabledInternalByUserId(Long userId) {
    return jdbcTemplate.query("""
        /* AGENT-002: Agent run 优先选择当前用户启用配置，未启用时由解析器回退平台默认模型。 */
        SELECT id, user_id, config_name, provider_code, base_url, model_name, auth_type, api_key_ciphertext,
               enabled, last_test_status, last_test_message, last_test_at
        FROM app_user_model_config
        WHERE user_id = ? AND enabled = true
        """, this::mapInternal, userId).stream().findFirst();
  }

  public Optional<ModelConfigDto> findPublicByUserId(Long userId) {
    return findInternalByUserId(userId).map(this::toPublicDto);
  }

  public long upsert(Long userId, String configName, String providerCode, String baseUrl, String modelName,
                     String authType, String apiKeyCiphertext, boolean enabled, Long actorUserId) {
    Long id = jdbcTemplate.queryForObject("""
        INSERT INTO app_user_model_config(user_id, config_name, provider_code, base_url, model_name, auth_type,
                                          api_key_ciphertext, enabled, created_by, updated_by)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (user_id) DO UPDATE SET
          config_name = EXCLUDED.config_name,
          provider_code = EXCLUDED.provider_code,
          base_url = EXCLUDED.base_url,
          model_name = EXCLUDED.model_name,
          auth_type = EXCLUDED.auth_type,
          api_key_ciphertext = COALESCE(EXCLUDED.api_key_ciphertext, app_user_model_config.api_key_ciphertext),
          enabled = EXCLUDED.enabled,
          updated_by = EXCLUDED.updated_by,
          updated_at = now()
        RETURNING id
        """, Long.class, userId, configName, providerCode, baseUrl, modelName, authType, apiKeyCiphertext, enabled,
        actorUserId, actorUserId);
    return id == null ? 0 : id;
  }

  public void updateTestResult(Long userId, String status, String message) {
    jdbcTemplate.update("""
        /* ADMIN-MODEL-003: 连通性测试结果留在配置上，便于管理员判断下次会话是否可用。 */
        UPDATE app_user_model_config
        SET last_test_status = ?, last_test_message = ?, last_test_at = now(), updated_at = now()
        WHERE user_id = ?
        """, status, message, userId);
  }

  public void disable(Long userId, Long actorUserId) {
    jdbcTemplate.update("""
        /* ADMIN-MODEL-004: 停用后不删除配置，下次 Agent run 自动回退平台默认链接。 */
        UPDATE app_user_model_config
        SET enabled = false, updated_by = ?, updated_at = now()
        WHERE user_id = ?
        """, actorUserId, userId);
  }

  public ModelConfigDto toPublicDto(ModelConfigInternal model) {
    return new ModelConfigDto(model.id(), model.userId(), model.configName(), model.providerCode(), model.baseUrl(),
        model.modelName(), model.authType(), model.apiKeyCiphertext() != null && !model.apiKeyCiphertext().isBlank(),
        model.enabled(), model.lastTestStatus(), model.lastTestMessage(), model.lastTestAt());
  }

  private ModelConfigInternal mapInternal(ResultSet rs, int rowNum) throws SQLException {
    return new ModelConfigInternal(rs.getLong("id"), rs.getLong("user_id"), rs.getString("config_name"),
        rs.getString("provider_code"), rs.getString("base_url"), rs.getString("model_name"),
        rs.getString("auth_type"), rs.getString("api_key_ciphertext"), rs.getBoolean("enabled"),
        rs.getString("last_test_status"), rs.getString("last_test_message"),
        rs.getObject("last_test_at", java.time.OffsetDateTime.class));
  }
}
