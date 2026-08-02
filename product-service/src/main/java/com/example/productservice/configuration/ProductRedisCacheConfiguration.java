package com.example.productservice.configuration;

import com.example.productservice.dto.response.CategoryDetailResponse;
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
public class ProductRedisCacheConfiguration {

    public static final String CATEGORY_LIST_CACHE = "categoryList";

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        JavaType categoryListType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, CategoryDetailResponse.class);
        //Danh sách category được chuyển thành JSON trước khi lưu Redis.
        JacksonJsonRedisSerializer<Object> categoryListSerializer =
                new JacksonJsonRedisSerializer<Object>(objectMapper, categoryListType);
    //Cấu hình mặc định
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "product-service:" + cacheName + "::");
        //TTL
        RedisCacheConfiguration categoryListConfig = defaults
                .entryTtl(Duration.ofMinutes(30))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(categoryListSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(CATEGORY_LIST_CACHE, categoryListConfig))
                .build();
    }
}
