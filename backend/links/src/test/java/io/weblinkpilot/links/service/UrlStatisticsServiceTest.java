package io.weblinkpilot.links.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.weblinkpilot.links.repository.ShortLinkRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UrlStatisticsServiceTest {

  @Mock private ShortLinkRepository repository;

  private UrlStatisticsService service;

  @BeforeEach
  void setUp() {
    service = new UrlStatisticsService(repository);
  }

  @Test
  void syncsClickCountsByCode() {
    service.syncClickCounts(Map.of("redis", 6L, "spring-boot", 12L));

    verify(repository).updateClickCountByCode("redis", 6L);
    verify(repository).updateClickCountByCode("spring-boot", 12L);
  }

  @Test
  void skipsInvalidClickCountEntries() {
    service.syncClickCounts(Map.of("redis", -1L, " ", 4L));

    verify(repository, never()).updateClickCountByCode("redis", -1L);
    verify(repository, never()).updateClickCountByCode("", 4L);
  }

  @Test
  void ignoresEmptyInput() {
    service.syncClickCounts(Map.of());

    verifyNoInteractions(repository);
  }

  @Test
  void delegatesTotalClickCount() {
    when(repository.sumClickCount()).thenReturn(26L);

    service.sumClickCount();

    verify(repository).sumClickCount();
  }
}
