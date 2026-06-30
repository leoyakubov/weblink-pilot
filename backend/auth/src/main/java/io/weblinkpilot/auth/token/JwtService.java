package io.weblinkpilot.auth.token;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.weblinkpilot.auth.config.AuthProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  public record JwtClaims(String username, String role, Instant issuedAt, Instant expiresAt) {}

  private static final int MIN_SECRET_LENGTH = 32;
  private static final String JWT_SEGMENT_SEPARATOR_REGEX = "\\.";
  private static final String JWT_SEGMENT_SEPARATOR = ".";
  private static final String JWT_ALGORITHM = "HmacSHA256";
  private static final String HEADER_ALGORITHM = "alg";
  private static final String HEADER_TYPE = "typ";
  private static final String HEADER_ALGORITHM_VALUE = "HS256";
  private static final String HEADER_TYPE_VALUE = "JWT";
  private static final String CLAIM_ISSUER = "iss";
  private static final String CLAIM_SUBJECT = "sub";
  private static final String CLAIM_ROLE = "role";
  private static final String CLAIM_ISSUED_AT = "iat";
  private static final String CLAIM_EXPIRES_AT = "exp";
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private final ObjectMapper objectMapper;
  private final String issuer;
  private final String jwtSecret;
  private final long tokenTtlMinutes;

  public JwtService(ObjectMapper objectMapper, AuthProperties authProperties) {
    this.objectMapper = objectMapper.copy();
    this.issuer = authProperties.getIssuer();
    this.jwtSecret = authProperties.getJwtSecret();
    this.tokenTtlMinutes = authProperties.getTokenTtlMinutes();
  }

  @PostConstruct
  void validateConfiguration() {
    if (jwtSecret == null || jwtSecret.isBlank()) {
      throw new IllegalStateException("JWT secret must be configured via JWT_SECRET");
    }
    if (jwtSecret.trim().length() < MIN_SECRET_LENGTH) {
      throw new IllegalStateException(
          "JWT secret must be at least "
              + MIN_SECRET_LENGTH
              + " characters long to resist brute force attacks");
    }
  }

  public String issueToken(String username, String role) {
    Instant issuedAt = Instant.now();
    Instant expiresAt = issuedAt.plus(Duration.ofMinutes(tokenTtlMinutes));

    Map<String, Object> header = new LinkedHashMap<>();
    header.put(HEADER_ALGORITHM, HEADER_ALGORITHM_VALUE);
    header.put(HEADER_TYPE, HEADER_TYPE_VALUE);

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put(CLAIM_ISSUER, issuer);
    payload.put(CLAIM_SUBJECT, username);
    payload.put(CLAIM_ROLE, role);
    payload.put(CLAIM_ISSUED_AT, issuedAt.getEpochSecond());
    payload.put(CLAIM_EXPIRES_AT, expiresAt.getEpochSecond());

    String headerPart = encodeJson(header);
    String payloadPart = encodeJson(payload);
    String signaturePart = sign(headerPart + JWT_SEGMENT_SEPARATOR + payloadPart);
    return headerPart + JWT_SEGMENT_SEPARATOR + payloadPart + JWT_SEGMENT_SEPARATOR + signaturePart;
  }

  public Optional<JwtClaims> parseToken(String token) {
    try {
      String[] segments = token.split(JWT_SEGMENT_SEPARATOR_REGEX);
      if (segments.length != 3) {
        return Optional.empty();
      }

      String expectedSignature = sign(segments[0] + JWT_SEGMENT_SEPARATOR + segments[1]);
      if (!MessageDigest.isEqual(DECODER.decode(expectedSignature), DECODER.decode(segments[2]))) {
        return Optional.empty();
      }

      JsonNode payload = objectMapper.readTree(DECODER.decode(segments[1]));
      if (!issuer.equals(payload.path(CLAIM_ISSUER).asText(null))) {
        return Optional.empty();
      }

      String username = payload.path(CLAIM_SUBJECT).asText(null);
      String role = payload.path(CLAIM_ROLE).asText(null);
      long issuedAt = payload.path(CLAIM_ISSUED_AT).asLong(0L);
      long expiresAt = payload.path(CLAIM_EXPIRES_AT).asLong(0L);
      if (username == null || role == null || issuedAt <= 0L || expiresAt <= 0L) {
        return Optional.empty();
      }
      if (Instant.now().getEpochSecond() >= expiresAt) {
        return Optional.empty();
      }

      return Optional.of(
          new JwtClaims(
              username, role, Instant.ofEpochSecond(issuedAt), Instant.ofEpochSecond(expiresAt)));
    } catch (IOException | IllegalArgumentException exception) {
      return Optional.empty();
    }
  }

  private String encodeJson(Map<String, Object> value) {
    try {
      return ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to encode JWT payload", exception);
    }
  }

  private String sign(String data) {
    try {
      Mac mac = Mac.getInstance(JWT_ALGORITHM);
      mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), JWT_ALGORITHM));
      return ENCODER.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
      throw new IllegalStateException("Unable to sign JWT", exception);
    }
  }
}
