package io.weblinkpilot.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "ai_link_metadata")
public class AiLinkMetadata {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "short_code", nullable = false, unique = true, length = 64)
  private String shortCode;

  @Column(name = "original_url", nullable = false, length = 2048)
  private String originalUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 24)
  private AiLinkMetadataStatus status;

  @Column(name = "provider", nullable = false, length = 64)
  private String provider;

  @Column(name = "prompt_version", nullable = false, length = 64)
  private String promptVersion;

  @Column(name = "title")
  private String title;

  @Column(name = "summary", length = 1000)
  private String summary;

  @Column(name = "category", length = 120)
  private String category;

  @Column(name = "icon", length = 16)
  private String icon;

  @Column(name = "suggested_alias", length = 120)
  private String suggestedAlias;

  @Column(name = "tags", length = 500)
  private String tags;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;

  protected AiLinkMetadata() {}

  public AiLinkMetadata(
      String shortCode,
      String originalUrl,
      AiLinkMetadataStatus status,
      String provider,
      String promptVersion,
      OffsetDateTime now) {
    this.shortCode = shortCode;
    this.originalUrl = originalUrl;
    this.status = status;
    this.provider = provider;
    this.promptVersion = promptVersion;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void markReady(AiLinkMetadataResult result, OffsetDateTime now) {
    this.status = AiLinkMetadataStatus.READY;
    this.title = result.title();
    this.summary = result.summary();
    this.category = result.category();
    this.icon = result.icon();
    this.suggestedAlias = result.suggestedAlias();
    this.tags = String.join(",", result.tags());
    this.errorMessage = null;
    this.updatedAt = now;
    this.completedAt = now;
  }

  public void markPending(String provider, String promptVersion, OffsetDateTime now) {
    this.status = AiLinkMetadataStatus.PENDING;
    this.provider = provider;
    this.promptVersion = promptVersion;
    this.errorMessage = null;
    this.updatedAt = now;
    this.completedAt = null;
  }

  public void markFailed(String message, OffsetDateTime now) {
    this.status = AiLinkMetadataStatus.FAILED;
    this.errorMessage = message;
    this.updatedAt = now;
    this.completedAt = now;
  }

  public List<String> tagList() {
    if (tags == null || tags.isBlank()) {
      return List.of();
    }
    return Arrays.stream(tags.split(",")).map(String::trim).filter(tag -> !tag.isBlank()).toList();
  }

  public String getShortCode() {
    return shortCode;
  }

  public String getOriginalUrl() {
    return originalUrl;
  }

  public AiLinkMetadataStatus getStatus() {
    return status;
  }

  public String getProvider() {
    return provider;
  }

  public String getPromptVersion() {
    return promptVersion;
  }

  public String getTitle() {
    return title;
  }

  public String getSummary() {
    return summary;
  }

  public String getCategory() {
    return category;
  }

  public String getIcon() {
    return icon;
  }

  public String getSuggestedAlias() {
    return suggestedAlias;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public OffsetDateTime getCompletedAt() {
    return completedAt;
  }
}
