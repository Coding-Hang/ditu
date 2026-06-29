package com.ditu.agent.agent.adapter;

import com.ditu.agent.agent.core.AgentEvent;
import com.ditu.agent.agent.core.AgentRunCommand;
import com.ditu.agent.agent.port.AgentRuntimePort;
import com.ditu.agent.rag.RagSearchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 本地 Mock Agent。
 *
 * <p>在没有真实模型服务时仍然输出标准 SSE 事件，便于验证次数扣减、多轮 Memory、RAG 上下文和事件落库。</p>
 */
@Primary
@Component
@ConditionalOnProperty(name = "ditu.agent.runtime", havingValue = "mock", matchIfMissing = true)
public class MockAgentRuntimeAdapter implements AgentRuntimePort {
  private final RagSearchService ragSearchService;

  public MockAgentRuntimeAdapter(RagSearchService ragSearchService) {
    this.ragSearchService = ragSearchService;
  }

  @Override
  public Flux<AgentEvent> run(AgentRunCommand command) {
    if (command.userMessage().toLowerCase().contains("fail-agent")) {
      return Flux.just(AgentEvent.of("run.started", Map.of("runId", command.runId(), "status", "RUNNING")),
          AgentEvent.of("error", Map.of("runId", command.runId(), "code", "AGENT_RUN_FAILED", "message", "Mock Agent 失败")));
    }
    List<AgentEvent> events = new ArrayList<>();
    Map<String, Object> started = new HashMap<>();
    started.put("runId", command.runId());
    started.put("status", "RUNNING");
    started.put("modelName", command.modelConnection().modelName());
    started.put("modelConfigId", command.modelConnection().modelConfigId());
    events.add(AgentEvent.of("run.started", started));
    String answer = answerFor(command);
    List<String> parts = split(answer);
    for (int i = 0; i < parts.size(); i++) {
      String part = parts.get(i);
      events.add(AgentEvent.of("message.delta", Map.of("runId", command.runId(), "delta", part)));
      if (i == 0) {
        var rag = ragSearchService.search(command.userId(), command.userMessage(), 5);
        if (!rag.isEmpty()) {
          events.add(AgentEvent.of("rag.context", Map.of("runId", command.runId(), "chunks", rag)));
        }
      }
    }
    events.add(AgentEvent.of("message.done", Map.of("runId", command.runId(), "content", answer)));
    return Flux.fromIterable(events).concatMap(event -> Mono.just(event));
  }

  private String answerFor(AgentRunCommand command) {
    int memoryTurns = command.memory() == null ? 0 : command.memory().size();
    return "已根据滴兔知识产权咨询流程处理你的问题：" + command.userMessage()
        + "。本次使用模型 " + command.modelConnection().modelName()
        + "，并参考同一会话历史 " + memoryTurns + " 条。建议先确认主体资质、权利类型、材料清单和后续人工复核节点。";
  }

  private List<String> split(String answer) {
    List<String> parts = new ArrayList<>();
    for (int i = 0; i < answer.length(); i += 24) {
      parts.add(answer.substring(i, Math.min(answer.length(), i + 24)));
    }
    return parts;
  }
}
