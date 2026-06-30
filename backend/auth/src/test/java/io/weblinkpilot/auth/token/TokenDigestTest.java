package io.weblinkpilot.auth.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenDigestTest {

  @Test
  void createsStableUrlSafeSha256Digest() {
    String digest = TokenDigest.sha256Base64Url("refresh-token");

    assertThat(digest).isEqualTo("DrF2Q9TpJhFjeDpCCFnJLH0hL6liQQahK1EK--wmYSA");
    assertThat(digest).doesNotContain("=", "+", "/");
  }
}
