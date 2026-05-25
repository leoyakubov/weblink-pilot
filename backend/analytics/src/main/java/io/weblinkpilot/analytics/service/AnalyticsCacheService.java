package io.weblinkpilot.analytics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsCacheService {

  private static final Logger log = LoggerFactory.getLogger(AnalyticsCacheService.class);

  @Caching(
      evict = {
        @CacheEvict(cacheNames = "analyticsCounts", key = "#code"),
        @CacheEvict(cacheNames = "analyticsSummaries", key = "#code")
      })
  public void evict(String code) {
    log.debug("analytics.cache.evict code={}", code);
  }
}
