package io.weblinkpilot.analytics.service;

import static io.weblinkpilot.shared.contracts.LinkTrackingSource.QR_SCAN;
import static io.weblinkpilot.shared.contracts.LinkTrackingSource.REDIRECT;

import io.weblinkpilot.analytics.domain.ClickEvent;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.shared.contracts.LinkTrackingSource;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsBootstrapService {

  private static final Logger log = LoggerFactory.getLogger(AnalyticsBootstrapService.class);

  private final ClickEventRepository clickEventRepository;

  public AnalyticsBootstrapService(ClickEventRepository clickEventRepository) {
    this.clickEventRepository = clickEventRepository;
  }

  @Transactional
  public void seedDefaultAnalytics() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);
    seedEvents(
        "spring-boot",
        now,
        List.of(
            event(5, 4, REDIRECT, "203.0.113.10", "US", "Chrome", "Desktop", "https://github.com"),
            event(
                5,
                2,
                REDIRECT,
                "203.0.113.11",
                "US",
                "Safari",
                "Mobile",
                "https://news.ycombinator.com"),
            event(4, 7, QR_SCAN, "203.0.113.12", "DE", "Chrome", "Mobile", null),
            event(
                4, 3, REDIRECT, "203.0.113.13", "ES", "Firefox", "Desktop", "https://spring.io"),
            event(3, 8, REDIRECT, "203.0.113.10", "US", "Chrome", "Desktop", "https://github.com"),
            event(3, 1, QR_SCAN, "203.0.113.14", "PL", "Safari", "Tablet", null),
            event(2, 6, REDIRECT, "203.0.113.15", "FR", "Edge", "Desktop", "https://google.com"),
            event(2, 2, REDIRECT, "203.0.113.16", "US", "Chrome", "Desktop", "https://github.com"),
            event(1, 9, QR_SCAN, "203.0.113.17", "CA", "Chrome", "Mobile", null),
            event(
                1, 5, REDIRECT, "203.0.113.18", "DE", "Firefox", "Desktop", "https://spring.io"),
            event(0, 6, REDIRECT, "203.0.113.19", "US", "Chrome", "Desktop", "https://google.com"),
            event(0, 1, QR_SCAN, "203.0.113.20", "ES", "Safari", "Mobile", null)));

    seedEvents(
        "vue-js",
        now,
        List.of(
            event(6, 3, REDIRECT, "198.51.100.10", "US", "Chrome", "Desktop", "https://github.com"),
            event(5, 7, QR_SCAN, "198.51.100.11", "NL", "Safari", "Mobile", null),
            event(
                4, 8, REDIRECT, "198.51.100.12", "UA", "Firefox", "Desktop", "https://vuejs.org"),
            event(3, 2, REDIRECT, "198.51.100.13", "ES", "Chrome", "Mobile", "https://google.com"),
            event(2, 5, QR_SCAN, "198.51.100.14", "US", "Chrome", "Mobile", null),
            event(1, 8, REDIRECT, "198.51.100.10", "US", "Chrome", "Desktop", "https://github.com"),
            event(0, 4, REDIRECT, "198.51.100.15", "DE", "Edge", "Desktop", "https://google.com"),
            event(0, 1, QR_SCAN, "198.51.100.16", "FR", "Safari", "Tablet", null)));

    seedEvents(
        "postgres",
        now,
        List.of(
            event(7, 3, REDIRECT, "192.0.2.10", "US", "Chrome", "Desktop", "https://github.com"),
            event(
                6,
                6,
                REDIRECT,
                "192.0.2.11",
                "DE",
                "Firefox",
                "Desktop",
                "https://postgresql.org"),
            event(5, 2, QR_SCAN, "192.0.2.12", "GB", "Safari", "Mobile", null),
            event(4, 7, REDIRECT, "192.0.2.13", "US", "Chrome", "Desktop", "https://google.com"),
            event(3, 5, REDIRECT, "192.0.2.14", "PL", "Edge", "Desktop", "https://github.com"),
            event(2, 9, QR_SCAN, "192.0.2.15", "ES", "Chrome", "Mobile", null),
            event(
                1,
                3,
                REDIRECT,
                "192.0.2.16",
                "FR",
                "Safari",
                "Tablet",
                "https://postgresql.org"),
            event(0, 2, REDIRECT, "192.0.2.10", "US", "Chrome", "Desktop", "https://github.com")));

    seedEvents(
        "redis",
        now,
        List.of(
            event(4, 4, REDIRECT, "203.0.113.40", "US", "Chrome", "Desktop", "https://github.com"),
            event(3, 6, QR_SCAN, "203.0.113.41", "ES", "Safari", "Mobile", null),
            event(
                2, 5, REDIRECT, "203.0.113.42", "DE", "Firefox", "Desktop", "https://redis.io"),
            event(1, 8, REDIRECT, "203.0.113.43", "US", "Chrome", "Mobile", "https://google.com"),
            event(0, 6, QR_SCAN, "203.0.113.44", "NL", "Chrome", "Mobile", null),
            event(0, 2, REDIRECT, "203.0.113.40", "US", "Chrome", "Desktop", "https://github.com")));
  }

  private void seedEvents(String code, OffsetDateTime now, List<SeedClick> clicks) {
    if (clickEventRepository.countByShortCode(code) > 0) {
      return;
    }

    List<ClickEvent> events = new ArrayList<>();
    for (SeedClick click : clicks) {
      events.add(
          new ClickEvent(
              code,
              now.minusDays(click.daysAgo()).minusHours(click.hoursAgo()),
              click.source(),
              click.ipAddress(),
              userAgentFor(click.browserFamily(), click.deviceType()),
              click.referrer(),
              click.country(),
              click.browserFamily(),
              click.deviceType()));
    }

    clickEventRepository.saveAll(events);
    log.info("bootstrap.analytics.seeded code={} events={}", code, events.size());
  }

  private static SeedClick event(
      int daysAgo,
      int hoursAgo,
      LinkTrackingSource source,
      String ipAddress,
      String country,
      String browserFamily,
      String deviceType,
      String referrer) {
    return new SeedClick(
        daysAgo, hoursAgo, source, ipAddress, country, browserFamily, deviceType, referrer);
  }

  private String userAgentFor(String browserFamily, String deviceType) {
    String platform =
        switch (deviceType) {
          case "Mobile" -> "iPhone; CPU iPhone OS 17_5 like Mac OS X";
          case "Tablet" -> "iPad; CPU OS 17_5 like Mac OS X";
          default -> "Macintosh; Intel Mac OS X 14_5";
        };
    return "Mozilla/5.0 ("
        + platform
        + ") AppleWebKit/537.36 (KHTML, like Gecko) "
        + browserFamily
        + "/125.0 Safari/537.36";
  }

  private record SeedClick(
      int daysAgo,
      int hoursAgo,
      LinkTrackingSource source,
      String ipAddress,
      String country,
      String browserFamily,
      String deviceType,
      String referrer) {}
}
