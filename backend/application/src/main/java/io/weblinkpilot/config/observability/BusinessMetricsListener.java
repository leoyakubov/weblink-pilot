package io.weblinkpilot.config.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.weblinkpilot.shared.contracts.LinkClickedEvent;
import io.weblinkpilot.shared.contracts.LinkCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetricsListener {

  private final Counter linksCreatedCounter;
  private final Counter linksClickedCounter;

  public BusinessMetricsListener(MeterRegistry meterRegistry) {
    this.linksCreatedCounter =
        Counter.builder("weblinkpilot.links.created.events")
            .description("Total number of short links created")
            .register(meterRegistry);
    this.linksClickedCounter =
        Counter.builder("weblinkpilot.links.clicked.events")
            .description("Total number of short link redirects")
            .register(meterRegistry);
  }

  @EventListener
  public void onLinkCreated(LinkCreatedEvent event) {
    linksCreatedCounter.increment();
  }

  @EventListener
  public void onLinkClicked(LinkClickedEvent event) {
    linksClickedCounter.increment();
  }
}
