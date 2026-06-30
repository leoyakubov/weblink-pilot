package io.weblinkpilot.shared.api.ai;

import java.time.OffsetDateTime;
import java.util.List;

public record AiLinkMetadataResponse(
    String code,
    String status,
    String provider,
    String promptVersion,
    String title,
    String summary,
    String category,
    List<String> tags,
    String icon,
    String suggestedAlias,
    String errorMessage,
    OffsetDateTime updatedAt,
    OffsetDateTime completedAt) {

  public AiLinkMetadataResponse {
    tags = tags == null ? List.of() : List.copyOf(tags);
  }
}
