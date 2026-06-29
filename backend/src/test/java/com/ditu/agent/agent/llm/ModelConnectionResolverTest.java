package com.ditu.agent.agent.llm;

import com.ditu.agent.infra.CryptoService;
import com.ditu.agent.infra.DituProperties;
import com.ditu.agent.user.UserDtos.ModelConfigInternal;
import com.ditu.agent.user.UserModelConfigRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModelConnectionResolverTest {

  @Test
  void usesEnabledUserConfigBeforePlatformDefault() {
    var properties = properties();
    var cryptoService = new CryptoService(properties);
    var repository = mock(UserModelConfigRepository.class);
    when(repository.findEnabledInternalByUserId(10L)).thenReturn(Optional.of(
        new ModelConfigInternal(99L, 10L, "customer", "CUSTOM", "https://customer.example/v1",
            "customer-model", "API_KEY", cryptoService.encrypt("sk-user"), true, "SUCCESS", "ok", null)));
    var resolver = new ModelConnectionResolver(repository, cryptoService, properties);

    ModelConnection connection = resolver.resolveForUser(10L);

    assertThat(connection.modelConfigId()).isEqualTo(99L);
    assertThat(connection.modelName()).isEqualTo("customer-model");
    assertThat(connection.apiKey()).isEqualTo("sk-user");
    assertThat(connection.platformDefault()).isFalse();
  }

  @Test
  void fallsBackToPlatformDefaultWhenNoEnabledUserConfig() {
    var properties = properties();
    var repository = mock(UserModelConfigRepository.class);
    when(repository.findEnabledInternalByUserId(11L)).thenReturn(Optional.empty());
    var resolver = new ModelConnectionResolver(repository, new CryptoService(properties), properties);

    ModelConnection connection = resolver.resolveForUser(11L);

    assertThat(connection.modelConfigId()).isNull();
    assertThat(connection.modelName()).isEqualTo("platform-model");
    assertThat(connection.platformDefault()).isTrue();
  }

  private DituProperties properties() {
    return new DituProperties(
        new DituProperties.Auth("unit-test-secret", 7200, 604800),
        new DituProperties.Crypto("crypto-secret"),
        new DituProperties.Model("https://platform.example/v1", "platform-model", "BEARER", "sk-platform"),
        new DituProperties.Embedding("mock", 1536),
        "./data");
  }
}
