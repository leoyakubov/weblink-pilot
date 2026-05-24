package io.weblinkpilot.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.analytics.domain.ClickEvent;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.shared.contracts.LinkClickedEvent;
import io.weblinkpilot.shared.contracts.LinkTrackingSource;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClickEventRecorderTest {

    @Mock
    private ClickEventRepository repository;

    @Mock
    private UserAgentParser userAgentParser;

    @Mock
    private AnalyticsCacheService analyticsCacheService;

    @InjectMocks
    private ClickEventRecorder recorder;

    @Test
    void recordsEnrichedClickEvent() {
        when(userAgentParser.parse("Mozilla/5.0")).thenReturn(new UserAgentMetadata("Chrome", "Desktop"));

        LinkClickedEvent event = new LinkClickedEvent(
                "demo",
                OffsetDateTime.now(ZoneOffset.UTC),
                LinkTrackingSource.REDIRECT,
                "127.0.0.1",
                "Mozilla/5.0",
                "https://github.com",
                "US"
        );

        recorder.record(event);

        ArgumentCaptor<ClickEvent> captor = ArgumentCaptor.forClass(ClickEvent.class);
        verify(repository).save(captor.capture());
        verify(analyticsCacheService).evict("demo");
        ClickEvent saved = captor.getValue();
        assertThat(saved.getShortCode()).isEqualTo("demo");
        assertThat(saved.getEventSource()).isEqualTo(LinkTrackingSource.REDIRECT);
        assertThat(saved.getCountry()).isEqualTo("US");
        assertThat(saved.getBrowserFamily()).isEqualTo("Chrome");
        assertThat(saved.getDeviceType()).isEqualTo("Desktop");
    }
}
