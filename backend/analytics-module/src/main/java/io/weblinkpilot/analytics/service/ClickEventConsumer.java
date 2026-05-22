package io.weblinkpilot.analytics.service;

import io.weblinkpilot.shared.contracts.LinkClickedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ClickEventConsumer {

    private final AnalyticsService analyticsService;

    public ClickEventConsumer(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Async
    @EventListener
    public void handle(LinkClickedEvent event) {
        analyticsService.record(event);
    }
}
