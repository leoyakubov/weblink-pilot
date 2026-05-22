package io.weblinkpilot.url.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.shared.contracts.CreateLinkRequest;
import io.weblinkpilot.shared.contracts.LinkCreatedEvent;
import io.weblinkpilot.shared.contracts.LinkResponse;
import io.weblinkpilot.url.codegen.Base62Codec;
import io.weblinkpilot.url.event.LinkPublisher;
import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private ShortLinkRepository repository;

    @Mock
    private Base62Codec base62Codec;

    @Mock
    private UrlCacheService cacheService;

    @Mock
    private LinkPublisher linkPublisher;

    @Test
    void createsLinkWithCustomAliasAndPublishesEvent() {
        UrlService service = new UrlService(repository, base62Codec, cacheService, linkPublisher);
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        ShortLink saved = new ShortLink("my-link", "https://example.com", "my-link", createdAt, null);

        when(repository.existsByCustomAlias("my-link")).thenReturn(false);
        when(repository.saveAndFlush(org.mockito.ArgumentMatchers.any(ShortLink.class))).thenReturn(saved);

        LinkResponse response = service.create(
                new CreateLinkRequest("https://example.com", "my-link", null),
                "http://localhost:8080"
        );

        assertThat(response.code()).isEqualTo("my-link");
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/r/my-link");
        assertThat(response.qrCodeUrl()).isEqualTo("http://localhost:8080/api/v1/urls/my-link/qr");
        assertThat(response.clickCount()).isZero();

        ArgumentCaptor<LinkCreatedEvent> captor = ArgumentCaptor.forClass(LinkCreatedEvent.class);
        verify(linkPublisher).publish(captor.capture());
        assertThat(captor.getValue().code()).isEqualTo("my-link");
    }
}
