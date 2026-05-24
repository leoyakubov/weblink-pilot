package io.weblinkpilot.analytics.service;

import io.weblinkpilot.analytics.domain.ClickEvent;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.shared.contracts.LinkClickedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ClickEventRecorder {

    private static final Logger log = LoggerFactory.getLogger(ClickEventRecorder.class);

    private final ClickEventRepository repository;
    private final UserAgentParser userAgentParser;
    private final AnalyticsCacheService analyticsCacheService;

    public ClickEventRecorder(ClickEventRepository repository, UserAgentParser userAgentParser, AnalyticsCacheService analyticsCacheService) {
        this.repository = repository;
        this.userAgentParser = userAgentParser;
        this.analyticsCacheService = analyticsCacheService;
    }

    @Transactional
    public void record(LinkClickedEvent event) {
        UserAgentMetadata metadata = userAgentParser.parse(event.userAgent());
        repository.save(new ClickEvent(
                event.code(),
                event.clickedAt(),
                event.source(),
                event.ipAddress(),
                event.userAgent(),
                event.referrer(),
                event.country(),
                metadata.browserFamily(),
                metadata.deviceType()
        ));
        evictAfterCommit(event.code());
        log.info(
                "analytics.click.persisted code={} source={} country={} browser={} device={} referrerPresent={}",
                event.code(),
                event.source(),
                event.country(),
                metadata.browserFamily(),
                metadata.deviceType(),
                event.referrer() != null && !event.referrer().isBlank()
        );
    }

    private void evictAfterCommit(String code) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            analyticsCacheService.evict(code);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                analyticsCacheService.evict(code);
            }
        });
    }
}
