package io.weblinkpilot.analytics.service;

import static org.mockito.Mockito.verify;

import io.weblinkpilot.shared.contracts.LinkClickedEvent;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClickEventConsumerTest {

    @Mock
    private ClickEventRecorder recorder;

    @InjectMocks
    private ClickEventConsumer consumer;

    @Test
    void delegatesToRecorder() {
        LinkClickedEvent event = new LinkClickedEvent(
                "demo",
                OffsetDateTime.now(ZoneOffset.UTC),
                "127.0.0.1",
                "Mozilla/5.0",
                "https://github.com",
                "US"
        );

        consumer.handle(event);

        verify(recorder).record(event);
    }
}
