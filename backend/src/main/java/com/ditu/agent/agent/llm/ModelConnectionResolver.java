package com.ditu.agent.agent.llm;

import com.ditu.agent.infra.CryptoService;
import com.ditu.agent.infra.DituProperties;
import com.ditu.agent.user.UserModelConfigRepository;
import org.springframework.stereotype.Service;

/**
 * 用户级模型链接解析器。
 *
 * <p>每次 Agent run 启动前必须调用本服务：用户有启用配置时使用用户配置，否则回退平台默认模型链接。</p>
 */
@Service
public class ModelConnectionResolver {
  private final UserModelConfigRepository modelConfigRepository;
  private final CryptoService cryptoService;
  private final DituProperties properties;

  public ModelConnectionResolver(UserModelConfigRepository modelConfigRepository, CryptoService cryptoService,
                                 DituProperties properties) {
    this.modelConfigRepository = modelConfigRepository;
    this.cryptoService = cryptoService;
    this.properties = properties;
  }

  public ModelConnection resolveForUser(Long userId) {
    return modelConfigRepository.findEnabledInternalByUserId(userId)
        .map(config -> new ModelConnection(config.id(), config.baseUrl(), config.modelName(), config.authType(),
            cryptoService.decrypt(config.apiKeyCiphertext()), false))
        .orElseGet(() -> new ModelConnection(null, properties.model().defaultBaseUrl(),
            properties.model().defaultModelName(), properties.model().defaultAuthType(),
            properties.model().defaultApiKey(), true));
  }
}
