package com.ditu.agent.agent.port;

import com.ditu.agent.agent.core.AgentEvent;
import com.ditu.agent.agent.core.AgentRunCommand;
import reactor.core.publisher.Flux;

/**
 * Agent 运行时端口。
 *
 * <p>chat 包只依赖此接口，不感知 AgentScope 内部类；后续可在 Mock 与真实 AgentScope 适配器之间切换。</p>
 */
public interface AgentRuntimePort {
  Flux<AgentEvent> run(AgentRunCommand command);
}
