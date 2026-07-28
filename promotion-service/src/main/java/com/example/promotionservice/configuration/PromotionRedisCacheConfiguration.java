package com.example.promotionservice.configuration;

import com.example.promotionservice.dto.response.FlashDealResponse;
import com.example.promotionservice.dto.response.PromotionCampaignResponse;
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
public class PromotionRedisCacheConfiguration {

    public static final String ACTIVE_PROMOTIONS_CACHE = "activePromotions";
    public static final String FLASH_DEALS_CACHE = "flashDealsByStatusAndType";

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "promotion-service:" + cacheName + "::");

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        ACTIVE_PROMOTIONS_CACHE, listCacheConfig(
                                defaults,
                                objectMapper,
                                PromotionCampaignResponse.class,
                                Duration.ofSeconds(60)
                        ),
                        FLASH_DEALS_CACHE, listCacheConfig(
                                defaults,
                                objectMapper,
                                FlashDealResponse.class,
                                Duration.ofSeconds(30)
                        )
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
        JacksonJsonRedisSerializer<Object> serializer = new JacksonJsonRedisSerializer<Object>(objectMapper, listType);
        return defaults.entryTtl(ttl)
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }
}
