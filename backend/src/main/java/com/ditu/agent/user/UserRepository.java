package com.ditu.agent.user;

import com.ditu.agent.common.PageResponse;
import com.ditu.agent.user.UserDtos.UserSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 用户账号仓储。
 *
 * <p>SQL 中显式保留状态和归属字段，服务层据此完成停用用户拦截、角色授权和管理端筛选。</p>
 */
@Repository
public class UserRepository {
  private final JdbcTemplate jdbcTemplate;
  private final RowMapper<UserAccount> accountMapper = this::mapAccount;

  public UserRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<UserAccount> findByUsername(String username) {
    List<UserAccount> users = jdbcTemplate.query("""
        /* AUTH-001: 登录只按唯一用户名读取账号，后续服务层校验密码和账号状态。 */
        SELECT id, username, password_hash, display_name, role_code, status, plan_code,
               quota_total, quota_used, last_login_at
        FROM app_user
        WHERE username = ?
        """, accountMapper, username);
    return users.stream().findFirst();
  }

  public Optional<UserAccount> findById(Long id) {
    List<UserAccount> users = jdbcTemplate.query("""
        /* AUTH-002/SEC-003: 当前用户与资源归属校验使用主键读取最新状态和次数。 */
        SELECT id, username, password_hash, display_name, role_code, status, plan_code,
               quota_total, quota_used, last_login_at
        FROM app_user
        WHERE id = ?
        """, accountMapper, id);
    return users.stream().findFirst();
  }

  public long createUser(String username, String passwordHash, String displayName, String phone, String email,
                         String roleCode, String planCode, int quotaTotal) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      var ps = connection.prepareStatement("""
          /* ADMIN-USER-002: 管理端创建用户时同时写入套餐和初始总次数，已用次数始终从 0 开始。 */
          INSERT INTO app_user(username, password_hash, display_name, phone, email, role_code, plan_code, quota_total)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?)
          """, new String[] {"id"});
      ps.setString(1, username);
      ps.setString(2, passwordHash);
      ps.setString(3, displayName);
      ps.setString(4, phone);
      ps.setString(5, email);
      ps.setString(6, roleCode);
      ps.setString(7, planCode);
      ps.setInt(8, quotaTotal);
      return ps;
    }, keyHolder);
    return keyHolder.getKey().longValue();
  }

  public void updateStatus(Long userId, String status) {
    jdbcTemplate.update("""
        /* ADMIN-USER-003/004: 状态变更立即影响登录和会话发起，不删除历史会话与工单。 */
        UPDATE app_user SET status = ?, updated_at = now() WHERE id = ?
        """, status, userId);
  }

  public void updatePlan(Long userId, String planCode) {
    jdbcTemplate.update("""
        /* ADMIN-PLAN-002: 套餐调整只改变 plan_code，不自动改次数，次数必须走 quota_ledger。 */
        UPDATE app_user SET plan_code = ?, updated_at = now() WHERE id = ?
        """, planCode, userId);
  }

  public void markLogin(Long userId) {
    jdbcTemplate.update("UPDATE app_user SET last_login_at = now(), updated_at = now() WHERE id = ?", userId);
  }

  public void updateQuota(Long userId, int quotaTotal, int quotaUsed) {
    jdbcTemplate.update("""
        /* ADMIN-QUOTA-003: app_user 保存当前可用额度快照，完整变化过程由 quota_ledger 追踪。 */
        UPDATE app_user SET quota_total = ?, quota_used = ?, updated_at = now() WHERE id = ?
        """, quotaTotal, quotaUsed, userId);
  }

  public PageResponse<UserSummary> pageUsers(String keyword, String status, String planCode, int page, int pageSize) {
    List<Object> params = new ArrayList<>();
    StringBuilder where = new StringBuilder(" WHERE 1=1 ");
    if (keyword != null && !keyword.isBlank()) {
      where.append(" AND (username ILIKE ? OR display_name ILIKE ? OR phone ILIKE ?) ");
      String like = "%" + keyword + "%";
      params.add(like);
      params.add(like);
      params.add(like);
    }
    if (status != null && !status.isBlank()) {
      where.append(" AND status = ? ");
      params.add(status);
    }
    if (planCode != null && !planCode.isBlank()) {
      where.append(" AND plan_code = ? ");
      params.add(planCode);
    }
    Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM app_user" + where, Long.class, params.toArray());
    params.add(pageSize);
    params.add((page - 1) * pageSize);
    List<UserSummary> records = jdbcTemplate.query("""
        /* ADMIN-USER-001: 管理端用户列表支持关键词、状态、套餐过滤，返回次数快照便于运营处理。 */
        SELECT id, username, password_hash, display_name, role_code, status, plan_code,
               quota_total, quota_used, last_login_at
        FROM app_user
        """ + where + " ORDER BY id DESC LIMIT ? OFFSET ?", accountMapper, params.toArray())
        .stream().map(UserSummary::from).toList();
    return new PageResponse<>(records, page, pageSize, total == null ? 0 : total);
  }

  private UserAccount mapAccount(ResultSet rs, int rowNum) throws SQLException {
    OffsetDateTime lastLoginAt = rs.getObject("last_login_at", OffsetDateTime.class);
    return new UserAccount(
        rs.getLong("id"),
        rs.getString("username"),
        rs.getString("password_hash"),
        rs.getString("display_name"),
        rs.getString("role_code"),
        rs.getString("status"),
        rs.getString("plan_code"),
        rs.getInt("quota_total"),
        rs.getInt("quota_used"),
        lastLoginAt);
  }
}
