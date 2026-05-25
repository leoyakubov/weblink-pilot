package io.weblinkpilot.url.event;

import io.weblinkpilot.shared.contracts.LinkClickedEvent;
import io.weblinkpilot.shared.contracts.LinkCreatedEvent;

public interface LinkPublisher {
  void publish(LinkCreatedEvent event);

  void publish(LinkClickedEvent event);
}
