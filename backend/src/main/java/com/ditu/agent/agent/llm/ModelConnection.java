package com.ditu.agent.agent.llm;

/**
 * Agent 运行时使用的大模型连接。
 *
 * <p>apiKey 只允许在内存中传递给 agent/adapter，不能进入日志、SSE 事件或接口响应。</p>
 */
public record ModelConnection(Long modelConfigId, String baseUrl, String modelName, String authType, String apiKey,
                              boolean platformDefault) {
}
