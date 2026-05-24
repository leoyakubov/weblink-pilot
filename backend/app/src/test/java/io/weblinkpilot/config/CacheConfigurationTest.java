package io.weblinkpilot.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.weblinkpilot.url.service.ShortLinkSnapshot;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

        CacheManager cacheManager = configuration.redisCacheManager(connectionFactory);

        assertThat(cacheManager.getCache("shortLinks")).isNotNull();
        assertThat(cacheManager.getCache("analyticsCounts")).isNotNull();
        assertThat(cacheManager.getCache("analyticsSummaries")).isNotNull();
    }

    @Test
    void redisObjectMapperCanSerializeJavaTimeTypes() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ShortLinkSnapshot snapshot = new ShortLinkSnapshot(
                "github-org",
                "https://example.com",
                OffsetDateTime.of(2026, 5, 24, 10, 0, 0, 0, ZoneOffset.UTC),
                null,
                7L
        );

        String json = objectMapper.writeValueAsString(snapshot);

        assertThat(json).contains("github-org");
        assertThat(json).contains("2026-05-24T10:00:00Z");
        assertThat(json).doesNotContain("1779616800.000000000");
    }
}
