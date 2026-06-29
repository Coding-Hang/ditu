package com.ditu.agent.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 滴兔智能体外部化配置。
 *
 * <p>模型链接、密钥、Embedding 和文件存储都从环境变量进入，生产环境不会把真实凭据写入仓库。</p>
 */
@ConfigurationProperties(prefix = "ditu")
public record DituProperties(Auth auth, Crypto crypto, Model model, Embedding embedding, String fileStorageRoot) {

  public record Auth(String tokenSecret, long accessTokenSeconds, long refreshTokenSeconds) {
  }

  public record Crypto(String secret) {
  }

  public record Model(String defaultBaseUrl, String defaultModelName, String defaultAuthType, String defaultApiKey) {
  }

  public record Embedding(String model, int dimension) {
  }
}
