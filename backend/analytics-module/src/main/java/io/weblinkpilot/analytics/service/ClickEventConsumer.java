package io.weblinkpilot.analytics.service;

import io.weblinkpilot.shared.contracts.LinkClickedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ClickEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ClickEventConsumer.class);

    private final AnalyticsService analyticsService;

    public ClickEventConsumer(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Async
    @EventListener
    public void handle(LinkClickedEvent event) {
        log.info("analytics.click.event.received code={} clickedAt={}", event.code(), event.clickedAt());
        analyticsService.record(event);
    }
}
