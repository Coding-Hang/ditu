package com.ditu.agent.auth;

import com.ditu.agent.infra.DituProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

  @Test
  void accessTokenCarriesUserRoleAndRejectsWrongType() {
    var properties = new DituProperties(
        new DituProperties.Auth("unit-test-secret", 7200, 604800),
        new DituProperties.Crypto("crypto"),
        new DituProperties.Model("http://default", "default-model", "NONE", ""),
        new DituProperties.Embedding("mock", 1536),
        "./data");
    var tokenService = new TokenService(properties);

    String accessToken = tokenService.issueAccessToken(7L, "ADMIN");

    var parsed = tokenService.parse(accessToken, "ACCESS");
    assertThat(parsed.userId()).isEqualTo(7L);
    assertThat(parsed.roleCode()).isEqualTo("ADMIN");
    assertThatThrownBy(() -> tokenService.parse(accessToken, "REFRESH"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
