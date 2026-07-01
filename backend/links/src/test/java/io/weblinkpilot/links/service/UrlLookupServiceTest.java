package io.weblinkpilot.links.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.weblinkpilot.links.cache.ShortLinkSnapshot;
import io.weblinkpilot.links.cache.UrlCacheService;
import io.weblinkpilot.links.domain.ShortLink;
import io.weblinkpilot.links.exception.UrlNotFoundException;
import io.weblinkpilot.links.mapper.LinkResponseMapper;
import io.weblinkpilot.links.repository.ShortLinkRepository;
import io.weblinkpilot.links.support.PublicUrlBuilder;
import io.weblinkpilot.shared.api.ai.AiLinkMetadataResponse;
import io.weblinkpilot.shared.api.links.LinkResponse;
import io.weblinkpilot.shared.ports.LinkAiMetadataService;
import io.weblinkpilot.shared.ports.LinkOwnerMetadataService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class UrlLookupServiceTest {

  @Mock private ShortLinkRepository repository;

  @Mock private UrlCacheService cacheService;

  @Mock private PublicUrlBuilder publicUrlBuilder;

  @Mock private LinkOwnerMetadataService linkOwnerMetadataService;

  @Mock private LinkAiMetadataService linkAiMetadataService;

  private UrlLookupService service;

  @BeforeEach
  void setUp() {
    service =
        new UrlLookupService(
            repository,
            cacheService,
            linkOwnerMetadataService,
            linkAiMetadataService,
            new LinkResponseMapper(publicUrlBuilder, linkOwnerMetadataService));
  }

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

    when(repository.findAll(any(Specification.class), any(Pageable.class)))
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
    verify(repository).findAll(any(Specification.class), pageableCaptor.capture());
    final Pageable pageable = pageableCaptor.getValue();
    assertThat(pageable.getPageSize()).isEqualTo(50);
    assertThat(pageable.getSort())
        .isEqualTo(
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
  }

  @Test
  void listsRecentLinksWithBatchAiMetadata() {
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

    AiLinkMetadataResponse metadata =
        new AiLinkMetadataResponse(
            "two",
            "READY",
            "stub",
            "link-metadata-v1",
            "Second link",
            "Generated summary",
            "Documentation",
            List.of("docs"),
            "docs",
            "second-link",
            null,
            OffsetDateTime.now(ZoneOffset.UTC),
            OffsetDateTime.now(ZoneOffset.UTC));

    when(repository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(second, first)));
    when(linkAiMetadataService.metadataByCodes(List.of("two", "one")))
        .thenReturn(Map.of("two", metadata));
    when(publicUrlBuilder.buildShortUrl("two")).thenReturn("http://localhost:8080/r/two");
    when(publicUrlBuilder.buildShortUrl("one")).thenReturn("http://localhost:8080/r/one");
    when(publicUrlBuilder.buildQrCodeUrl("two"))
        .thenReturn("http://localhost:8080/api/v1/urls/two/qr");
    when(publicUrlBuilder.buildQrCodeUrl("one"))
        .thenReturn("http://localhost:8080/api/v1/urls/one/qr");

    final List<LinkResponse> response = service.listRecentLinks(10);

    assertThat(response).extracting(LinkResponse::code).containsExactly("two", "one");
    assertThat(response.getFirst().aiMetadata()).isEqualTo(metadata);
    assertThat(response.get(1).aiMetadata()).isNull();
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

    when(repository.findAll(any(Specification.class), any(Pageable.class)))
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
    verify(repository).findAll(any(Specification.class), pageableCaptor.capture());
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

    when(repository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(first)));
    when(publicUrlBuilder.buildShortUrl("one")).thenReturn("http://localhost:8080/r/one");
    when(publicUrlBuilder.buildQrCodeUrl("one"))
        .thenReturn("http://localhost:8080/api/v1/urls/one/qr");

    final List<LinkResponse> response = service.listRecentLinks("admin", true, "anonymous", 99);

    assertThat(response).extracting(LinkResponse::code).containsExactly("one");
    verify(repository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void filtersRecentLinksByExpirationStatus() {
    final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    final ShortLink active =
        new ShortLink(
            "active",
            "https://github.com/weblinkpilot/weblink-pilot/active",
            null,
            null,
            now.minusMinutes(1),
            now.plusDays(1));
    final ShortLink expired =
        new ShortLink(
            "expired",
            "https://github.com/weblinkpilot/weblink-pilot/expired",
            null,
            null,
            now.minusMinutes(2),
            now.minusMinutes(1));
    final ShortLink never =
        new ShortLink(
            "never",
            "https://github.com/weblinkpilot/weblink-pilot/never",
            null,
            null,
            now.minusMinutes(3),
            null);

    when(repository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(active, never)));
    when(publicUrlBuilder.buildShortUrl("active")).thenReturn("http://localhost:8080/r/active");
    when(publicUrlBuilder.buildQrCodeUrl("active"))
        .thenReturn("http://localhost:8080/api/v1/urls/active/qr");
    when(publicUrlBuilder.buildShortUrl("never")).thenReturn("http://localhost:8080/r/never");
    when(publicUrlBuilder.buildQrCodeUrl("never"))
        .thenReturn("http://localhost:8080/api/v1/urls/never/qr");

    final List<LinkResponse> response =
        service.listRecentLinks("admin", true, null, null, "ACTIVE", 10);

    assertThat(response).extracting(LinkResponse::code).containsExactly("active", "never");
    final ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(repository).findAll(any(Specification.class), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
  }
}
