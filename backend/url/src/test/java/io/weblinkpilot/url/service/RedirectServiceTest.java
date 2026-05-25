package io.weblinkpilot.url.service;

import io.weblinkpilot.shared.contracts.LinkClickedEvent;
import io.weblinkpilot.url.event.LinkPublisher;
import io.weblinkpilot.url.exception.UrlExpiredException;
import io.weblinkpilot.url.exception.UrlNotFoundException;
import io.weblinkpilot.url.repository.ShortLinkRepository;
import io.weblinkpilot.url.web.RedirectRequestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

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

        when(cacheService.findByCode("abc123")).thenReturn(new ShortLinkSnapshot("abc123", "https://github.com/weblinkpilot/weblink-pilot", null, createdAt, null, 5L));
        when(repository.incrementClickCountByCode("abc123")).thenReturn(1);

        String target = service.resolveTarget(
                "abc123",
                new RedirectRequestContext("127.0.0.1", "JUnit", "https://referrer.example", "US")
        );

        assertThat(target).isEqualTo("https://github.com/weblinkpilot/weblink-pilot");
        verify(repository).incrementClickCountByCode("abc123");
        verify(cacheService).evict("abc123");

        ArgumentCaptor<LinkClickedEvent> captor = ArgumentCaptor.forClass(LinkClickedEvent.class);
        verify(linkPublisher).publish(captor.capture());
        assertThat(captor.getValue().code()).isEqualTo("abc123");
        assertThat(captor.getValue().country()).isEqualTo("US");
    }

    @Test
    void resolvesTargetForIpv6ClientIpAndBlankReferrer() {
        RedirectService service = new RedirectService(repository, cacheService, linkPublisher);
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

        when(cacheService.findByCode("ipv6")).thenReturn(new ShortLinkSnapshot("ipv6", "https://github.com/weblinkpilot/weblink-pilot/path", null, createdAt, null, 1L));
        when(repository.incrementClickCountByCode("ipv6")).thenReturn(1);

        String target = service.resolveTarget(
                "ipv6",
                new RedirectRequestContext("2001:db8::1", "JUnit", "  ", "UNKNOWN")
        );

        assertThat(target).isEqualTo("https://github.com/weblinkpilot/weblink-pilot/path");
        verify(cacheService).evict("ipv6");
        verify(linkPublisher).publish(org.mockito.ArgumentMatchers.any(LinkClickedEvent.class));
    }

    @Test
    void throwsWhenCacheMisses() {
        RedirectService service = new RedirectService(repository, cacheService, linkPublisher);

        when(cacheService.findByCode("missing")).thenReturn(null);

        assertThatThrownBy(() -> service.resolveTarget(
                "missing",
                new RedirectRequestContext("127.0.0.1", null, null, "LOCAL")
        )).isInstanceOf(UrlNotFoundException.class);

        verifyNoInteractions(repository);
        verifyNoInteractions(linkPublisher);
    }

    @Test
    void throwsWhenLinkIsExpired() {
        RedirectService service = new RedirectService(repository, cacheService, linkPublisher);

        when(cacheService.findByCode("expired")).thenReturn(new ShortLinkSnapshot(
                "expired",
                "https://github.com/weblinkpilot/weblink-pilot",
                null,
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(2),
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(1),
                3L
        ));

        assertThatThrownBy(() -> service.resolveTarget(
                "expired",
                new RedirectRequestContext("127.0.0.1", null, null, "LOCAL")
        )).isInstanceOf(UrlExpiredException.class);

        verifyNoInteractions(repository);
        verifyNoInteractions(linkPublisher);
    }

    @Test
    void throwsWhenClickCountUpdateFails() {
        RedirectService service = new RedirectService(repository, cacheService, linkPublisher);
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

        when(cacheService.findByCode("missing-db")).thenReturn(new ShortLinkSnapshot("missing-db", "https://github.com/weblinkpilot/weblink-pilot", null, createdAt, null, 1L));
        when(repository.incrementClickCountByCode("missing-db")).thenReturn(0);

        assertThatThrownBy(() -> service.resolveTarget(
                "missing-db",
                new RedirectRequestContext("127.0.0.1", null, null, "LOCAL")
        )).isInstanceOf(UrlNotFoundException.class);

        verify(cacheService).evict("missing-db");
        verifyNoInteractions(linkPublisher);
    }
}
