package io.weblinkpilot.shared.ports;

import io.weblinkpilot.shared.api.ai.AiLinkMetadataResponse;
import java.util.Collection;
import java.util.Map;

public interface LinkAiMetadataService {

  default Map<String, AiLinkMetadataResponse> metadataByCodes(Collection<String> codes) {
    return Map.of();
  }
}
