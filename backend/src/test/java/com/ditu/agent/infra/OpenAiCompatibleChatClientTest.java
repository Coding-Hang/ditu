package com.ditu.agent.infra;

import com.ditu.agent.agent.core.AgentRunCommand.MemoryMessage;
import com.ditu.agent.agent.llm.ModelConnection;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleChatClientTest {

  @Test
  void callsFullChatCompletionsEndpointWithRawApiKeyAuthorization() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.setExecutor(Executors.newSingleThreadExecutor());
    final String[] authorization = new String[1];
    server.createContext("/compatible-mode/v1/chat/completions", exchange -> {
      authorization[0] = exchange.getRequestHeaders().getFirst("Authorization");
      byte[] body = """
          {"choices":[{"message":{"role":"assistant","content":"测试成功"}}]}
          """.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    try {
      String url = "http://localhost:" + server.getAddress().getPort() + "/compatible-mode/v1/chat/completions";
      String answer = new OpenAiCompatibleChatClient().chat(
          new ModelConnection(null, url, "qwen3.7-max", "API_KEY", "sk-test", true),
          List.of(new MemoryMessage("USER", "上一轮")), "你是谁？");

      assertThat(answer).isEqualTo("测试成功");
      assertThat(authorization[0]).isEqualTo("sk-test");
    } finally {
      server.stop(0);
    }
  }
}
