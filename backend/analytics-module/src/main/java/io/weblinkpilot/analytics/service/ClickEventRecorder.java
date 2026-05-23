package io.weblinkpilot.analytics.service;

import io.weblinkpilot.analytics.domain.ClickEvent;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.shared.contracts.LinkClickedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClickEventRecorder {

    private static final Logger log = LoggerFactory.getLogger(ClickEventRecorder.class);

    private final ClickEventRepository repository;
    private final UserAgentParser userAgentParser;

    public ClickEventRecorder(ClickEventRepository repository, UserAgentParser userAgentParser) {
        this.repository = repository;
        this.userAgentParser = userAgentParser;
    }

    @Transactional
    public void record(LinkClickedEvent event) {
        UserAgentMetadata metadata = userAgentParser.parse(event.userAgent());
        repository.save(new ClickEvent(
                event.code(),
                event.clickedAt(),
                event.ipAddress(),
                event.userAgent(),
                event.referrer(),
                "UNKNOWN",
                metadata.browserFamily(),
                metadata.deviceType()
        ));
        log.info(
                "analytics.click.persisted code={} browser={} device={} referrerPresent={}",
                event.code(),
                metadata.browserFamily(),
                metadata.deviceType(),
                event.referrer() != null && !event.referrer().isBlank()
        );
    }
}
