package io.weblinkpilot.analytics;

import io.weblinkpilot.analytics.domain.ClickEvent;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.analytics.repository.CountryClicksView;
import io.weblinkpilot.shared.contracts.LinkTrackingSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ClickEventRepositoryTest.TestConfig.class)
@Transactional
class ClickEventRepositoryTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestConfig {
    }

    @Autowired
    private ClickEventRepository repository;

    @Test
    void aggregatesCountsAndLatestEvent() {
        OffsetDateTime first = OffsetDateTime.of(2026, 5, 22, 12, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime second = first.plusMinutes(2);

        repository.saveAll(List.of(
                new ClickEvent("demo", first, LinkTrackingSource.REDIRECT, "203.0.113.10", "Mozilla/5.0", "https://github.com", "US", "Chrome", "Desktop"),
                new ClickEvent("demo", second, LinkTrackingSource.REDIRECT, "203.0.113.11", "Mozilla/5.0", null, "DE", "Safari", "Mobile"),
                new ClickEvent("demo", second.plusMinutes(1), LinkTrackingSource.QR_SCAN, "203.0.113.10", "Mozilla/5.0", null, "US", "Firefox", "Mobile")
        ));

        assertThat(repository.countByShortCode("demo")).isEqualTo(3L);
        assertThat(repository.countByShortCodeAndEventSource("demo", LinkTrackingSource.REDIRECT)).isEqualTo(2L);
        assertThat(repository.countByShortCodeAndEventSource("demo", LinkTrackingSource.QR_SCAN)).isEqualTo(1L);
        assertThat(repository.countDistinctIpAddressByShortCode("demo")).isEqualTo(2L);
        assertThat(repository.findFirstByShortCodeOrderByClickedAtDesc("demo")).isPresent();
        assertThat(repository.findTopCountriesByShortCode("demo"))
                .extracting(CountryClicksView::getCountry, CountryClicksView::getClicks)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("US", 2L),
                        org.assertj.core.groups.Tuple.tuple("DE", 1L)
                );
    }
}
