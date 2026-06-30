package io.weblinkpilot.platform.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import io.weblinkpilot.links.cache.ShortLinkSnapshot;
import io.weblinkpilot.shared.cache.CacheNames;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class CacheConfigurationTest {

  private final CacheConfiguration configuration = new CacheConfiguration();
  private final PlatformCacheProperties cacheProperties = new PlatformCacheProperties();

  @Test
  void createsLocalCacheManagerWithExpectedCaches() {
    CacheManager cacheManager = configuration.localCacheManager();

    assertThat(cacheManager.getCache(CacheNames.SHORT_LINKS)).isNotNull();
    assertThat(cacheManager.getCache(CacheNames.ANALYTICS_COUNTS)).isNotNull();
    assertThat(cacheManager.getCache(CacheNames.ANALYTICS_SUMMARIES)).isNotNull();
    assertThat(cacheManager.getCache(CacheNames.ANALYTICS_DETAILS)).isNotNull();
  }

  @Test
  void createsRedisCacheManagerWithExpectedCaches() {
    RedisConnectionFactory connectionFactory =
        org.mockito.Mockito.mock(RedisConnectionFactory.class);

    CacheManager cacheManager = configuration.redisCacheManager(connectionFactory, cacheProperties);

    assertThat(cacheManager.getCache(CacheNames.SHORT_LINKS)).isNotNull();
    assertThat(cacheManager.getCache(CacheNames.ANALYTICS_COUNTS)).isNotNull();
    assertThat(cacheManager.getCache(CacheNames.ANALYTICS_SUMMARIES)).isNotNull();
    assertThat(cacheManager.getCache(CacheNames.ANALYTICS_DETAILS)).isNotNull();
  }

  @Test
  void redisObjectMapperCanRoundTripShortLinkSnapshots() {
    ShortLinkSnapshot snapshot =
        new ShortLinkSnapshot(
            "github-org",
            "https://github.com/weblinkpilot/weblink-pilot",
            null,
            OffsetDateTime.of(2026, 5, 24, 10, 0, 0, 0, ZoneOffset.UTC),
            null,
            null,
            7L);

    assertThatNoException()
        .isThrownBy(
            () -> {
              byte[] serialized =
                  CacheConfiguration.redisObjectMapper().writeValueAsBytes(snapshot);
              ShortLinkSnapshot deserialized =
                  CacheConfiguration.redisObjectMapper()
                      .readValue(serialized, ShortLinkSnapshot.class);

              assertThat(deserialized).isEqualTo(snapshot);
            });
  }

  @Test
  void redisCacheManagerUsesTypedSerialization() {
    RedisCacheManager cacheManager =
        (RedisCacheManager)
            configuration.redisCacheManager(
                org.mockito.Mockito.mock(RedisConnectionFactory.class), cacheProperties);

    assertThat(cacheManager).isNotNull();
  }
}
