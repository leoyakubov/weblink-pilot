package io.weblinkpilot.analytics.web;

import io.weblinkpilot.analytics.repository.ClickEventRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final ClickEventRepository repository;

    public AnalyticsController(ClickEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{code}/count")
    public long count(@PathVariable String code) {
        return repository.countByShortCode(code);
    }
}
