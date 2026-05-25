package io.weblinkpilot.url.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.weblinkpilot.shared.contracts.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiErrorResponseFactoryTest {

  @Test
  void createsConsistentErrorPayload() {
    ApiErrorResponse response =
        ApiErrorResponseFactory.create(
            HttpStatus.CONFLICT, "CONFLICT", "Alias already exists", "/api/v1/urls");

    assertThat(response.status()).isEqualTo(409);
    assertThat(response.error()).isEqualTo("Conflict");
    assertThat(response.code()).isEqualTo("CONFLICT");
    assertThat(response.message()).isEqualTo("Alias already exists");
    assertThat(response.path()).isEqualTo("/api/v1/urls");
    assertThat(response.timestamp()).isNotNull();
  }
}
