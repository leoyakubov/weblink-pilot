package io.weblinkpilot.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.url.config.ShortLinkProperties;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShortLinkCleanupJobTest {

  @Mock private ShortLinkRepository repository;

  @Mock private ShortLinkProperties shortLinkProperties;

  @InjectMocks private ShortLinkCleanupJob job;

  @Test
  void purgesExpiredLinksOlderThanRetentionWindow() {
    when(shortLinkProperties.getCleanupRetention()).thenReturn(Duration.ofDays(30));
    when(repository.deleteExpiredLinksBefore(org.mockito.ArgumentMatchers.any()))
        .thenReturn(2);

    job.purgeExpiredLinks();

    ArgumentCaptor<OffsetDateTime> cutoffCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
    verify(repository).deleteExpiredLinksBefore(cutoffCaptor.capture());

    OffsetDateTime cutoff = cutoffCaptor.getValue();
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    assertThat(cutoff).isBetween(now.minusDays(30).minusSeconds(5), now.minusDays(30).plusSeconds(5));
  }
}
