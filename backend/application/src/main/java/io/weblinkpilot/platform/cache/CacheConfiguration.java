package io.weblinkpilot.platform.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import io.weblinkpilot.platform.PlatformPropertyKeys;
import io.weblinkpilot.shared.cache.CacheNames;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfiguration {

  private static final String CACHE_PREFIX_VERSION = "v2";
  private static final String CACHE_PREFIX_SEPARATOR = "::";
  private static final String APPLICATION_PACKAGE_PREFIX = "io.weblinkpilot";
  private static final String JAVA_TIME_PACKAGE_PREFIX = "java.time";
  private static final String JAVA_UTIL_PACKAGE_PREFIX = "java.util";

  static ObjectMapper redisObjectMapper() {
    BasicPolymorphicTypeValidator validator =
        BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(APPLICATION_PACKAGE_PREFIX)
            .allowIfSubType(JAVA_TIME_PACKAGE_PREFIX)
            .allowIfSubType(JAVA_UTIL_PACKAGE_PREFIX)
            .build();

    return new ObjectMapper()
        .findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .activateDefaultTyping(
            validator, ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
  }

  @Bean
  @ConditionalOnProperty(
      name = PlatformPropertyKeys.CACHE_PROVIDER,
      havingValue = CacheProviderNames.LOCAL,
      matchIfMissing = true)
  public CacheManager localCacheManager() {
    return new ConcurrentMapCacheManager(
        CacheNames.SHORT_LINKS,
        CacheNames.ANALYTICS_COUNTS,
        CacheNames.ANALYTICS_SUMMARIES,
        CacheNames.ANALYTICS_DETAILS);
  }

  @Bean
  @ConditionalOnProperty(
      name = PlatformPropertyKeys.CACHE_PROVIDER,
      havingValue = CacheProviderNames.REDIS)
  public CacheManager redisCacheManager(
      RedisConnectionFactory connectionFactory, PlatformCacheProperties cacheProperties) {
    RedisCacheConfiguration defaults =
        RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .computePrefixWith(
                cacheName ->
                    CACHE_PREFIX_VERSION
                        + CACHE_PREFIX_SEPARATOR
                        + cacheName
                        + CACHE_PREFIX_SEPARATOR)
            .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer(redisObjectMapper())));

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaults.entryTtl(cacheProperties.getTtl().getDefaultTtl()))
        .withCacheConfiguration(
            CacheNames.SHORT_LINKS, defaults.entryTtl(cacheProperties.getTtl().getShortLinks()))
        .withCacheConfiguration(
            CacheNames.ANALYTICS_COUNTS, defaults.entryTtl(cacheProperties.getTtl().getAnalytics()))
        .withCacheConfiguration(
            CacheNames.ANALYTICS_SUMMARIES,
            defaults.entryTtl(cacheProperties.getTtl().getAnalytics()))
        .withCacheConfiguration(
            CacheNames.ANALYTICS_DETAILS,
            defaults.entryTtl(cacheProperties.getTtl().getAnalytics()))
        .build();
  }
}
