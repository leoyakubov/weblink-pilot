package io.weblinkpilot.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.weblinkpilot.auth.config.AuthProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {

    public record JwtClaims(String username, String role, Instant issuedAt, Instant expiresAt) {
    }

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final AuthProperties authProperties;

    public JwtService(ObjectMapper objectMapper, AuthProperties authProperties) {
        this.objectMapper = objectMapper;
        this.authProperties = authProperties;
        if (authProperties.getJwtSecret() == null || authProperties.getJwtSecret().isBlank()) {
            throw new IllegalStateException("JWT secret must be configured via JWT_SECRET");
        }
    }

    public String issueToken(String username, String role) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofMinutes(authProperties.getTokenTtlMinutes()));

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", authProperties.getIssuer());
        payload.put("sub", username);
        payload.put("role", role);
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String headerPart = encodeJson(header);
        String payloadPart = encodeJson(payload);
        String signaturePart = sign(headerPart + "." + payloadPart);
        return headerPart + "." + payloadPart + "." + signaturePart;
    }

    public Optional<JwtClaims> parseToken(String token) {
        try {
            String[] segments = token.split("\\.");
            if (segments.length != 3) {
                return Optional.empty();
            }

            String expectedSignature = sign(segments[0] + "." + segments[1]);
            if (!MessageDigest.isEqual(DECODER.decode(expectedSignature), DECODER.decode(segments[2]))) {
                return Optional.empty();
            }

            JsonNode payload = objectMapper.readTree(DECODER.decode(segments[1]));
            if (!authProperties.getIssuer().equals(payload.path("iss").asText(null))) {
                return Optional.empty();
            }

            String username = payload.path("sub").asText(null);
            String role = payload.path("role").asText(null);
            long issuedAt = payload.path("iat").asLong(0L);
            long expiresAt = payload.path("exp").asLong(0L);
            if (username == null || role == null || issuedAt <= 0L || expiresAt <= 0L) {
                return Optional.empty();
            }
            if (Instant.now().getEpochSecond() >= expiresAt) {
                return Optional.empty();
            }

            return Optional.of(new JwtClaims(
                    username,
                    role,
                    Instant.ofEpochSecond(issuedAt),
                    Instant.ofEpochSecond(expiresAt)
            ));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to encode JWT payload", exception);
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(authProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return ENCODER.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign JWT", exception);
        }
    }
}
