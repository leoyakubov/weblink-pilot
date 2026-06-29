package io.weblinkpilot.ai.domain;

import java.util.List;

public record AiLinkMetadataResult(
    String title,
    String summary,
    String category,
    List<String> tags,
    String icon,
    String suggestedAlias) {

  public AiLinkMetadataResult {
    tags = tags == null ? List.of() : List.copyOf(tags);
  }
}
