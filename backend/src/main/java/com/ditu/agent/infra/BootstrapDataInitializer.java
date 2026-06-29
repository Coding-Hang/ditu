package com.ditu.agent.infra;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 本地演示数据初始化。
 *
 * <p>首次启动时创建管理员和演示用户，便于按验收清单执行登录、会话、工单和管理端操作；生产可自行修改密码。</p>
 */
@Component
public class BootstrapDataInitializer implements ApplicationRunner {
  private final JdbcTemplate jdbcTemplate;
  private final PasswordEncoder passwordEncoder;

  public BootstrapDataInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
    this.jdbcTemplate = jdbcTemplate;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(ApplicationArguments args) {
    Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM app_user", Integer.class);
    if (count != null && count > 0) {
      return;
    }
    jdbcTemplate.update("""
        INSERT INTO app_user(username, password_hash, display_name, role_code, status, plan_code, quota_total)
        VALUES
          ('admin', ?, '滴兔管理员', 'ADMIN', 'ACTIVE', 'PLUS', 1000),
          ('demo', ?, '演示用户', 'USER', 'ACTIVE', 'PRO', 300)
        """, passwordEncoder.encode("Admin123!"), passwordEncoder.encode("Demo123!"));
    jdbcTemplate.update("""
        INSERT INTO quota_ledger(user_id, change_type, delta_count, before_total, before_used, after_total,
                                 after_used, reason, ref_type, ref_id, operator_user_id)
        SELECT id, 'ALLOCATE', quota_total, 0, 0, quota_total, 0, '系统初始化演示账号次数', 'APP_USER', id, NULL
        FROM app_user
        """);
  }
}
