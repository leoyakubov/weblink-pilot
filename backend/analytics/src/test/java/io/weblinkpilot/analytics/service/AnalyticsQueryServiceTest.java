package io.weblinkpilot.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.analytics.domain.ClickEvent;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.analytics.repository.CountryClicksView;
import io.weblinkpilot.shared.contracts.AnalyticsDetailsResponse;
import io.weblinkpilot.shared.contracts.AnalyticsSummaryResponse;
import io.weblinkpilot.shared.contracts.LinkTrackingSource;
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

  @Mock private ClickEventRepository repository;

  @InjectMocks private AnalyticsQueryService service;

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
    ClickEvent latest =
        new ClickEvent(
            "demo",
            clickedAt,
            LinkTrackingSource.REDIRECT,
            "127.0.0.1",
            "Mozilla/5.0",
            "https://github.com",
            "US",
            "Chrome",
            "Desktop");
    CountryClicksView country =
        new CountryClicksView() {
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
    when(repository.countByShortCodeAndEventSource("demo", LinkTrackingSource.REDIRECT))
        .thenReturn(6L);
    when(repository.countByShortCodeAndEventSource("demo", LinkTrackingSource.QR_SCAN))
        .thenReturn(3L);
    when(repository.countDistinctIpAddressByShortCode("demo")).thenReturn(4L);
    when(repository.findFirstByShortCodeOrderByClickedAtDesc("demo"))
        .thenReturn(Optional.of(latest));
    when(repository.findTopCountriesByShortCode("demo")).thenReturn(List.of(country));

    AnalyticsSummaryResponse summary = service.summarize("demo");

    assertThat(summary.code()).isEqualTo("demo");
    assertThat(summary.totalClicks()).isEqualTo(9L);
    assertThat(summary.redirectClicks()).isEqualTo(6L);
    assertThat(summary.qrScans()).isEqualTo(3L);
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
    when(repository.countByShortCodeAndEventSource("empty", LinkTrackingSource.REDIRECT))
        .thenReturn(0L);
    when(repository.countByShortCodeAndEventSource("empty", LinkTrackingSource.QR_SCAN))
        .thenReturn(0L);
    when(repository.countDistinctIpAddressByShortCode("empty")).thenReturn(0L);
    when(repository.findFirstByShortCodeOrderByClickedAtDesc("empty")).thenReturn(Optional.empty());
    when(repository.findTopCountriesByShortCode("empty")).thenReturn(List.of());

    AnalyticsSummaryResponse summary = service.summarize("empty");

    assertThat(summary.totalClicks()).isZero();
    assertThat(summary.redirectClicks()).isZero();
    assertThat(summary.qrScans()).isZero();
    assertThat(summary.uniqueVisitors()).isZero();
    assertThat(summary.lastClickedAt()).isNull();
    assertThat(summary.topCountries()).isEmpty();
  }

  @Test
  void buildsDetailedBreakdownsFromClickEvents() {
    OffsetDateTime first = OffsetDateTime.parse("2026-06-25T10:15:00Z");
    OffsetDateTime second = OffsetDateTime.parse("2026-06-25T11:20:00Z");
    when(repository.findByShortCodeOrderByClickedAtAsc("demo"))
        .thenReturn(
            List.of(
                new ClickEvent(
                    "demo",
                    first,
                    LinkTrackingSource.REDIRECT,
                    "127.0.0.1",
                    "Mozilla/5.0 Chrome",
                    "https://github.com/weblinkpilot",
                    "US",
                    "CHROME",
                    "DESKTOP"),
                new ClickEvent(
                    "demo",
                    second,
                    LinkTrackingSource.QR_SCAN,
                    "127.0.0.2",
                    "Mozilla/5.0 Safari",
                    null,
                    "ES",
                    "SAFARI",
                    "MOBILE")));

    AnalyticsDetailsResponse details = service.details("demo");

    assertThat(details.code()).isEqualTo("demo");
    assertThat(details.timelineByDay()).hasSize(1);
    assertThat(details.timelineByDay().getFirst().totalClicks()).isEqualTo(2L);
    assertThat(details.timelineByHour()).hasSize(2);
    assertThat(details.browserBreakdown()).extracting("label").contains("CHROME", "SAFARI");
    assertThat(details.deviceBreakdown()).extracting("label").contains("DESKTOP", "MOBILE");
    assertThat(details.referrerBreakdown())
        .extracting("label")
        .contains("github.com", "DIRECT / NONE");
    assertThat(details.recentEvents()).hasSize(2);
    assertThat(details.recentEvents().getFirst().eventSource())
        .isEqualTo(LinkTrackingSource.QR_SCAN);
  }
}
