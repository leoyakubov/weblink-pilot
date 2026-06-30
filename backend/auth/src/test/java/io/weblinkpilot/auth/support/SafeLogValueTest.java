package io.weblinkpilot.auth.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SafeLogValueTest {

  @Test
  void emailMasksLocalPartAndDomain() {
    assertThat(SafeLogValue.email("Alice.Example@Example.com")).isEqualTo("a***@***.com");
  }

  @Test
  void emailHandlesBlankAndMalformedValues() {
    assertThat(SafeLogValue.email(" ")).isEqualTo("blank");
    assertThat(SafeLogValue.email("not-an-email")).isEqualTo("masked");
  }
}
