package io.weblinkpilot.url.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.shared.contracts.CreateLinkRequest;
import io.weblinkpilot.shared.contracts.LinkCreatedEvent;
import io.weblinkpilot.shared.contracts.LinkResponse;
import io.weblinkpilot.url.codegen.Base62Codec;
import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.event.LinkPublisher;
import io.weblinkpilot.url.exception.DuplicateAliasException;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UrlCreationServiceTest {

    @Mock
    private ShortLinkRepository repository;

    @Mock
    private Base62Codec base62Codec;

    @Mock
    private UrlCacheService cacheService;

    @Mock
    private LinkPublisher linkPublisher;

    @Mock
    private PublicUrlBuilder publicUrlBuilder;

    @InjectMocks
    private UrlCreationService service;

    @Test
    void createsCustomAliasLink() {
        when(repository.existsByCustomAlias("github-org")).thenReturn(false);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(publicUrlBuilder.buildShortUrl("github-org")).thenReturn("http://localhost:8080/r/github-org");
        when(publicUrlBuilder.buildQrCodeUrl("github-org")).thenReturn("http://localhost:8080/api/v1/urls/github-org/qr");

        CreateLinkRequest request = new CreateLinkRequest(
                "https://github.com/docs",
                "github-org",
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
        );

        LinkResponse response = service.create(request);

        assertThat(response.code()).isEqualTo("github-org");
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/r/github-org");
        assertThat(response.qrCodeUrl()).isEqualTo("http://localhost:8080/api/v1/urls/github-org/qr");
        assertThat(response.originalUrl()).isEqualTo("https://github.com/docs");
        verify(repository).existsByCustomAlias("github-org");
        verify(cacheService).evict("github-org");
        verify(linkPublisher).publish(org.mockito.ArgumentMatchers.any(LinkCreatedEvent.class));
    }

    @Test
    void createsGeneratedAliasLink() throws Exception {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            ShortLink link = invocation.getArgument(0);
            setId(link, 42L);
            return link;
        });
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(base62Codec.encode(42L)).thenReturn("abc123");
        when(publicUrlBuilder.buildShortUrl("abc123")).thenReturn("http://localhost:8080/r/abc123");
        when(publicUrlBuilder.buildQrCodeUrl("abc123")).thenReturn("http://localhost:8080/api/v1/urls/abc123/qr");

        LinkResponse response = service.create(new CreateLinkRequest(
                "https://google.com/about",
                null,
                null
        ));

        assertThat(response.code()).isEqualTo("abc123");
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/r/abc123");
        assertThat(response.qrCodeUrl()).isEqualTo("http://localhost:8080/api/v1/urls/abc123/qr");
        verify(base62Codec).encode(42L);
        verify(cacheService).evict("abc123");
        verify(linkPublisher).publish(org.mockito.ArgumentMatchers.any(LinkCreatedEvent.class));
    }

    @Test
    void rejectsExpiredRequests() {
        assertThatThrownBy(() -> service.create(new CreateLinkRequest(
                "https://example.com",
                "expired",
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1)
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expiration time must be in the future");
    }

    @Test
    void rejectsDuplicateAliases() {
        when(repository.existsByCustomAlias("dup")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateLinkRequest(
                "https://example.com",
                "dup",
                null
        ))).isInstanceOf(DuplicateAliasException.class)
                .hasMessageContaining("dup");
    }

    @Test
    void rejectsInvalidUrl() {
        assertThatThrownBy(() -> service.create(new CreateLinkRequest(
                "not-a-url",
                null,
                null
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("absolute");
    }

    private static void setId(ShortLink link, Long id) throws Exception {
        Field field = ShortLink.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(link, id);
    }
}
