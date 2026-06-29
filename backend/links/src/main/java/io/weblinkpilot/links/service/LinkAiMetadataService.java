package io.weblinkpilot.links.service;

import io.weblinkpilot.shared.contracts.AiLinkMetadataResponse;
import java.util.Collection;
import java.util.Map;

public interface LinkAiMetadataService {

  default Map<String, AiLinkMetadataResponse> metadataByCodes(Collection<String> codes) {
    return Map.of();
  }
}
