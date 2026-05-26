package io.weblinkpilot.url.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "short_links")
public class ShortLink {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "code", unique = true, nullable = false, length = 32)
  private String code;

  @Column(name = "original_url", nullable = false, length = 2048)
  private String originalUrl;

  @Column(name = "custom_alias", unique = true, length = 64)
  private String customAlias;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "expires_at")
  private OffsetDateTime expiresAt;

  @Column(name = "click_count", nullable = false)
  private long clickCount;

  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  @Column(name = "owner_username", length = 128)
  private String ownerUsername;

  protected ShortLink() {}

  public ShortLink(
      String code,
      String originalUrl,
      String customAlias,
      String ownerUsername,
      OffsetDateTime createdAt,
      OffsetDateTime expiresAt) {
    this.code = code;
    this.originalUrl = originalUrl;
    this.customAlias = customAlias;
    this.ownerUsername = ownerUsername;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
    this.clickCount = 0L;
    this.deletedAt = null;
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getOriginalUrl() {
    return originalUrl;
  }

  public String getCustomAlias() {
    return customAlias;
  }

  public void setCustomAlias(String customAlias) {
    this.customAlias = customAlias;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public long getClickCount() {
    return clickCount;
  }

  public OffsetDateTime getDeletedAt() {
    return deletedAt;
  }

  public String getOwnerUsername() {
    return ownerUsername;
  }

  public void setOwnerUsername(String ownerUsername) {
    this.ownerUsername = ownerUsername;
  }

  public void incrementClickCount() {
    this.clickCount++;
  }

  public void markDeleted(OffsetDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }

  public boolean isExpired(OffsetDateTime now) {
    return expiresAt != null && !expiresAt.isAfter(now);
  }
}
