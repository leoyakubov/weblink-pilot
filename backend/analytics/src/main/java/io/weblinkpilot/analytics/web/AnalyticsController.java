package io.weblinkpilot.analytics.web;

import io.weblinkpilot.analytics.service.AnalyticsQueryService;
import io.weblinkpilot.shared.contracts.AnalyticsSummaryResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "basicAuth")
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsQueryService analyticsQueryService;
    private final Counter countCounter;
    private final Counter summaryCounter;

    public AnalyticsController(AnalyticsQueryService analyticsQueryService, MeterRegistry meterRegistry) {
        this.analyticsQueryService = analyticsQueryService;
        this.countCounter = Counter.builder("weblinkpilot.analytics.count.requests")
                .description("Number of analytics count requests")
                .register(meterRegistry);
        this.summaryCounter = Counter.builder("weblinkpilot.analytics.summary.requests")
                .description("Number of analytics summary requests")
                .register(meterRegistry);
    }

    @GetMapping("/{code}/count")
    public long count(@PathVariable("code") String code) {
        countCounter.increment();
        return analyticsQueryService.countClicks(code);
    }

    @GetMapping("/{code}")
    public AnalyticsSummaryResponse summary(@PathVariable("code") String code) {
        summaryCounter.increment();
        AnalyticsSummaryResponse response = analyticsQueryService.summarize(code);
        log.info("analytics.summary.code={} totalClicks={} uniqueVisitors={}", code, response.totalClicks(), response.uniqueVisitors());
        return response;
    }
}
