package io.weblinkpilot.links.criteria;

import io.weblinkpilot.links.domain.ShortLink;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;

public enum ExpirationFilter {
  ACTIVE,
  EXPIRED,
  NEVER;

  public static ExpirationFilter from(String value) {
    if (value == null) {
      return null;
    }

    String normalized = value.trim();
    if (normalized.isEmpty() || "ALL".equalsIgnoreCase(normalized)) {
      return null;
    }

    try {
      return ExpirationFilter.valueOf(normalized.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  public boolean matches(ShortLink link, Clock clock) {
    OffsetDateTime expiresAt = link.getExpiresAt();
    OffsetDateTime now = OffsetDateTime.now(clock);
    return switch (this) {
      case ACTIVE -> expiresAt == null || expiresAt.isAfter(now);
      case EXPIRED -> expiresAt != null && !expiresAt.isAfter(now);
      case NEVER -> expiresAt == null;
    };
  }
}
