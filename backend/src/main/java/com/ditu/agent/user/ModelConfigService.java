package com.ditu.agent.user;

import com.ditu.agent.agent.llm.ModelConnectionResolver;
import com.ditu.agent.common.BusinessException;
import com.ditu.agent.common.ErrorCode;
import com.ditu.agent.infra.AuditService;
import com.ditu.agent.infra.CryptoService;
import com.ditu.agent.user.UserDtos.ModelConfigDto;
import java.net.URI;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端用户模型链接应用服务。
 *
 * <p>保存、测试和停用都写审计日志；查询接口不返回密钥，Agent 运行时通过 ModelConnectionResolver 解密。</p>
 */
@Service
public class ModelConfigService {
  private final UserModelConfigRepository repository;
  private final CryptoService cryptoService;
  private final AuditService auditService;
  private final ModelConnectionResolver resolver;

  public ModelConfigService(UserModelConfigRepository repository, CryptoService cryptoService,
                            AuditService auditService, ModelConnectionResolver resolver) {
    this.repository = repository;
    this.cryptoService = cryptoService;
    this.auditService = auditService;
    this.resolver = resolver;
  }

  public ModelConfigDto get(Long userId) {
    return repository.findPublicByUserId(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_CONFIG_NOT_FOUND, "用户模型链接不存在"));
  }

  @Transactional
  public ModelConfigDto save(Long userId, SaveModelConfigCommand command, Long actorUserId) {
    validateUrl(command.baseUrl());
    if ((command.authType().equals("API_KEY") || command.authType().equals("BEARER"))
        && repository.findInternalByUserId(userId).isEmpty()
        && (command.apiKey() == null || command.apiKey().isBlank())) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "首次保存鉴权模型必须提供密钥");
    }
    String encrypted = command.apiKey() == null || command.apiKey().isBlank() ? null : cryptoService.encrypt(command.apiKey());
    long id = repository.upsert(userId, command.configName(), command.providerCode(), command.baseUrl(),
        command.modelName(), command.authType(), encrypted, command.enabled(), actorUserId);
    auditService.record(actorUserId, "USER_MODEL_SAVE", "APP_USER_MODEL_CONFIG", id,
        Map.of("userId", userId, "modelName", command.modelName(), "enabled", command.enabled()));
    return get(userId);
  }

  @Transactional
  public ModelConfigDto test(Long userId, String message, Long actorUserId) {
    var config = repository.findInternalByUserId(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_CONFIG_NOT_FOUND, "用户模型链接不存在"));
    String status = config.baseUrl() != null && !config.baseUrl().isBlank() ? "SUCCESS" : "FAILED";
    String resultMessage = status.equals("SUCCESS") ? "连接成功: " + config.modelName() : "模型地址为空";
    repository.updateTestResult(userId, status, resultMessage);
    auditService.record(actorUserId, "USER_MODEL_TEST", "APP_USER_MODEL_CONFIG", config.id(),
        Map.of("userId", userId, "status", status, "message", message == null ? "ping" : message));
    if ("FAILED".equals(status)) {
      throw new BusinessException(ErrorCode.MODEL_CONNECTION_FAILED, resultMessage);
    }
    return get(userId);
  }

  @Transactional
  public void disable(Long userId, Long actorUserId) {
    repository.disable(userId, actorUserId);
    auditService.record(actorUserId, "USER_MODEL_DISABLE", "APP_USER_MODEL_CONFIG", null, Map.of("userId", userId));
  }

  private void validateUrl(String baseUrl) {
    try {
      URI uri = URI.create(baseUrl);
      if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
        throw new IllegalArgumentException("模型地址必须是 http 或 https");
      }
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模型地址必须是 http 或 https");
    }
  }

  public record SaveModelConfigCommand(String configName, String providerCode, String baseUrl, String modelName,
                                       String authType, String apiKey, boolean enabled) {
  }
}
