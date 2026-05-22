package io.weblinkpilot.url.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.shared.contracts.LinkClickedEvent;
import io.weblinkpilot.url.domain.ShortLink;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RedirectServiceTest {

    @Mock
    private ShortLinkRepository repository;

    @Mock
    private UrlCacheService cacheService;

    @Mock
    private LinkPublisher linkPublisher;

    @Test
    void resolvesTargetAndIncrementsClickCount() {
        RedirectService service = new RedirectService(repository, cacheService, linkPublisher);
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        ShortLink link = new ShortLink("abc123", "https://example.com", null, createdAt, null);

        when(cacheService.findByCode("abc123")).thenReturn(new ShortLinkSnapshot("abc123", "https://example.com", createdAt, null));
        when(repository.findByCode("abc123")).thenReturn(Optional.of(link));

        String target = service.resolveTarget("abc123", "127.0.0.1", "JUnit", "https://referrer.example");

        assertThat(target).isEqualTo("https://example.com");
        assertThat(link.getClickCount()).isEqualTo(1);
        verify(repository).save(link);

        ArgumentCaptor<LinkClickedEvent> captor = ArgumentCaptor.forClass(LinkClickedEvent.class);
        verify(linkPublisher).publish(captor.capture());
        assertThat(captor.getValue().code()).isEqualTo("abc123");
    }
}
