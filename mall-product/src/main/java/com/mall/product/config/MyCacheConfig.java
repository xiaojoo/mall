package com.mall.product.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import lombok.RequiredArgsConstructor;

/**
 * 缓存配置类
 * 使用 Spring 官方推荐的 RedisSerializer.json() 方法
 */
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class MyCacheConfig {
    
    @Bean
    RedisCacheConfiguration redisCacheConfiguration() {
        // 使用 Spring 官方推荐的 json() 方法
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();
        
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        config = config.serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
        );
        config = config.serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
        );
        return config;
    }
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration redisCacheConfiguration = redisCacheConfiguration();
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(redisCacheConfiguration)
                .build();
    }
}
