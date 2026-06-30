package io.weblinkpilot.analytics.bootstrap;

import io.weblinkpilot.analytics.domain.ClickEvent;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.analytics.useragent.BrowserFamily;
import io.weblinkpilot.analytics.useragent.DeviceType;
import io.weblinkpilot.shared.seed.DemoSeedDataCatalog;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
  public Map<String, Long> seedDefaultAnalytics() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);
    Map<String, Long> counts = new LinkedHashMap<>();
    DemoSeedDataCatalog.analyticsEventsByCode()
        .forEach((code, clicks) -> counts.put(code, seedEvents(code, now, clicks)));
    return counts;
  }

  private long seedEvents(
      String code, OffsetDateTime now, List<DemoSeedDataCatalog.DemoAnalyticsEvent> clicks) {
    if (clickEventRepository.countByShortCode(code) > 0) {
      return clickEventRepository.countByShortCode(code);
    }

    List<ClickEvent> events = new ArrayList<>();
    for (DemoSeedDataCatalog.DemoAnalyticsEvent click : clicks) {
      events.add(
          new ClickEvent(
              code,
              now.minusDays(click.daysAgo()).minusHours(click.hoursAgo()),
              click.source(),
              click.ipAddress(),
              DemoUserAgent.forBrowserAndDevice(click.browserFamily(), click.deviceType()),
              click.referrer(),
              click.country(),
              BrowserFamily.fromSeedValue(click.browserFamily()).value(),
              DeviceType.fromSeedValue(click.deviceType()).value()));
    }

    clickEventRepository.saveAll(events);
    log.info("bootstrap.analytics.seeded code={} events={}", code, events.size());
    return events.size();
  }
}
