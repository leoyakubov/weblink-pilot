package io.weblinkpilot.analytics.web;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.weblinkpilot.analytics.service.AnalyticsQueryService;
import io.weblinkpilot.shared.api.analytics.AnalyticsDetailsResponse;
import io.weblinkpilot.shared.api.analytics.AnalyticsSummaryResponse;
import io.weblinkpilot.shared.ports.LinkOwnershipLookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

  private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

  private final AnalyticsQueryService analyticsQueryService;
  private final LinkOwnershipLookupService linkOwnershipLookupService;
  private final Counter countCounter;
  private final Counter summaryCounter;

  public AnalyticsController(
      AnalyticsQueryService analyticsQueryService,
      LinkOwnershipLookupService linkOwnershipLookupService,
      MeterRegistry meterRegistry) {
    this.analyticsQueryService = analyticsQueryService;
    this.linkOwnershipLookupService = linkOwnershipLookupService;
    this.countCounter =
        Counter.builder("weblinkpilot.analytics.count.requests")
            .description("Number of analytics count requests")
            .register(meterRegistry);
    this.summaryCounter =
        Counter.builder("weblinkpilot.analytics.summary.requests")
            .description("Number of analytics summary requests")
            .register(meterRegistry);
  }

  @GetMapping("/{code}/count")
  public long count(Authentication authentication, @PathVariable("code") String code) {
    assertCanReadAnalytics(authentication, code);
    countCounter.increment();
    return analyticsQueryService.countClicks(code);
  }

  @GetMapping("/{code}")
  public AnalyticsSummaryResponse summary(
      Authentication authentication, @PathVariable("code") String code) {
    assertCanReadAnalytics(authentication, code);
    summaryCounter.increment();
    AnalyticsSummaryResponse response = analyticsQueryService.summarize(code);
    log.info(
        "analytics.api.summary code={} totalClicks={} uniqueVisitors={}",
        code,
        response.totalClicks(),
        response.uniqueVisitors());
    return response;
  }

  @GetMapping("/{code}/details")
  public AnalyticsDetailsResponse details(
      Authentication authentication, @PathVariable("code") String code) {
    assertCanReadAnalytics(authentication, code);
    summaryCounter.increment();
    AnalyticsDetailsResponse response = analyticsQueryService.details(code);
    log.info(
        "analytics.api.details code={} timelineDays={} recentEvents={}",
        code,
        response.timelineByDay().size(),
        response.recentEvents().size());
    return response;
  }

  private void assertCanReadAnalytics(Authentication authentication, String code) {
    String ownerUsername = linkOwnershipLookupService.ownerUsernameForCode(code);
    if (ownerUsername == null || ownerUsername.isBlank()) {
      return;
    }

    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Sign in to view analytics for this link");
    }

    boolean isAdmin =
        authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    if (isAdmin || ownerUsername.equalsIgnoreCase(authentication.getName())) {
      return;
    }

    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN, "You can only view analytics for your own links");
  }
}
