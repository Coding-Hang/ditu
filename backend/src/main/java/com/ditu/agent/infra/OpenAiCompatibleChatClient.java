package com.ditu.agent.infra;

import com.ditu.agent.agent.core.AgentRunCommand.MemoryMessage;
import com.ditu.agent.agent.llm.ModelConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * OpenAI-compatible chat/completions 客户端。
 *
 * <p>阿里云百炼 compatible-mode、OpenAI 兼容网关和用户自有模型链接都走这个边界；密钥只进入请求头，不写日志、不入库、不进 SSE。</p>
 */
@Component
public class OpenAiCompatibleChatClient {
  private final WebClient webClient = WebClient.builder().build();

  public String chat(ModelConnection connection, List<MemoryMessage> memory, String userMessage) {
    List<Map<String, String>> messages = new ArrayList<>();
    messages.add(Map.of("role", "system", "content", "You are 滴兔智能体, a helpful intellectual-property assistant."));
    for (MemoryMessage message : memory) {
      messages.add(Map.of("role", normalizeRole(message.role()), "content", message.content()));
    }
    messages.add(Map.of("role", "user", "content", userMessage));

    Map<?, ?> response = webClient.post()
        .uri(chatCompletionUri(connection.baseUrl()))
        .headers(headers -> {
          headers.set("Content-Type", "application/json");
          String authHeader = authorizationHeader(connection);
          if (authHeader != null && !authHeader.isBlank()) {
            headers.set("Authorization", authHeader);
          }
        })
        .bodyValue(Map.of("model", connection.modelName(), "messages", messages))
        .retrieve()
        .bodyToMono(Map.class)
        .block();
    return extractAssistantContent(response);
  }

  private URI chatCompletionUri(String configuredUrl) {
    String normalized = configuredUrl.endsWith("/") ? configuredUrl.substring(0, configuredUrl.length() - 1) : configuredUrl;
    if (normalized.endsWith("/chat/completions")) {
      return URI.create(normalized);
    }
    return URI.create(normalized + "/chat/completions");
  }

  private String authorizationHeader(ModelConnection connection) {
    if (connection.apiKey() == null || connection.apiKey().isBlank() || "NONE".equalsIgnoreCase(connection.authType())) {
      return null;
    }
    if ("BEARER".equalsIgnoreCase(connection.authType())) {
      return "Bearer " + connection.apiKey();
    }
    return connection.apiKey();
  }

  private String normalizeRole(String role) {
    return switch (role) {
      case "ASSISTANT" -> "assistant";
      case "SYSTEM" -> "system";
      case "TOOL" -> "tool";
      default -> "user";
    };
  }

  @SuppressWarnings("unchecked")
  private String extractAssistantContent(Map<?, ?> response) {
    if (response == null) {
      throw new IllegalStateException("模型响应为空");
    }
    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
    if (choices == null || choices.isEmpty()) {
      throw new IllegalStateException("模型响应缺少 choices");
    }
    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
    Object content = message == null ? null : message.get("content");
    if (content == null || content.toString().isBlank()) {
      throw new IllegalStateException("模型响应缺少 assistant content");
    }
    return content.toString();
  }
}
