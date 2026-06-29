package io.weblinkpilot.shared.contracts;

import java.time.OffsetDateTime;

public record LinkResponse(
    String code,
    String shortUrl,
    String qrCodeUrl,
    String originalUrl,
    OffsetDateTime createdAt,
    OffsetDateTime expiresAt,
    long clickCount,
    String ownerUsername,
    String ownerRole,
    AiLinkMetadataResponse aiMetadata) {

  public LinkResponse(
      String code,
      String shortUrl,
      String qrCodeUrl,
      String originalUrl,
      OffsetDateTime createdAt,
      OffsetDateTime expiresAt,
      long clickCount,
      String ownerUsername,
      String ownerRole) {
    this(
        code,
        shortUrl,
        qrCodeUrl,
        originalUrl,
        createdAt,
        expiresAt,
        clickCount,
        ownerUsername,
        ownerRole,
        null);
  }

  public LinkResponse(
      String code,
      String shortUrl,
      String qrCodeUrl,
      String originalUrl,
      OffsetDateTime createdAt,
      OffsetDateTime expiresAt,
      long clickCount,
      String ownerUsername) {
    this(
        code,
        shortUrl,
        qrCodeUrl,
        originalUrl,
        createdAt,
        expiresAt,
        clickCount,
        ownerUsername,
        null);
  }
}
