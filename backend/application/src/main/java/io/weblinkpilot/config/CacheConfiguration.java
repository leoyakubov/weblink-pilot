package io.weblinkpilot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.cache.provider", havingValue = "local", matchIfMissing = true)
    public CacheManager localCacheManager() {
        return new ConcurrentMapCacheManager("shortLinks", "analytics");
    }
}
