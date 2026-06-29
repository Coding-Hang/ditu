package com.ditu.agent.user;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 用户域 DTO 集合。
 *
 * <p>这些 DTO 在用户端和管理端之间复用，但永远不携带密码哈希或模型密钥明文。</p>
 */
public final class UserDtos {
  private UserDtos() {
  }

  public record UserSummary(Long id, String username, String displayName, String roleCode, String status,
                            String planCode, int quotaTotal, int quotaUsed, int remainingQuota,
                            OffsetDateTime lastLoginAt) {
    public static UserSummary from(UserAccount user) {
      return new UserSummary(user.id(), user.username(), user.displayName(), user.roleCode(), user.status(),
          user.planCode(), user.quotaTotal(), user.quotaUsed(), user.remainingQuota(), user.lastLoginAt());
    }
  }

  public record PlanDto(String code, String name, int levelOrder, int monthlyQuota, boolean ragEnabled,
                        boolean prioritySupport, String description, List<String> benefits) {
  }

  public record QuotaLedgerDto(Long id, Long userId, String changeType, int deltaCount, int beforeTotal,
                               int beforeUsed, int afterTotal, int afterUsed, String reason, String refType,
                               Long refId, Long operatorUserId, OffsetDateTime createdAt) {
  }

  public record ModelConfigDto(Long id, Long userId, String configName, String providerCode, String baseUrl,
                               String modelName, String authType, boolean hasApiKey, boolean enabled,
                               String lastTestStatus, String lastTestMessage, OffsetDateTime lastTestAt) {
  }

  public record ModelConfigInternal(Long id, Long userId, String configName, String providerCode, String baseUrl,
                                    String modelName, String authType, String apiKeyCiphertext, boolean enabled,
                                    String lastTestStatus, String lastTestMessage, OffsetDateTime lastTestAt) {
  }
}
