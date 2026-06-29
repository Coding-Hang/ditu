package com.ditu.agent.user;

import java.time.OffsetDateTime;

/**
 * app_user 的核心业务视图。
 *
 * <p>认证、次数控制和资源归属校验都会依赖该对象，字段只包含运行时需要的安全上下文和额度信息。</p>
 */
public record UserAccount(
    Long id,
    String username,
    String passwordHash,
    String displayName,
    String roleCode,
    String status,
    String planCode,
    int quotaTotal,
    int quotaUsed,
    OffsetDateTime lastLoginAt) {

  public int remainingQuota() {
    return quotaTotal - quotaUsed;
  }
}
