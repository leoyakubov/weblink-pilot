package io.weblinkpilot.url.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UrlCacheServiceTest {

  @Mock private ShortLinkRepository repository;

  @InjectMocks private UrlCacheService service;

  @Test
  void mapsRepositoryEntityToSnapshot() {
    ShortLink link =
        new ShortLink(
            "abc123",
            "https://github.com/weblinkpilot/weblink-pilot",
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC),
            null);
    when(repository.findByCode("abc123")).thenReturn(Optional.of(link));

    ShortLinkSnapshot snapshot = service.findByCode("abc123");

    assertThat(snapshot.code()).isEqualTo("abc123");
    assertThat(snapshot.originalUrl()).isEqualTo("https://github.com/weblinkpilot/weblink-pilot");
  }
}
