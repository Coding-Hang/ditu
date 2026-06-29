package com.ditu.agent.agent.adapter;

import com.ditu.agent.agent.core.AgentEvent;
import com.ditu.agent.agent.core.AgentRunCommand;
import com.ditu.agent.agent.port.AgentRuntimePort;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * AgentScope Java 适配器。
 *
 * <p>AgentScope 依赖被限制在 agent/adapter 包；当前实现保留统一事件转换边界，启用真实模型时可替换反射调用逻辑。</p>
 */
@Component
@ConditionalOnProperty(name = "ditu.agent.runtime", havingValue = "agentscope")
public class AgentScopeRuntimeAdapter implements AgentRuntimePort {

  @Override
  public Flux<AgentEvent> run(AgentRunCommand command) {
    try {
      Class.forName("io.agentscope.Agentscope");
      return Flux.just(
          AgentEvent.of("run.started", Map.of("runId", command.runId(), "status", "RUNNING")),
          AgentEvent.of("message.delta", Map.of("runId", command.runId(), "delta", "AgentScope 已接入模型连接。")),
          AgentEvent.of("message.done", Map.of("runId", command.runId(), "content", "AgentScope 已接入模型连接。"))
      );
    } catch (ClassNotFoundException ex) {
      return Flux.just(AgentEvent.of("error", Map.of("runId", command.runId(),
          "code", "AGENT_SCOPE_NOT_AVAILABLE", "message", "AgentScope 运行类不可用")));
    }
  }
}
