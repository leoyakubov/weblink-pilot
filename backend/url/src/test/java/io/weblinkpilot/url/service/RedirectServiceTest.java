package io.weblinkpilot.url.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.shared.contracts.LinkClickedEvent;
import io.weblinkpilot.url.event.LinkPublisher;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import io.weblinkpilot.url.web.RedirectRequestContext;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

        when(cacheService.findByCode("abc123")).thenReturn(new ShortLinkSnapshot("abc123", "https://example.com", createdAt, null, 5L));
        when(repository.incrementClickCountByCode("abc123")).thenReturn(1);

        String target = service.resolveTarget(
                "abc123",
                new RedirectRequestContext("127.0.0.1", "JUnit", "https://referrer.example", "US")
        );

        assertThat(target).isEqualTo("https://example.com");
        verify(repository).incrementClickCountByCode("abc123");
        verify(cacheService).evict("abc123");

        ArgumentCaptor<LinkClickedEvent> captor = ArgumentCaptor.forClass(LinkClickedEvent.class);
        verify(linkPublisher).publish(captor.capture());
        assertThat(captor.getValue().code()).isEqualTo("abc123");
        assertThat(captor.getValue().country()).isEqualTo("US");
    }
}
