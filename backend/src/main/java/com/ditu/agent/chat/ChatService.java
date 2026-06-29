package com.ditu.agent.chat;

import com.ditu.agent.agent.core.AgentEvent;
import com.ditu.agent.agent.core.AgentRunCommand;
import com.ditu.agent.agent.llm.ModelConnectionResolver;
import com.ditu.agent.agent.memory.ConversationMemoryStore;
import com.ditu.agent.agent.port.AgentRuntimePort;
import com.ditu.agent.chat.ChatDtos.ConversationDto;
import com.ditu.agent.chat.ChatDtos.MessageDto;
import com.ditu.agent.chat.ChatDtos.RunInfo;
import com.ditu.agent.chat.ChatDtos.SendMessageResponse;
import com.ditu.agent.chat.ChatDtos.StoredEvent;
import com.ditu.agent.common.BusinessException;
import com.ditu.agent.common.ErrorCode;
import com.ditu.agent.common.PageResponse;
import com.ditu.agent.user.QuotaService;
import com.ditu.agent.user.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

/**
 * 会话与 SSE 应用服务。
 *
 * <p>这里串起账号状态、次数预占、模型链接解析、Agent 事件落库、成功扣次和失败回滚，是 CHAT 与 AGENT 的事务边界。</p>
 */
@Service
public class ChatService {
  private final ChatRepository chatRepository;
  private final UserRepository userRepository;
  private final QuotaService quotaService;
  private final ModelConnectionResolver modelConnectionResolver;
  private final ConversationMemoryStore memoryStore;
  private final AgentRuntimePort agentRuntimePort;

  public ChatService(ChatRepository chatRepository, UserRepository userRepository, QuotaService quotaService,
                     ModelConnectionResolver modelConnectionResolver, ConversationMemoryStore memoryStore,
                     AgentRuntimePort agentRuntimePort) {
    this.chatRepository = chatRepository;
    this.userRepository = userRepository;
    this.quotaService = quotaService;
    this.modelConnectionResolver = modelConnectionResolver;
    this.memoryStore = memoryStore;
    this.agentRuntimePort = agentRuntimePort;
  }

  @Transactional
  public ConversationDto createConversation(Long userId, String title) {
    long id = chatRepository.createConversation(userId, title);
    return chatRepository.findConversationForUser(id, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND, "会话创建失败"));
  }

  public PageResponse<ConversationDto> pageConversations(Long userId, int page, int pageSize) {
    return chatRepository.pageConversations(userId, page, pageSize);
  }

  public PageResponse<MessageDto> pageMessages(Long userId, Long conversationId, int page, int pageSize) {
    requireConversation(conversationId, userId);
    return chatRepository.pageMessages(conversationId, userId, page, pageSize);
  }

  @Transactional
  public SendMessageResponse sendMessage(Long userId, Long conversationId, String content) {
    requireConversation(conversationId, userId);
    var user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户不存在"));
    if (!"ACTIVE".equals(user.status())) {
      throw new BusinessException(ErrorCode.USER_DISABLED, "账号已停用，不能发起会话");
    }
    var modelConnection = modelConnectionResolver.resolveForUser(userId);
    int sequenceNo = chatRepository.nextSequence(conversationId);
    long userMessageId = chatRepository.insertMessage(conversationId, userId, "USER", sequenceNo, content, null, 0);
    long runId = chatRepository.createRun(conversationId, userId, userMessageId, modelConnection.modelConfigId(),
        modelConnection.modelName());
    quotaService.reserveForRun(userId, runId);
    return new SendMessageResponse(userMessageId, runId, conversationId, 1);
  }

  public Flux<ServerSentEvent<Map<String, Object>>> streamEvents(Long userId, Long conversationId, Long runId,
                                                                  Integer lastEventId) {
    RunInfo run = chatRepository.findRunForUser(conversationId, runId, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND, "会话运行不存在或无权访问"));
    if ("RUNNING".equals(run.status()) && !chatRepository.hasEvents(runId)) {
      generateAndPersist(run);
    }
    return Flux.fromIterable(chatRepository.listEventsAfter(runId, lastEventId == null ? 0 : lastEventId))
        .filter(StoredEvent::visibleToUser)
        .map(event -> ServerSentEvent.<Map<String, Object>>builder(event.payload())
            .event(event.eventType())
            .id(String.valueOf(event.sequenceNo()))
            .build());
  }

  @Transactional
  protected void generateAndPersist(RunInfo run) {
    String userMessage = chatRepository.findUserMessage(run.userMessageId());
    var modelConnection = modelConnectionResolver.resolveForUser(run.userId());
    var memory = memoryStore.loadMemory(run.conversationId(), 20);
    var command = new AgentRunCommand(run.id(), run.conversationId(), run.userId(), userMessage, memory, modelConnection);
    List<AgentEvent> events = agentRuntimePort.run(command).collectList().block();
    if (events == null) {
      events = List.of();
    }
    StringBuilder content = new StringBuilder();
    boolean failed = false;
    String errorMessage = null;
    for (AgentEvent event : events) {
      if ("message.delta".equals(event.eventType())) {
        Object delta = event.payload().get("delta");
        if (delta != null) {
          content.append(delta);
        }
      }
      if ("message.done".equals(event.eventType())) {
        Object done = event.payload().get("content");
        if (done != null) {
          content.setLength(0);
          content.append(done);
        }
      }
      if ("error".equals(event.eventType())) {
        failed = true;
        errorMessage = String.valueOf(event.payload().getOrDefault("message", "Agent 执行失败"));
      }
    }
    if (failed) {
      persistEvents(events, run);
      chatRepository.updateRunStatus(run.id(), "FAILED", "AGENT_RUN_FAILED", errorMessage);
      quotaService.rollbackRun(run.userId(), run.id(), "Agent run 失败回滚预占次数");
      return;
    }
    int assistantSequence = chatRepository.nextSequence(run.conversationId());
    long assistantMessageId = chatRepository.insertMessage(run.conversationId(), run.userId(), "ASSISTANT",
        assistantSequence, content.toString(), run.id(), 1);
    List<AgentEvent> adjusted = new ArrayList<>();
    for (AgentEvent event : events) {
      if ("message.done".equals(event.eventType())) {
        Map<String, Object> payload = new HashMap<>(event.payload());
        payload.put("messageId", assistantMessageId);
        adjusted.add(new AgentEvent(event.eventType(), event.visibleToUser(), payload));
      } else {
        adjusted.add(event);
      }
    }
    persistEvents(adjusted, run);
    quotaService.commitRun(run.userId(), run.id());
    int remaining = userRepository.findById(run.userId()).map(user -> user.remainingQuota()).orElse(0);
    chatRepository.insertEvent(run.id(), run.conversationId(), "quota.updated", true,
        Map.of("runId", run.id(), "quotaCost", 1, "remainingQuota", remaining));
    chatRepository.updateRunStatus(run.id(), "COMPLETED", null, null);
    chatRepository.insertEvent(run.id(), run.conversationId(), "run.completed", true,
        Map.of("runId", run.id(), "status", "COMPLETED"));
  }

  private void persistEvents(List<AgentEvent> events, RunInfo run) {
    for (AgentEvent event : events) {
      chatRepository.insertEvent(run.id(), run.conversationId(), event.eventType(), event.visibleToUser(),
          event.payload());
    }
  }

  private void requireConversation(Long conversationId, Long userId) {
    chatRepository.findConversationForUser(conversationId, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND, "会话不存在或无权访问"));
  }
}
