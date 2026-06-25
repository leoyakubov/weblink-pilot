package io.weblinkpilot.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.weblinkpilot.analytics.domain.ClickEvent;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.shared.contracts.LinkTrackingSource;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = AnalyticsBootstrapServiceTest.TestConfig.class)
@Transactional
class AnalyticsBootstrapServiceTest {

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @EntityScan("io.weblinkpilot.analytics.domain")
  @EnableJpaRepositories("io.weblinkpilot.analytics.repository")
  @Import(AnalyticsBootstrapService.class)
  static class TestConfig {}

  @Autowired private AnalyticsBootstrapService service;

  @Autowired private ClickEventRepository repository;

  @Test
  void seedsAnalyticsForDefaultLinks() {
    service.seedDefaultAnalytics();

    assertThat(repository.countByShortCode("spring-boot")).isEqualTo(12L);
    assertThat(repository.countByShortCode("vue-js")).isEqualTo(8L);
    assertThat(repository.countByShortCode("postgres")).isEqualTo(8L);
    assertThat(repository.countByShortCode("redis")).isEqualTo(6L);

    List<ClickEvent> redisEvents = repository.findByShortCodeOrderByClickedAtAsc("redis");
    assertThat(redisEvents)
        .extracting(ClickEvent::getEventSource)
        .contains(LinkTrackingSource.REDIRECT, LinkTrackingSource.QR_SCAN);
    assertThat(redisEvents).extracting(ClickEvent::getCountry).contains("US", "ES", "DE", "NL");
    assertThat(redisEvents)
        .extracting(ClickEvent::getBrowserFamily)
        .contains("Chrome", "Safari", "Firefox");
  }

  @Test
  void doesNotDuplicateExistingSeededAnalytics() {
    service.seedDefaultAnalytics();
    service.seedDefaultAnalytics();

    assertThat(repository.countByShortCode("spring-boot")).isEqualTo(12L);
    assertThat(repository.countByShortCode("redis")).isEqualTo(6L);
  }
}
