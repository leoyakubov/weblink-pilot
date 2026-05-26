package io.weblinkpilot.url.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.weblinkpilot.shared.contracts.CreateLinkRequest;
import io.weblinkpilot.shared.contracts.LinkCreatedEvent;
import io.weblinkpilot.shared.contracts.LinkResponse;
import io.weblinkpilot.url.codegen.ShortCodeGenerator;
import io.weblinkpilot.url.config.ShortLinkProperties;
import io.weblinkpilot.url.event.LinkPublisher;
import io.weblinkpilot.url.exception.DuplicateAliasException;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UrlCreationServiceTest {

  @Mock private ShortLinkRepository repository;

  @Mock private ShortCodeGenerator shortCodeGenerator;

  @Mock private UrlCacheService cacheService;

  @Mock private LinkPublisher linkPublisher;

  @Mock private PublicUrlBuilder publicUrlBuilder;

  @Mock private ShortLinkProperties shortLinkProperties;

  @InjectMocks private UrlCreationService service;

  @BeforeEach
  void setUp() {
    lenient().when(shortLinkProperties.getMaxExpiration()).thenReturn(Duration.ofDays(365));
  }

  @Test
  void createsCustomAliasLink() {
    when(repository.existsByCustomAlias("github-org")).thenReturn(false);
    when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(publicUrlBuilder.buildShortUrl("github-org"))
        .thenReturn("http://localhost:8080/r/github-org");
    when(publicUrlBuilder.buildQrCodeUrl("github-org"))
        .thenReturn("http://localhost:8080/api/v1/urls/github-org/qr");

    final CreateLinkRequest request =
        new CreateLinkRequest(
            "https://github.com/weblinkpilot/weblink-pilot/tree/main/docs",
            "github-org",
            OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));

    final LinkResponse response = service.create(request);

    assertThat(response.code()).isEqualTo("github-org");
    assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/r/github-org");
    assertThat(response.qrCodeUrl()).isEqualTo("http://localhost:8080/api/v1/urls/github-org/qr");
    assertThat(response.originalUrl())
        .isEqualTo("https://github.com/weblinkpilot/weblink-pilot/tree/main/docs");
    verify(repository).existsByCustomAlias("github-org");
    verify(cacheService).evict("github-org");
    verify(linkPublisher).publish(org.mockito.ArgumentMatchers.any(LinkCreatedEvent.class));
  }

  @Test
  void createsGeneratedAliasLink() {
    when(shortCodeGenerator.generate()).thenReturn("abc1234");
    when(repository.existsByCode("abc1234")).thenReturn(false);
    when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(publicUrlBuilder.buildShortUrl("abc1234")).thenReturn("http://localhost:8080/r/abc1234");
    when(publicUrlBuilder.buildQrCodeUrl("abc1234"))
        .thenReturn("http://localhost:8080/api/v1/urls/abc1234/qr");

    final LinkResponse response =
        service.create(
            new CreateLinkRequest(
                "https://github.com/weblinkpilot/weblink-pilot/about", null, null));

    assertThat(response.code()).isEqualTo("abc1234");
    assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/r/abc1234");
    assertThat(response.qrCodeUrl()).isEqualTo("http://localhost:8080/api/v1/urls/abc1234/qr");
    verify(shortCodeGenerator).generate();
    verify(repository).existsByCode("abc1234");
    verify(cacheService).evict("abc1234");
    verify(linkPublisher).publish(org.mockito.ArgumentMatchers.any(LinkCreatedEvent.class));
  }

  @Test
  void rejectsExpiredRequests() {
    assertThatThrownBy(
            () ->
                service.create(
                    new CreateLinkRequest(
                        "https://github.com/weblinkpilot/weblink-pilot",
                        "expired",
                        OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Expiration time must be in the future");
  }

  @Test
  void rejectsRequestsBeyondMaximumLifetime() {
    assertThatThrownBy(
            () ->
                service.create(
                    new CreateLinkRequest(
                        "https://github.com/weblinkpilot/weblink-pilot",
                        null,
                        OffsetDateTime.now(ZoneOffset.UTC).plusDays(366))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("configured maximum lifetime");
  }

  @Test
  void rejectsDuplicateAliases() {
    when(repository.existsByCustomAlias("dup")).thenReturn(true);

    assertThatThrownBy(
            () ->
                service.create(
                    new CreateLinkRequest(
                        "https://github.com/weblinkpilot/weblink-pilot", "dup", null)))
        .isInstanceOf(DuplicateAliasException.class)
        .hasMessageContaining("dup");
  }

  @Test
  void rejectsInvalidAliasFormat() {
    assertThatThrownBy(
            () ->
                service.create(
                    new CreateLinkRequest(
                        "https://github.com/weblinkpilot/weblink-pilot", "bad alias!", null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Custom alias must be 3-64 characters long");

    verifyNoInteractions(
        repository, cacheService, linkPublisher, shortCodeGenerator, publicUrlBuilder);
  }

  @Test
  void rejectsInvalidUrl() {
    assertThatThrownBy(() -> service.create(new CreateLinkRequest("not-a-url", null, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("absolute");
  }
}
