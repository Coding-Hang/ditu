package com.ditu.agent.infra;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

class TestcontainersPostgresAvailabilityTest {

  @Test
  void startsPostgresqlContainerWhenDockerIsAvailable() {
    Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
        "Docker is not available in this workstation, skipping Testcontainers startup.");

    try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg15")
        .withDatabaseName("ditu")
        .withUsername("ditu")
        .withPassword("ditu")) {
      postgres.start();
      assertThat(postgres.isRunning()).isTrue();
    }
  }
}
