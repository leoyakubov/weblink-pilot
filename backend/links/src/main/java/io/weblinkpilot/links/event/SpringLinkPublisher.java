package io.weblinkpilot.links.event;

import io.weblinkpilot.shared.events.LinkClickedEvent;
import io.weblinkpilot.shared.events.LinkCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringLinkPublisher implements LinkPublisher {

  private final ApplicationEventPublisher publisher;

  public SpringLinkPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  public void publish(LinkCreatedEvent event) {
    publisher.publishEvent(event);
  }

  @Override
  public void publish(LinkClickedEvent event) {
    publisher.publishEvent(event);
  }
}
