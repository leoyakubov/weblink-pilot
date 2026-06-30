package io.weblinkpilot.analytics.event;

import io.weblinkpilot.analytics.service.AnalyticsCacheService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AnalyticsCacheInvalidationEventListener {

  private final AnalyticsCacheService analyticsCacheService;

  public AnalyticsCacheInvalidationEventListener(AnalyticsCacheService analyticsCacheService) {
    this.analyticsCacheService = analyticsCacheService;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onAnalyticsCacheInvalidationRequested(
      AnalyticsCacheInvalidationRequestedEvent event) {
    analyticsCacheService.evict(event.code());
  }
}
