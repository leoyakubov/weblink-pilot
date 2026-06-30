package io.weblinkpilot.links.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.weblinkpilot.links.cache.UrlCacheService;
import io.weblinkpilot.links.domain.ShortLink;
import io.weblinkpilot.links.repository.ShortLinkRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShortLinkCleanupServiceTest {

  @Mock private ShortLinkRepository repository;

  @Mock private UrlCacheService cacheService;

  @Test
  void archivesExpiredLinksOlderThanRetentionWindow() {
    ShortLinkCleanupService service = new ShortLinkCleanupService(repository, cacheService);

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
    org.mockito.Mockito.when(repository.findExpiredLinksBefore(org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(first, second));

    service.purgeExpiredLinks(Duration.ofDays(30));

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
    ShortLinkCleanupService service = new ShortLinkCleanupService(repository, cacheService);

    service.purgeExpiredLinks(Duration.ofDays(-1));

    verifyNoInteractions(repository, cacheService);
  }
}
