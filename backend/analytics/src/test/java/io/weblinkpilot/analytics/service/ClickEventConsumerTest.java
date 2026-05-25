package io.weblinkpilot.analytics.service;

import io.weblinkpilot.shared.contracts.LinkClickedEvent;
import io.weblinkpilot.shared.contracts.LinkTrackingSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.Mockito.verify;

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
                LinkTrackingSource.QR_SCAN,
                "127.0.0.1",
                "Mozilla/5.0",
                "https://github.com",
                "US"
        );

        consumer.handle(event);

        verify(recorder).record(event);
    }
}
