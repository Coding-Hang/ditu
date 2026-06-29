package com.ditu.agent.agent.core;

import java.util.Map;

/**
 * 统一 Agent 事件。
 *
 * <p>Mock Agent 和 AgentScope Adapter 都输出该事件，chat 包负责落库并转换为 SSE，确保事件流和数据库一致。</p>
 */
public record AgentEvent(String eventType, boolean visibleToUser, Map<String, Object> payload) {

  public static AgentEvent of(String eventType, Map<String, Object> payload) {
    return new AgentEvent(eventType, true, payload);
  }

  public static AgentEvent internal(String eventType, Map<String, Object> payload) {
    return new AgentEvent(eventType, false, payload);
  }
}
