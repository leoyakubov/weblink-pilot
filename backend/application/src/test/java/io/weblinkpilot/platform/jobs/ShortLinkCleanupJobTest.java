package io.weblinkpilot.platform.jobs;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.links.config.ShortLinkProperties;
import io.weblinkpilot.links.service.ShortLinkCleanupService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShortLinkCleanupJobTest {

  @Mock private ShortLinkCleanupService cleanupService;

  @Mock private ShortLinkProperties shortLinkProperties;

  private ShortLinkCleanupJob job;

  @BeforeEach
  void setUp() {
    job = new ShortLinkCleanupJob(cleanupService, shortLinkProperties);
  }

  @Test
  void delegatesCleanupWithConfiguredRetention() {
    when(shortLinkProperties.getCleanupRetention()).thenReturn(Duration.ofDays(30));

    job.purgeExpiredLinks();

    verify(cleanupService).purgeExpiredLinks(Duration.ofDays(30));
  }
}
