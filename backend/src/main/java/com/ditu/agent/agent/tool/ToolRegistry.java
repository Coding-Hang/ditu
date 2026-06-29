package com.ditu.agent.agent.tool;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Agent 工具注册表。
 *
 * <p>当前提供套餐查询、RAG 检索、工单创建三类工具名称，真实 AgentScope 适配器可按这些稳定名称注册工具。</p>
 */
@Component
public class ToolRegistry {

  public List<String> toolNames() {
    return List.of("plan_lookup", "rag_search", "ticket_create");
  }
}
