package io.weblinkpilot.ai.provider;

import io.weblinkpilot.ai.domain.AiLinkMetadataResult;

public interface AiProvider {

  String name();

  AiLinkMetadataResult generateLinkMetadata(AiLinkMetadataPrompt prompt);
}
