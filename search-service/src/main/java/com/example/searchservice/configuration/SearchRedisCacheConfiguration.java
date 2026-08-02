package com.example.searchservice.configuration;

import com.example.searchservice.document.ProductDocument;
import com.example.searchservice.dto.response.AggregationResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Configuration
public class SearchRedisCacheConfiguration {

    public static final String SUGGESTIONS_CACHE = "productSuggestions";
    public static final String AGGREGATIONS_CACHE = "productAggregations";

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "search-service:" + cacheName + "::");

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        SUGGESTIONS_CACHE, listCacheConfig(defaults, objectMapper, ProductDocument.class, Duration.ofSeconds(30)),
                        AGGREGATIONS_CACHE, objectCacheConfig(defaults, objectMapper, AggregationResponse.class, Duration.ofSeconds(60))
                ))
                .build();
    }

    private RedisCacheConfiguration listCacheConfig(
            RedisCacheConfiguration defaults,
            ObjectMapper objectMapper,
            Class<?> itemType,
            Duration ttl
    ) {
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, itemType);
        return serializedCacheConfig(defaults, objectMapper, listType, ttl);
    }

    private RedisCacheConfiguration objectCacheConfig(
            RedisCacheConfiguration defaults,
            ObjectMapper objectMapper,
            Class<?> valueType,
            Duration ttl
    ) {
        return serializedCacheConfig(defaults, objectMapper, objectMapper.constructType(valueType), ttl);
    }

    private RedisCacheConfiguration serializedCacheConfig(
            RedisCacheConfiguration defaults,
            ObjectMapper objectMapper,
            JavaType valueType,
            Duration ttl
    ) {
        JacksonJsonRedisSerializer<Object> serializer = new JacksonJsonRedisSerializer<Object>(objectMapper, valueType);
        return defaults.entryTtl(ttl)
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }
}
