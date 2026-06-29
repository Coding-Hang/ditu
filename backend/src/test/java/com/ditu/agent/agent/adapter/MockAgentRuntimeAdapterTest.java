package com.ditu.agent.agent.adapter;

import com.ditu.agent.agent.core.AgentRunCommand;
import com.ditu.agent.agent.llm.ModelConnection;
import com.ditu.agent.rag.RagSearchService;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MockAgentRuntimeAdapterTest {

  @Test
  void emitsStandardStreamingEventsBeforeQuotaEventIsAppendedByChatService() {
    var ragSearchService = mock(RagSearchService.class);
    when(ragSearchService.search(1L, "商标注册", 5)).thenReturn(List.of());
    var adapter = new MockAgentRuntimeAdapter(ragSearchService);
    var command = new AgentRunCommand(3L, 2L, 1L, "商标注册", List.of(),
        new ModelConnection(null, "http://model", "mock-model", "NONE", null, true));

    var events = adapter.run(command).collectList().block();

    assertThat(events).isNotNull();
    assertThat(events.stream().map(event -> event.eventType()).toList())
        .startsWith("run.started", "message.delta")
        .contains("message.done");
  }
}
