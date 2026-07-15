package com.courierapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_COURIER_WAYS   = "courierWays";
    public static final String CACHE_PACKAGE_TYPES  = "packageTypes";
    public static final String CACHE_DEPARTMENTS    = "departments";
    public static final String CACHE_FLEX_FIELDS    = "flexFields";
    public static final String CACHE_COMPANY_SETTINGS = "companySettings";
    public static final String CACHE_PARTIES        = "parties";
    public static final String CACHE_BOOKINGS       = "bookings";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        var jsonSerializer = new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> perCache = Map.of(
                CACHE_COURIER_WAYS,      defaults.entryTtl(Duration.ofMinutes(30)),
                CACHE_PACKAGE_TYPES,     defaults.entryTtl(Duration.ofMinutes(30)),
                CACHE_DEPARTMENTS,       defaults.entryTtl(Duration.ofMinutes(30)),
                CACHE_FLEX_FIELDS,       defaults.entryTtl(Duration.ofMinutes(30)),
                CACHE_COMPANY_SETTINGS,  defaults.entryTtl(Duration.ofMinutes(10)),
                CACHE_PARTIES,           defaults.entryTtl(Duration.ofMinutes(5)),
                CACHE_BOOKINGS,          defaults.entryTtl(Duration.ofMinutes(2))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults.entryTtl(Duration.ofMinutes(5)))
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    /**
     * Cached entries are serialized with their Java type embedded. If a class shape changes
     * between deploys (a field added/removed on a cached DTO, say), an old entry left over
     * from before the restart can fail to deserialize — without this handler, that failure
     * propagates as a 500 to whoever's request happened to hit it first. Treating a broken
     * cache read/write as a miss instead means the method just runs live and re-caches
     * cleanly, so a stale entry degrades to "slightly slower" rather than "page is broken."
     */
    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache read failed for cache '{}' key '{}' — falling back to a live lookup: {}",
                        cache.getName(), key, exception.toString());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache write failed for cache '{}' key '{}': {}", cache.getName(), key, exception.toString());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache evict failed for cache '{}' key '{}': {}", cache.getName(), key, exception.toString());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache clear failed for cache '{}': {}", cache.getName(), exception.toString());
            }
        };
    }
}
