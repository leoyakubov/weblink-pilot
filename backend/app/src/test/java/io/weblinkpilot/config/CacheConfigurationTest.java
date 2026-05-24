package io.weblinkpilot.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class CacheConfigurationTest {

    private final CacheConfiguration configuration = new CacheConfiguration();

    @Test
    void createsLocalCacheManagerWithExpectedCaches() {
        CacheManager cacheManager = configuration.localCacheManager();

        assertThat(cacheManager.getCache("shortLinks")).isNotNull();
        assertThat(cacheManager.getCache("analyticsCounts")).isNotNull();
        assertThat(cacheManager.getCache("analyticsSummaries")).isNotNull();
    }

    @Test
    void createsRedisCacheManagerWithExpectedCaches() {
        RedisConnectionFactory connectionFactory = org.mockito.Mockito.mock(RedisConnectionFactory.class);
        ObjectMapper objectMapper = new ObjectMapper();

        CacheManager cacheManager = configuration.redisCacheManager(connectionFactory, objectMapper);

        assertThat(cacheManager.getCache("shortLinks")).isNotNull();
        assertThat(cacheManager.getCache("analyticsCounts")).isNotNull();
        assertThat(cacheManager.getCache("analyticsSummaries")).isNotNull();
    }
}
