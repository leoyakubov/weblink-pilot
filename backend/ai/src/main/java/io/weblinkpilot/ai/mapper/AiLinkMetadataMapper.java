package io.weblinkpilot.ai.mapper;

import io.weblinkpilot.ai.domain.AiLinkMetadata;
import io.weblinkpilot.shared.api.ai.AiLinkMetadataResponse;
import org.springframework.stereotype.Component;

@Component
public class AiLinkMetadataMapper {

  public AiLinkMetadataResponse toResponse(AiLinkMetadata metadata) {
    return new AiLinkMetadataResponse(
        metadata.getShortCode(),
        metadata.getStatus().name(),
        metadata.getProvider(),
        metadata.getPromptVersion(),
        metadata.getTitle(),
        metadata.getSummary(),
        metadata.getCategory(),
        metadata.tagList(),
        metadata.getIcon(),
        metadata.getSuggestedAlias(),
        metadata.getErrorMessage(),
        metadata.getUpdatedAt(),
        metadata.getCompletedAt());
  }
}
