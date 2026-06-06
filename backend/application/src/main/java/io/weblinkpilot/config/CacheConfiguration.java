package io.weblinkpilot.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import java.time.Duration;
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

  static ObjectMapper redisObjectMapper() {
    BasicPolymorphicTypeValidator validator =
        BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("io.weblinkpilot")
            .allowIfSubType("java.time")
            .allowIfSubType("java.util")
            .build();

    return new ObjectMapper()
        .findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .activateDefaultTyping(
            validator, ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
  }

  @Bean
  @ConditionalOnProperty(name = "app.cache.provider", havingValue = "local", matchIfMissing = true)
  public CacheManager localCacheManager() {
    return new ConcurrentMapCacheManager("shortLinks", "analyticsCounts", "analyticsSummaries");
  }

  @Bean
  @ConditionalOnProperty(name = "app.cache.provider", havingValue = "redis")
  public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration defaults =
        RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .computePrefixWith(cacheName -> "v2::" + cacheName + "::")
            .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer(redisObjectMapper())));

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaults.entryTtl(Duration.ofMinutes(10)))
        .withCacheConfiguration("shortLinks", defaults.entryTtl(Duration.ofMinutes(30)))
        .withCacheConfiguration("analyticsCounts", defaults.entryTtl(Duration.ofSeconds(30)))
        .withCacheConfiguration("analyticsSummaries", defaults.entryTtl(Duration.ofSeconds(30)))
        .build();
  }
}
