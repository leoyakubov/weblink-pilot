package io.weblinkpilot.platform.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.weblinkpilot.shared.events.LinkClickedEvent;
import io.weblinkpilot.shared.events.LinkCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetricsListener {

  private final Counter linksCreatedCounter;
  private final Counter linksClickedCounter;

  public BusinessMetricsListener(MeterRegistry meterRegistry) {
    this.linksCreatedCounter =
        Counter.builder(BusinessMetric.LINKS_CREATED.meterName())
            .description(BusinessMetric.LINKS_CREATED.description())
            .register(meterRegistry);
    this.linksClickedCounter =
        Counter.builder(BusinessMetric.LINKS_CLICKED.meterName())
            .description(BusinessMetric.LINKS_CLICKED.description())
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
