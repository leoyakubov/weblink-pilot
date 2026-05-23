package io.weblinkpilot.analytics.service;

import io.weblinkpilot.analytics.domain.ClickEvent;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.analytics.repository.CountryClicksView;
import io.weblinkpilot.shared.contracts.AnalyticsCountryStatResponse;
import io.weblinkpilot.shared.contracts.AnalyticsSummaryResponse;
import io.weblinkpilot.shared.contracts.LinkClickedEvent;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

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
        log.info(
                "analytics.click.persisted code={} browser={} device={} referrerPresent={}",
                event.code(),
                metadata.browserFamily(),
                metadata.deviceType(),
                event.referrer() != null && !event.referrer().isBlank()
        );
    }

    @Transactional(readOnly = true)
    public long countClicks(String code) {
        long count = repository.countByShortCode(code);
        log.info("analytics.count.code={} count={}", code, count);
        return count;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse summarize(String code) {
        long totalClicks = repository.countByShortCode(code);
        long uniqueVisitors = repository.countDistinctIpAddressByShortCode(code);
        Optional<ClickEvent> latestEvent = repository.findFirstByShortCodeOrderByClickedAtDesc(code);
        List<AnalyticsCountryStatResponse> topCountries = repository.findTopCountriesByShortCode(code).stream()
                .limit(5)
                .map(view -> new AnalyticsCountryStatResponse(view.getCountry(), view.getClicks()))
                .toList();

        ClickEvent latest = latestEvent.orElse(null);
        AnalyticsSummaryResponse summary = new AnalyticsSummaryResponse(
                code,
                totalClicks,
                uniqueVisitors,
                latest == null ? null : latest.getClickedAt(),
                latest == null ? null : latest.getReferrer(),
                latest == null ? null : latest.getBrowserFamily(),
                latest == null ? null : latest.getDeviceType(),
                topCountries
        );

        log.info(
                "analytics.summary.code={} totalClicks={} uniqueVisitors={} topCountries={}",
                code,
                totalClicks,
                uniqueVisitors,
                topCountries.size()
        );
        return summary;
    }
}
