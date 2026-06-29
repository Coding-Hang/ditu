package com.ditu.agent.agent.core;

import com.ditu.agent.agent.llm.ModelConnection;
import java.util.List;

/**
 * Agent 单次运行命令。
 *
 * <p>chat 包在创建 agent_run 后组装此命令，包含当前用户消息、多轮 Memory 和已解析的大模型链接。</p>
 */
public record AgentRunCommand(Long runId, Long conversationId, Long userId, String userMessage,
                              List<MemoryMessage> memory, ModelConnection modelConnection) {

  public record MemoryMessage(String role, String content) {
  }
}
