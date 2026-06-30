package io.weblinkpilot.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.weblinkpilot.analytics.domain.ClickEvent;
import io.weblinkpilot.analytics.event.AnalyticsCacheInvalidationRequestedEvent;
import io.weblinkpilot.analytics.repository.ClickEventRepository;
import io.weblinkpilot.analytics.useragent.BrowserFamily;
import io.weblinkpilot.analytics.useragent.DeviceType;
import io.weblinkpilot.analytics.useragent.UserAgentMetadata;
import io.weblinkpilot.analytics.useragent.UserAgentParser;
import io.weblinkpilot.shared.events.LinkClickedEvent;
import io.weblinkpilot.shared.types.LinkTrackingSource;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ClickEventRecorderTest {

  @Mock private ClickEventRepository repository;

  @Mock private UserAgentParser userAgentParser;

  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private ClickEventRecorder recorder;

  @Test
  void recordsEnrichedClickEvent() {
    when(userAgentParser.parse("Mozilla/5.0"))
        .thenReturn(new UserAgentMetadata(BrowserFamily.CHROME, DeviceType.DESKTOP));

    LinkClickedEvent event =
        new LinkClickedEvent(
            "demo",
            OffsetDateTime.now(ZoneOffset.UTC),
            LinkTrackingSource.REDIRECT,
            "127.0.0.1",
            "Mozilla/5.0",
            "https://github.com",
            "US");

    recorder.record(event);

    ArgumentCaptor<ClickEvent> captor = ArgumentCaptor.forClass(ClickEvent.class);
    verify(repository).save(captor.capture());
    verify(eventPublisher).publishEvent(new AnalyticsCacheInvalidationRequestedEvent("demo"));
    ClickEvent saved = captor.getValue();
    assertThat(saved.getShortCode()).isEqualTo("demo");
    assertThat(saved.getEventSource()).isEqualTo(LinkTrackingSource.REDIRECT);
    assertThat(saved.getCountry()).isEqualTo("US");
    assertThat(saved.getBrowserFamily()).isEqualTo("CHROME");
    assertThat(saved.getDeviceType()).isEqualTo("DESKTOP");
  }
}
