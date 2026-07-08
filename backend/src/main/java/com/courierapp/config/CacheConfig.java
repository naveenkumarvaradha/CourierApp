package com.courierapp.config;

import org.springframework.cache.annotation.EnableCaching;
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
}
