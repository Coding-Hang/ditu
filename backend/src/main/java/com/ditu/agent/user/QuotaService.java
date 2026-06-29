package com.ditu.agent.user;

import com.ditu.agent.common.BusinessException;
import com.ditu.agent.common.ErrorCode;
import com.ditu.agent.common.PageResponse;
import com.ditu.agent.user.UserDtos.QuotaLedgerDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 次数控制服务。
 *
 * <p>所有额度变化都先读取 app_user 快照，再同时更新用户次数并写 quota_ledger，确保 ADMIN-QUOTA-003 可审计。</p>
 */
@Service
public class QuotaService {
  private final UserRepository userRepository;
  private final JdbcTemplate jdbcTemplate;

  public QuotaService(UserRepository userRepository, JdbcTemplate jdbcTemplate) {
    this.userRepository = userRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional
  public long adjust(Long userId, int deltaCount, String reason, Long operatorUserId) {
    UserAccount user = lockUser(userId);
    int afterTotal = user.quotaTotal() + deltaCount;
    if (afterTotal < user.quotaUsed()) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "调整后总次数不能小于已用次数");
    }
    userRepository.updateQuota(userId, afterTotal, user.quotaUsed());
    return insertLedger(userId, "ADJUST", deltaCount, user.quotaTotal(), user.quotaUsed(), afterTotal,
        user.quotaUsed(), reason, "ADMIN_ADJUST", null, operatorUserId);
  }

  @Transactional
  public long reserveForRun(Long userId, Long runId) {
    UserAccount user = lockUser(userId);
    if (!"ACTIVE".equals(user.status())) {
      throw new BusinessException(ErrorCode.USER_DISABLED, "账号已停用，不能发起会话");
    }
    if (user.remainingQuota() <= 0) {
      throw new BusinessException(ErrorCode.QUOTA_NOT_ENOUGH, "可用次数不足");
    }
    return insertLedger(userId, "RESERVE", 0, user.quotaTotal(), user.quotaUsed(), user.quotaTotal(),
        user.quotaUsed(), "Agent run 预占 1 次", "AGENT_RUN", runId, userId);
  }

  @Transactional
  public long commitRun(Long userId, Long runId) {
    UserAccount user = lockUser(userId);
    if (user.remainingQuota() <= 0) {
      throw new BusinessException(ErrorCode.QUOTA_NOT_ENOUGH, "可用次数不足");
    }
    int afterUsed = user.quotaUsed() + 1;
    userRepository.updateQuota(userId, user.quotaTotal(), afterUsed);
    return insertLedger(userId, "COMMIT", -1, user.quotaTotal(), user.quotaUsed(), user.quotaTotal(), afterUsed,
        "Agent run 成功提交扣减", "AGENT_RUN", runId, userId);
  }

  @Transactional
  public long rollbackRun(Long userId, Long runId, String reason) {
    UserAccount user = lockUser(userId);
    return insertLedger(userId, "ROLLBACK", 0, user.quotaTotal(), user.quotaUsed(), user.quotaTotal(),
        user.quotaUsed(), reason, "AGENT_RUN", runId, userId);
  }

  public PageResponse<QuotaLedgerDto> pageLedger(Long userId, int page, int pageSize) {
    Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM quota_ledger WHERE user_id = ?", Long.class, userId);
    var records = jdbcTemplate.query("""
        /* ADMIN-QUOTA-002: 流水按时间倒序展示，运营可以还原每次预占、提交、回滚和手工调整。 */
        SELECT id, user_id, change_type, delta_count, before_total, before_used, after_total, after_used,
               reason, ref_type, ref_id, operator_user_id, created_at
        FROM quota_ledger
        WHERE user_id = ?
        ORDER BY created_at DESC
        LIMIT ? OFFSET ?
        """, this::mapLedger, userId, pageSize, (page - 1) * pageSize);
    return new PageResponse<>(records, page, pageSize, total == null ? 0 : total);
  }

  private UserAccount lockUser(Long userId) {
    return jdbcTemplate.query("""
        /* ADMIN-QUOTA-003: FOR UPDATE 防止并发扣次让 quota_used 超过 quota_total。 */
        SELECT id, username, password_hash, display_name, role_code, status, plan_code,
               quota_total, quota_used, last_login_at
        FROM app_user
        WHERE id = ?
        FOR UPDATE
        """, (rs, rowNum) -> new UserAccount(rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"),
        rs.getString("display_name"), rs.getString("role_code"), rs.getString("status"), rs.getString("plan_code"),
        rs.getInt("quota_total"), rs.getInt("quota_used"), rs.getObject("last_login_at", java.time.OffsetDateTime.class)),
        userId).stream().findFirst().orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
  }

  private long insertLedger(Long userId, String type, int delta, int beforeTotal, int beforeUsed, int afterTotal,
                            int afterUsed, String reason, String refType, Long refId, Long operatorUserId) {
    return jdbcTemplate.queryForObject("""
        INSERT INTO quota_ledger(user_id, change_type, delta_count, before_total, before_used, after_total,
                                 after_used, reason, ref_type, ref_id, operator_user_id)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """, Long.class, userId, type, delta, beforeTotal, beforeUsed, afterTotal, afterUsed, reason, refType, refId,
        operatorUserId);
  }

  private QuotaLedgerDto mapLedger(ResultSet rs, int rowNum) throws SQLException {
    return new QuotaLedgerDto(rs.getLong("id"), rs.getLong("user_id"), rs.getString("change_type"),
        rs.getInt("delta_count"), rs.getInt("before_total"), rs.getInt("before_used"), rs.getInt("after_total"),
        rs.getInt("after_used"), rs.getString("reason"), rs.getString("ref_type"),
        rs.getObject("ref_id") == null ? null : rs.getLong("ref_id"),
        rs.getObject("operator_user_id") == null ? null : rs.getLong("operator_user_id"),
        rs.getObject("created_at", java.time.OffsetDateTime.class));
  }
}
