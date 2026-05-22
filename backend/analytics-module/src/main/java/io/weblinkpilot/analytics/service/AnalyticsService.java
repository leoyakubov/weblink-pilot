package io.weblinkpilot.analytics.service;

import io.weblinkpilot.analytics.domain.ClickEvent;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.shared.contracts.LinkClickedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private final ClickEventRepository repository;
    private final UserAgentParser userAgentParser;

    public AnalyticsService(ClickEventRepository repository, UserAgentParser userAgentParser) {
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
    }
}
