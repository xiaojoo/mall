package com.mall.cart.config;

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
        // 不写死域名：cookie 跟随当前访问域（localhost:5173 不再出现跨域警告）
        cookieSerializer.setCookieName("MALL_SESSION");
        return cookieSerializer;
    }
    
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        // 使用 Spring 官方推荐的 json() 方法
        return RedisSerializer.json();
    }
}
