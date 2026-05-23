package io.weblinkpilot.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.analytics.domain.ClickEvent;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.analytics.repository.CountryClicksView;
import io.weblinkpilot.shared.contracts.AnalyticsSummaryResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsQueryServiceTest {

    @Mock
    private ClickEventRepository repository;

    @InjectMocks
    private AnalyticsQueryService service;

    @Test
    void countsClicks() {
        when(repository.countByShortCode("demo")).thenReturn(5L);

        long count = service.countClicks("demo");

        assertThat(count).isEqualTo(5L);
        verify(repository).countByShortCode("demo");
    }

    @Test
    void summarizesLatestEventAndCountries() {
        OffsetDateTime clickedAt = OffsetDateTime.now(ZoneOffset.UTC);
        ClickEvent latest = new ClickEvent(
                "demo",
                clickedAt,
                "127.0.0.1",
                "Mozilla/5.0",
                "https://github.com",
                "US",
                "Chrome",
                "Desktop"
        );
        CountryClicksView country = new CountryClicksView() {
            @Override
            public String getCountry() {
                return "US";
            }

            @Override
            public long getClicks() {
                return 3L;
            }
        };

        when(repository.countByShortCode("demo")).thenReturn(9L);
        when(repository.countDistinctIpAddressByShortCode("demo")).thenReturn(4L);
        when(repository.findFirstByShortCodeOrderByClickedAtDesc("demo")).thenReturn(Optional.of(latest));
        when(repository.findTopCountriesByShortCode("demo")).thenReturn(List.of(country));

        AnalyticsSummaryResponse summary = service.summarize("demo");

        assertThat(summary.code()).isEqualTo("demo");
        assertThat(summary.totalClicks()).isEqualTo(9L);
        assertThat(summary.uniqueVisitors()).isEqualTo(4L);
        assertThat(summary.lastClickedAt()).isEqualTo(clickedAt);
        assertThat(summary.lastReferrer()).isEqualTo("https://github.com");
        assertThat(summary.lastBrowserFamily()).isEqualTo("Chrome");
        assertThat(summary.lastDeviceType()).isEqualTo("Desktop");
        assertThat(summary.topCountries()).hasSize(1);
        assertThat(summary.topCountries().getFirst().country()).isEqualTo("US");
    }

    @Test
    void summarizesEmptyData() {
        when(repository.countByShortCode("empty")).thenReturn(0L);
        when(repository.countDistinctIpAddressByShortCode("empty")).thenReturn(0L);
        when(repository.findFirstByShortCodeOrderByClickedAtDesc("empty")).thenReturn(Optional.empty());
        when(repository.findTopCountriesByShortCode("empty")).thenReturn(List.of());

        AnalyticsSummaryResponse summary = service.summarize("empty");

        assertThat(summary.totalClicks()).isZero();
        assertThat(summary.uniqueVisitors()).isZero();
        assertThat(summary.lastClickedAt()).isNull();
        assertThat(summary.topCountries()).isEmpty();
    }
}
