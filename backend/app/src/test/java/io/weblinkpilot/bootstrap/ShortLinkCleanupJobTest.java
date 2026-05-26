package io.weblinkpilot.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.weblinkpilot.url.config.ShortLinkProperties;
import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import io.weblinkpilot.url.service.UrlCacheService;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShortLinkCleanupJobTest {

  @Mock private ShortLinkRepository repository;

  @Mock private UrlCacheService cacheService;

  @Mock private ShortLinkProperties shortLinkProperties;

  @InjectMocks private ShortLinkCleanupJob job;

  @Test
  void archivesExpiredLinksOlderThanRetentionWindow() {
    when(shortLinkProperties.getCleanupRetention()).thenReturn(Duration.ofDays(30));
    ShortLink first =
        new ShortLink(
            "first",
            "https://github.com/weblinkpilot/weblink-pilot",
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC).minusDays(60),
            OffsetDateTime.now(ZoneOffset.UTC).minusDays(45));
    ShortLink second =
        new ShortLink(
            "second",
            "https://github.com/weblinkpilot/weblink-pilot/about",
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC).minusDays(50),
            OffsetDateTime.now(ZoneOffset.UTC).minusDays(40));
    when(repository.findExpiredLinksBefore(org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(first, second));

    job.purgeExpiredLinks();

    ArgumentCaptor<OffsetDateTime> cutoffCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
    verify(repository).findExpiredLinksBefore(cutoffCaptor.capture());
    verify(repository).saveAll(List.of(first, second));
    verify(cacheService).evict("first");
    verify(cacheService).evict("second");

    OffsetDateTime cutoff = cutoffCaptor.getValue();
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(cutoff)
        .isBetween(now.minusDays(30).minusSeconds(5), now.minusDays(30).plusSeconds(5));
  }

  @Test
  void skipsCleanupWhenRetentionIsNegative() {
    when(shortLinkProperties.getCleanupRetention()).thenReturn(Duration.ofDays(-1));

    job.purgeExpiredLinks();

    verifyNoInteractions(repository, cacheService);
  }
}
