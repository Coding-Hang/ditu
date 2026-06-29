package com.ditu.agent.user;

import com.ditu.agent.common.BusinessException;
import com.ditu.agent.common.ErrorCode;
import com.ditu.agent.common.PageResponse;
import com.ditu.agent.infra.AuditService;
import com.ditu.agent.user.UserDtos.UserSummary;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端用户应用服务。
 *
 * <p>用户创建、状态、套餐和次数调整都在这里编排，确保业务变更同时产生 quota_ledger 和 audit_log。</p>
 */
@Service
public class UserService {
  private final UserRepository userRepository;
  private final PlanRepository planRepository;
  private final QuotaService quotaService;
  private final AuditService auditService;
  private final PasswordEncoder passwordEncoder;
  private final JdbcTemplate jdbcTemplate;

  public UserService(UserRepository userRepository, PlanRepository planRepository, QuotaService quotaService,
                     AuditService auditService, PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate) {
    this.userRepository = userRepository;
    this.planRepository = planRepository;
    this.quotaService = quotaService;
    this.auditService = auditService;
    this.passwordEncoder = passwordEncoder;
    this.jdbcTemplate = jdbcTemplate;
  }

  public PageResponse<UserSummary> pageUsers(String keyword, String status, String planCode, int page, int pageSize) {
    return userRepository.pageUsers(keyword, status, planCode, page, pageSize);
  }

  public UserSummary detail(Long userId) {
    return userRepository.findById(userId).map(UserSummary::from)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
  }

  @Transactional
  public UserSummary createUser(CreateUserCommand command, Long actorUserId) {
    planRepository.findByCode(command.planCode())
        .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "套餐不存在"));
    long userId = userRepository.createUser(command.username(), passwordEncoder.encode(command.password()),
        command.displayName(), command.phone(), command.email(), "USER", command.planCode(), command.quotaTotal());
    insertInitialQuotaLedger(userId, command.quotaTotal(), actorUserId);
    auditService.record(actorUserId, "USER_CREATE", "APP_USER", userId,
        Map.of("username", command.username(), "planCode", command.planCode(), "quotaTotal", command.quotaTotal()));
    return detail(userId);
  }

  @Transactional
  public UserSummary disable(Long userId, Long actorUserId) {
    userRepository.updateStatus(userId, "DISABLED");
    auditService.record(actorUserId, "USER_DISABLE", "APP_USER", userId, Map.of("status", "DISABLED"));
    return detail(userId);
  }

  @Transactional
  public UserSummary enable(Long userId, Long actorUserId) {
    userRepository.updateStatus(userId, "ACTIVE");
    auditService.record(actorUserId, "USER_ENABLE", "APP_USER", userId, Map.of("status", "ACTIVE"));
    return detail(userId);
  }

  @Transactional
  public UserSummary changePlan(Long userId, String planCode, String reason, Long actorUserId) {
    planRepository.findByCode(planCode)
        .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "套餐不存在"));
    userRepository.updatePlan(userId, planCode);
    auditService.record(actorUserId, "USER_PLAN_CHANGE", "APP_USER", userId,
        Map.of("planCode", planCode, "reason", reason == null ? "" : reason));
    return detail(userId);
  }

  @Transactional
  public QuotaAdjustmentResult adjustQuota(Long userId, int deltaCount, String reason, Long actorUserId) {
    long ledgerId = quotaService.adjust(userId, deltaCount, reason, actorUserId);
    auditService.record(actorUserId, "USER_QUOTA_ADJUST", "APP_USER", userId,
        Map.of("deltaCount", deltaCount, "reason", reason, "ledgerId", ledgerId));
    UserSummary user = detail(userId);
    return new QuotaAdjustmentResult(ledgerId, user.quotaTotal(), user.quotaUsed(), user.remainingQuota());
  }

  private void insertInitialQuotaLedger(long userId, int quotaTotal, Long actorUserId) {
    jdbcTemplate.update("""
        /* ADMIN-USER-002/ADMIN-QUOTA-002: 初始次数也写入流水，保证用户总次数来源可追溯。 */
        INSERT INTO quota_ledger(user_id, change_type, delta_count, before_total, before_used, after_total,
                                 after_used, reason, ref_type, ref_id, operator_user_id)
        VALUES (?, 'ALLOCATE', ?, 0, 0, ?, 0, '管理员创建用户初始次数', 'APP_USER', ?, ?)
        """, userId, quotaTotal, quotaTotal, userId, actorUserId);
  }

  public record CreateUserCommand(String username, String password, String displayName, String phone, String email,
                                  String planCode, int quotaTotal) {
  }

  public record QuotaAdjustmentResult(Long ledgerId, int quotaTotal, int quotaUsed, int remainingQuota) {
  }
}
