package com.ditu.agent.agent.adapter;

import com.ditu.agent.agent.core.AgentEvent;
import com.ditu.agent.agent.core.AgentRunCommand;
import com.ditu.agent.agent.port.AgentRuntimePort;
import com.ditu.agent.infra.OpenAiCompatibleChatClient;
import com.ditu.agent.rag.RagSearchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 真实 OpenAI-compatible 模型运行时。
 *
 * <p>用于连接已验证的大模型地址；chat 包仍只依赖 AgentRuntimePort，模型地址和密钥由 ModelConnectionResolver 按用户解析。</p>
 */
@Component
@ConditionalOnProperty(name = "ditu.agent.runtime", havingValue = "compatible")
public class CompatibleModelRuntimeAdapter implements AgentRuntimePort {
  private final OpenAiCompatibleChatClient chatClient;
  private final RagSearchService ragSearchService;

  public CompatibleModelRuntimeAdapter(OpenAiCompatibleChatClient chatClient, RagSearchService ragSearchService) {
    this.chatClient = chatClient;
    this.ragSearchService = ragSearchService;
  }

  @Override
  public Flux<AgentEvent> run(AgentRunCommand command) {
    try {
      List<AgentEvent> events = new ArrayList<>();
      Map<String, Object> started = new HashMap<>();
      started.put("runId", command.runId());
      started.put("status", "RUNNING");
      started.put("modelName", command.modelConnection().modelName());
      started.put("modelConfigId", command.modelConnection().modelConfigId());
      events.add(AgentEvent.of("run.started", started));

      String answer = chatClient.chat(command.modelConnection(), command.memory(), command.userMessage());
      List<String> parts = split(answer);
      for (int i = 0; i < parts.size(); i++) {
        events.add(AgentEvent.of("message.delta", Map.of("runId", command.runId(), "delta", parts.get(i))));
        if (i == 0) {
          var rag = ragSearchService.search(command.userId(), command.userMessage(), 5);
          if (!rag.isEmpty()) {
            events.add(AgentEvent.of("rag.context", Map.of("runId", command.runId(), "chunks", rag)));
          }
        }
      }
      events.add(AgentEvent.of("message.done", Map.of("runId", command.runId(), "content", answer)));
      return Flux.fromIterable(events);
    } catch (Exception ex) {
      return Flux.just(
          AgentEvent.of("run.started", Map.of("runId", command.runId(), "status", "RUNNING")),
          AgentEvent.of("error", Map.of("runId", command.runId(), "code", "MODEL_CONNECTION_FAILED",
              "message", "模型调用失败，请检查用户级或平台默认模型链接"))
      );
    }
  }

  private List<String> split(String answer) {
    List<String> parts = new ArrayList<>();
    for (int i = 0; i < answer.length(); i += 24) {
      parts.add(answer.substring(i, Math.min(answer.length(), i + 24)));
    }
    return parts;
  }
}
