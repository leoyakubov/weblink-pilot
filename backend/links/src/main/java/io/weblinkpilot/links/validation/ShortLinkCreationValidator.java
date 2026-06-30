package io.weblinkpilot.links.validation;

import io.weblinkpilot.links.config.ShortLinkProperties;
import io.weblinkpilot.shared.api.links.CreateLinkRequest;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ShortLinkCreationValidator {

  public record ValidatedCreation(
      String originalUrl, String customAlias, String ownerUsername, OffsetDateTime expiresAt) {}

  private static final Logger log = LoggerFactory.getLogger(ShortLinkCreationValidator.class);

  private final Duration maxExpiration;
  private final int aliasMinLength;
  private final int aliasMaxLength;
  private final Pattern aliasAllowedPattern;

  public ShortLinkCreationValidator(ShortLinkProperties shortLinkProperties) {
    this.maxExpiration = shortLinkProperties.getMaxExpiration();
    this.aliasMinLength = shortLinkProperties.getAlias().getMinLength();
    this.aliasMaxLength = shortLinkProperties.getAlias().getMaxLength();
    this.aliasAllowedPattern = Pattern.compile(shortLinkProperties.getAlias().getAllowedPattern());
  }

  public ValidatedCreation validate(
      CreateLinkRequest request, String ownerUsername, OffsetDateTime now) {
    if (request.expiresAt() != null) {
      validateExpirationWindow(request.expiresAt(), now);
    }
    return new ValidatedCreation(
        normalizeUrl(request.originalUrl()),
        normalizeAlias(request.customAlias()),
        normalizeOwnerUsername(ownerUsername),
        request.expiresAt());
  }

  private void validateExpirationWindow(OffsetDateTime expiresAt, OffsetDateTime now) {
    if (!expiresAt.isAfter(now)) {
      log.warn("url.link.create.rejected reason=expired_request expiresAt={}", expiresAt);
      throw new IllegalArgumentException("Expiration time must be in the future");
    }

    if (maxExpiration == null || maxExpiration.isNegative() || maxExpiration.isZero()) {
      return;
    }

    OffsetDateTime maxExpiresAt = now.plus(maxExpiration);
    if (expiresAt.isAfter(maxExpiresAt)) {
      log.warn(
          "url.link.create.rejected reason=expiration_too_far expiresAt={} maxExpiresAt={}",
          expiresAt,
          maxExpiresAt);
      throw new IllegalArgumentException("Expiration time exceeds the configured maximum lifetime");
    }
  }

  private String normalizeAlias(String customAlias) {
    if (customAlias == null || customAlias.isBlank()) {
      return null;
    }
    String alias = customAlias.trim();
    if (alias.length() < aliasMinLength
        || alias.length() > aliasMaxLength
        || !aliasAllowedPattern.matcher(alias).matches()) {
      throw new IllegalArgumentException(
          "Custom alias must be "
              + aliasMinLength
              + "-"
              + aliasMaxLength
              + " characters long and contain only letters, digits, dash or underscore");
    }
    return alias;
  }

  private String normalizeUrl(String rawUrl) {
    String value = rawUrl == null ? null : rawUrl.trim();
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Original URL is required");
    }
    URI uri = URI.create(value);
    if (uri.getScheme() == null || uri.getHost() == null) {
      throw new IllegalArgumentException(
          "Original URL must be absolute and include scheme and host");
    }
    return uri.toString();
  }

  private String normalizeOwnerUsername(String ownerUsername) {
    if (ownerUsername == null || ownerUsername.isBlank()) {
      return null;
    }
    return ownerUsername.trim().toLowerCase(Locale.ROOT);
  }
}
