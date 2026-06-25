package io.weblinkpilot.links.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.weblinkpilot.links.domain.ShortLink;
import io.weblinkpilot.links.exception.UrlNotFoundException;
import io.weblinkpilot.links.repository.ShortLinkRepository;
import io.weblinkpilot.shared.contracts.LinkResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class UrlLookupServiceTest {

  @Mock private ShortLinkRepository repository;

  @Mock private UrlCacheService cacheService;

  @Mock private PublicUrlBuilder publicUrlBuilder;

  @Mock private LinkOwnerMetadataService linkOwnerMetadataService;

  @InjectMocks private UrlLookupService service;

  @Test
  void returnsLinkWhenCacheHit() {
    final ShortLinkSnapshot snapshot =
        new ShortLinkSnapshot(
            "abc123",
            "https://github.com/weblinkpilot/weblink-pilot",
            null,
            OffsetDateTime.now(ZoneOffset.UTC),
            null,
            null,
            2L);

    when(cacheService.findByCode("abc123")).thenReturn(snapshot);
    when(publicUrlBuilder.buildShortUrl("abc123")).thenReturn("http://localhost:8080/r/abc123");
    when(publicUrlBuilder.buildQrCodeUrl("abc123"))
        .thenReturn("http://localhost:8080/api/v1/urls/abc123/qr");

    final LinkResponse response = service.getByCode("abc123");

    assertThat(response.code()).isEqualTo("abc123");
    assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/r/abc123");
    assertThat(response.qrCodeUrl()).isEqualTo("http://localhost:8080/api/v1/urls/abc123/qr");
    assertThat(response.clickCount()).isEqualTo(2L);
    verify(cacheService).findByCode("abc123");
  }

  @Test
  void throwsWhenCacheMisses() {
    when(cacheService.findByCode("missing")).thenReturn(null);

    assertThatThrownBy(() -> service.getByCode("missing")).isInstanceOf(UrlNotFoundException.class);

    verify(cacheService).findByCode("missing");
    verifyNoInteractions(repository);
  }

  @Test
  void throwsGoneWhenSnapshotIsArchived() {
    when(cacheService.findByCode("archived"))
        .thenReturn(
            new ShortLinkSnapshot(
                "archived",
                "https://github.com/weblinkpilot/weblink-pilot",
                null,
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(10),
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(5),
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(2),
                2L));

    assertThatThrownBy(() -> service.getByCode("archived"))
        .isInstanceOf(io.weblinkpilot.links.exception.UrlExpiredException.class);
  }

  @Test
  void listsRecentLinksWithClampedLimitAndNewestSort() {
    final ShortLink first =
        new ShortLink(
            "one",
            "https://github.com/weblinkpilot/weblink-pilot/one",
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1),
            null);
    final ShortLink second =
        new ShortLink(
            "two",
            "https://github.com/weblinkpilot/weblink-pilot/two",
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC),
            null);

    when(repository.findAllByOwnerUsernameIsNullAndDeletedAtIsNull(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(second, first)));
    when(publicUrlBuilder.buildShortUrl("two")).thenReturn("http://localhost:8080/r/two");
    when(publicUrlBuilder.buildShortUrl("one")).thenReturn("http://localhost:8080/r/one");
    when(publicUrlBuilder.buildQrCodeUrl("two"))
        .thenReturn("http://localhost:8080/api/v1/urls/two/qr");
    when(publicUrlBuilder.buildQrCodeUrl("one"))
        .thenReturn("http://localhost:8080/api/v1/urls/one/qr");

    final List<LinkResponse> response = service.listRecentLinks(99);

    assertThat(response).extracting(LinkResponse::code).containsExactly("two", "one");
    final ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(repository).findAllByOwnerUsernameIsNullAndDeletedAtIsNull(pageableCaptor.capture());
    final Pageable pageable = pageableCaptor.getValue();
    assertThat(pageable.getPageSize()).isEqualTo(50);
    assertThat(pageable.getSort())
        .isEqualTo(
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
  }

  @Test
  void adminsCanFilterRecentLinksByCreator() {
    final ShortLink first =
        new ShortLink(
            "one",
            "https://github.com/weblinkpilot/weblink-pilot/one",
            null,
            "alice",
            OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1),
            null);
    final ShortLink second =
        new ShortLink(
            "two",
            "https://github.com/weblinkpilot/weblink-pilot/two",
            null,
            "alice",
            OffsetDateTime.now(ZoneOffset.UTC),
            null);

    when(repository.findAllByOwnerUsernameAndDeletedAtIsNull(anyString(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(second, first)));
    when(publicUrlBuilder.buildShortUrl("two")).thenReturn("http://localhost:8080/r/two");
    when(publicUrlBuilder.buildShortUrl("one")).thenReturn("http://localhost:8080/r/one");
    when(publicUrlBuilder.buildQrCodeUrl("two"))
        .thenReturn("http://localhost:8080/api/v1/urls/two/qr");
    when(publicUrlBuilder.buildQrCodeUrl("one"))
        .thenReturn("http://localhost:8080/api/v1/urls/one/qr");

    final List<LinkResponse> response = service.listRecentLinks("admin", true, "Alice", 99);

    assertThat(response).extracting(LinkResponse::code).containsExactly("two", "one");
    final ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(repository)
        .findAllByOwnerUsernameAndDeletedAtIsNull(anyString(), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
  }

  @Test
  void adminsCanFilterAnonymousRecentLinks() {
    final ShortLink first =
        new ShortLink(
            "one",
            "https://github.com/weblinkpilot/weblink-pilot/one",
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1),
            null);

    when(repository.findAllByOwnerUsernameIsNullAndDeletedAtIsNull(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(first)));
    when(publicUrlBuilder.buildShortUrl("one")).thenReturn("http://localhost:8080/r/one");
    when(publicUrlBuilder.buildQrCodeUrl("one"))
        .thenReturn("http://localhost:8080/api/v1/urls/one/qr");

    final List<LinkResponse> response = service.listRecentLinks("admin", true, "anonymous", 99);

    assertThat(response).extracting(LinkResponse::code).containsExactly("one");
    verify(repository).findAllByOwnerUsernameIsNullAndDeletedAtIsNull(any(Pageable.class));
  }
}
