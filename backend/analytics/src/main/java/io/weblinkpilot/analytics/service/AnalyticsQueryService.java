package io.weblinkpilot.analytics.service;

import io.weblinkpilot.analytics.domain.ClickEvent;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.analytics.repository.CountryClicksView;
import io.weblinkpilot.shared.contracts.AnalyticsCountryStatResponse;
import io.weblinkpilot.shared.contracts.AnalyticsSummaryResponse;
import io.weblinkpilot.shared.contracts.LinkTrackingSource;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsQueryService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsQueryService.class);

    private final ClickEventRepository repository;

    public AnalyticsQueryService(ClickEventRepository repository) {
        this.repository = repository;
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
        long redirectClicks = repository.countByShortCodeAndEventSource(code, LinkTrackingSource.REDIRECT);
        long qrScans = repository.countByShortCodeAndEventSource(code, LinkTrackingSource.QR_SCAN);
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
                redirectClicks,
                qrScans,
                uniqueVisitors,
                latest == null ? null : latest.getClickedAt(),
                latest == null ? null : latest.getReferrer(),
                latest == null ? null : latest.getBrowserFamily(),
                latest == null ? null : latest.getDeviceType(),
                topCountries
        );

        log.info(
                "analytics.summary.code={} totalClicks={} redirectClicks={} qrScans={} uniqueVisitors={} topCountries={}",
                code,
                totalClicks,
                redirectClicks,
                qrScans,
                uniqueVisitors,
                topCountries.size()
        );
        return summary;
    }
}
