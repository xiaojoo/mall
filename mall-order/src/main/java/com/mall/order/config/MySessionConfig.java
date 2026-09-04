package com.mall.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class MySessionConfig {
    
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
        cookieSerializer.setCookieName("MALL_SESSION");
        return cookieSerializer;
    }
    
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        // 使用 Spring 官方推荐的 json() 方法
        return RedisSerializer.json();
    }
}
