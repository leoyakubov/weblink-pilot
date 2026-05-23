package io.weblinkpilot.analytics.web;

import io.weblinkpilot.analytics.service.AnalyticsQueryService;
import io.weblinkpilot.shared.contracts.AnalyticsSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsQueryService analyticsQueryService;

    public AnalyticsController(AnalyticsQueryService analyticsQueryService) {
        this.analyticsQueryService = analyticsQueryService;
    }

    @GetMapping("/{code}/count")
    public long count(@PathVariable("code") String code) {
        return analyticsQueryService.countClicks(code);
    }

    @GetMapping("/{code}")
    public AnalyticsSummaryResponse summary(@PathVariable("code") String code) {
        AnalyticsSummaryResponse response = analyticsQueryService.summarize(code);
        log.info("analytics.summary.code={} totalClicks={} uniqueVisitors={}", code, response.totalClicks(), response.uniqueVisitors());
        return response;
    }
}
