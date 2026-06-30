package io.weblinkpilot.analytics.service;

import io.weblinkpilot.analytics.domain.ClickEvent;
import io.weblinkpilot.analytics.event.AnalyticsCacheInvalidationRequestedEvent;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.analytics.useragent.UserAgentMetadata;
import io.weblinkpilot.analytics.useragent.UserAgentParser;
import io.weblinkpilot.shared.events.LinkClickedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClickEventRecorder {

  private static final Logger log = LoggerFactory.getLogger(ClickEventRecorder.class);

  private final ClickEventRepository repository;
  private final UserAgentParser userAgentParser;
  private final ApplicationEventPublisher eventPublisher;

  public ClickEventRecorder(
      ClickEventRepository repository,
      UserAgentParser userAgentParser,
      ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.userAgentParser = userAgentParser;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public void record(LinkClickedEvent event) {
    UserAgentMetadata metadata = userAgentParser.parse(event.userAgent());
    repository.save(
        new ClickEvent(
            event.code(),
            event.clickedAt(),
            event.source(),
            event.ipAddress(),
            event.userAgent(),
            event.referrer(),
            event.country(),
            metadata.browserFamilyValue(),
            metadata.deviceTypeValue()));
    eventPublisher.publishEvent(new AnalyticsCacheInvalidationRequestedEvent(event.code()));
    log.info(
        "analytics.click.persisted code={} source={} country={} browser={} device={} referrerPresent={}",
        event.code(),
        event.source(),
        event.country(),
        metadata.browserFamilyValue(),
        metadata.deviceTypeValue(),
        event.referrer() != null && !event.referrer().isBlank());
  }
}
