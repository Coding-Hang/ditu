package com.ditu.agent.user;

import com.ditu.agent.agent.llm.ModelConnection;
import com.ditu.agent.common.BusinessException;
import com.ditu.agent.infra.AuditService;
import com.ditu.agent.infra.CryptoService;
import com.ditu.agent.infra.DituProperties;
import com.ditu.agent.infra.OpenAiCompatibleChatClient;
import com.ditu.agent.user.UserDtos.ModelConfigDto;
import com.ditu.agent.user.UserDtos.ModelConfigInternal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelConfigServiceTest {

  @Test
  void testCallsRealCompatibleClientAndStoresSuccessStatus() {
    var cryptoService = new CryptoService(properties());
    var repository = mock(UserModelConfigRepository.class);
    var chatClient = mock(OpenAiCompatibleChatClient.class);
    var service = new ModelConfigService(repository, cryptoService, mock(AuditService.class), chatClient);
    when(repository.findInternalByUserId(7L)).thenReturn(Optional.of(internal(cryptoService)));
    when(repository.findPublicByUserId(7L)).thenReturn(Optional.of(publicDto("SUCCESS", "连接成功")));
    when(chatClient.chat(any(ModelConnection.class), anyList(), eq("ping"))).thenReturn("OK");

    ModelConfigDto dto = service.test(7L, "ping", 1L);

    assertThat(dto.lastTestStatus()).isEqualTo("SUCCESS");
    var captor = ArgumentCaptor.forClass(ModelConnection.class);
    verify(chatClient).chat(captor.capture(), anyList(), eq("ping"));
    assertThat(captor.getValue().apiKey()).isEqualTo("sk-user");
    verify(repository).updateTestResult(eq(7L), eq("SUCCESS"), any(String.class));
  }

  @Test
  void testStoresFailedStatusWhenModelCallFails() {
    var cryptoService = new CryptoService(properties());
    var repository = mock(UserModelConfigRepository.class);
    var chatClient = mock(OpenAiCompatibleChatClient.class);
    var service = new ModelConfigService(repository, cryptoService, mock(AuditService.class), chatClient);
    when(repository.findInternalByUserId(7L)).thenReturn(Optional.of(internal(cryptoService)));
    when(chatClient.chat(any(ModelConnection.class), anyList(), eq("ping")))
        .thenThrow(new IllegalStateException("bad gateway"));

    assertThatThrownBy(() -> service.test(7L, "ping", 1L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("连接失败");
    verify(repository).updateTestResult(eq(7L), eq("FAILED"), any(String.class));
  }

  private ModelConfigInternal internal(CryptoService cryptoService) {
    return new ModelConfigInternal(22L, 7L, "客户模型", "CUSTOM", "https://llm.example.com/v1",
        "qwen", "API_KEY", cryptoService.encrypt("sk-user"), true, "UNTESTED", null, null);
  }

  private ModelConfigDto publicDto(String status, String message) {
    return new ModelConfigDto(22L, 7L, "客户模型", "CUSTOM", "https://llm.example.com/v1",
        "qwen", "API_KEY", true, true, status, message, null);
  }

  private DituProperties properties() {
    return new DituProperties(
        new DituProperties.Agent("mock"),
        new DituProperties.Auth("unit-test-secret", 7200, 604800),
        new DituProperties.Crypto("crypto-secret"),
        new DituProperties.Model("https://platform.example/v1", "platform-model", "BEARER", "sk-platform"),
        new DituProperties.Embedding("mock", 1536),
        "./data");
  }
}
