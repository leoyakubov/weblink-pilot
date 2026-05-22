package io.weblinkpilot.analytics.web;

import io.weblinkpilot.analytics.repository.ClickEventRepository;
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

    private final ClickEventRepository repository;

    public AnalyticsController(ClickEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{code}/count")
    public long count(@PathVariable("code") String code) {
        long count = repository.countByShortCode(code);
        log.info("analytics.count.code={} count={}", code, count);
        return count;
    }
}
