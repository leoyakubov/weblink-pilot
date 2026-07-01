package io.weblinkpilot.links.criteria;

import java.util.Locale;

public record LinkSearchCriteria(
    String ownerUsername,
    boolean admin,
    String creator,
    String ownerRole,
    ExpirationFilter expiration,
    int page,
    int size) {

  private static final int DEFAULT_LIMIT = 10;
  private static final int MAX_LIMIT = 50;
  private static final String ALL_FILTER = "ALL";
  private static final String ANONYMOUS_CREATOR = "anonymous";
  private static final String GUEST_CREATOR = "guest";

  public LinkSearchCriteria {
    ownerUsername = normalizeLower(ownerUsername);
    creator = normalizeCreator(creator);
    ownerRole = normalizeRole(ownerRole);
  }

  public static LinkSearchCriteria guest(String expiration, int limit) {
    return guest(expiration, 0, limit);
  }

  public static LinkSearchCriteria guest(String expiration, int page, int size) {
    return new LinkSearchCriteria(
        null, false, null, null, ExpirationFilter.from(expiration), page, size);
  }

  public static LinkSearchCriteria user(
      String ownerUsername,
      boolean admin,
      String creator,
      String ownerRole,
      String expiration,
      int limit) {
    return new LinkSearchCriteria(
        ownerUsername, admin, creator, ownerRole, ExpirationFilter.from(expiration), 0, limit);
  }

  public static LinkSearchCriteria user(
      String ownerUsername,
      boolean admin,
      String creator,
      String ownerRole,
      String expiration,
      int page,
      int size) {
    return new LinkSearchCriteria(
        ownerUsername, admin, creator, ownerRole, ExpirationFilter.from(expiration), page, size);
  }

  public int pageNumber() {
    return Math.max(0, page);
  }

  public int pageSize(int defaultLimit, int maxLimit) {
    return clampSize(size, defaultLimit, maxLimit);
  }

  public int pageSize() {
    return pageSize(DEFAULT_LIMIT, MAX_LIMIT);
  }

  public boolean creatorIsAnonymous() {
    return ANONYMOUS_CREATOR.equalsIgnoreCase(creator) || GUEST_CREATOR.equalsIgnoreCase(creator);
  }

  private static int clampSize(int value, int defaultLimit, int maxLimit) {
    int normalizedDefault = Math.max(1, defaultLimit);
    int normalizedMax = Math.max(normalizedDefault, maxLimit);
    return Math.max(1, Math.min(value <= 0 ? normalizedDefault : value, normalizedMax));
  }

  private static String normalizeCreator(String value) {
    return normalizeLower(value);
  }

  private static String normalizeRole(String value) {
    String normalized = normalize(value);
    if (normalized == null || ALL_FILTER.equalsIgnoreCase(normalized)) {
      return null;
    }
    return normalized.toUpperCase(Locale.ROOT);
  }

  private static String normalizeLower(String value) {
    String normalized = normalize(value);
    return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
