package io.weblinkpilot.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class TokenDigest {

  private static final String SHA_256 = "SHA-256";
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

  private TokenDigest() {}

  public static String sha256Base64Url(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance(SHA_256);
      byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return ENCODER.encodeToString(hashed);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Unable to hash token", exception);
    }
  }
}
