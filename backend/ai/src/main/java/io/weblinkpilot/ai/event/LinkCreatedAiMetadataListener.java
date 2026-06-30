package io.weblinkpilot.ai.event;

import io.weblinkpilot.ai.service.AiLinkMetadataService;
import io.weblinkpilot.shared.events.LinkCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class LinkCreatedAiMetadataListener {

  private final AiLinkMetadataService metadataService;

  public LinkCreatedAiMetadataListener(AiLinkMetadataService metadataService) {
    this.metadataService = metadataService;
  }

  @Async
  @EventListener
  public void onLinkCreated(LinkCreatedEvent event) {
    metadataService.enrich(event);
  }
}
