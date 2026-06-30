package io.weblinkpilot.links.event;

import io.weblinkpilot.shared.events.LinkClickedEvent;
import io.weblinkpilot.shared.events.LinkCreatedEvent;

public interface LinkPublisher {
  void publish(LinkCreatedEvent event);

  void publish(LinkClickedEvent event);
}
