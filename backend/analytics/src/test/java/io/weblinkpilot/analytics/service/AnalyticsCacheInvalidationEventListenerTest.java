package io.weblinkpilot.analytics.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsCacheInvalidationEventListenerTest {

  @Mock private AnalyticsCacheService analyticsCacheService;

  private AnalyticsCacheInvalidationEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new AnalyticsCacheInvalidationEventListener(analyticsCacheService);
  }

  @Test
  void evictsAnalyticsCacheForRequestedCode() {
    listener.onAnalyticsCacheInvalidationRequested(
        new AnalyticsCacheInvalidationRequestedEvent("demo"));

    verify(analyticsCacheService).evict("demo");
  }
}
