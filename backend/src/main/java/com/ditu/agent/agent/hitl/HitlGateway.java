package com.ditu.agent.agent.hitl;

import org.springframework.stereotype.Component;

/**
 * 人工介入网关。
 *
 * <p>Agent 判断需要人工处理时通过该边界创建工单，避免 Agent 适配器直接操作 ticket 表。</p>
 */
@Component
public class HitlGateway {

  public boolean shouldTransferToHuman(String userMessage) {
    return userMessage != null && (userMessage.contains("人工") || userMessage.contains("客服"));
  }
}
