package com.ditu.agent.rag;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RagSearchSqlPolicyTest {

  @Test
  void ragSearchSqlKeepsGlobalOrCurrentUserFilter() throws Exception {
    String source = Files.readString(Path.of("src/main/java/com/ditu/agent/rag/RagSearchService.java"));

    assertThat(source).contains("rc.scope = 'GLOBAL'");
    assertThat(source).contains("rc.scope = 'USER' AND rc.owner_user_id = ?");
  }
}
