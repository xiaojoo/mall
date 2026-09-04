package com.mall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyRedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:CHANGE_ME}")
    private String redisPassword;

    /**
     * 多有对Redisson的使用都是通过RedissonClient对象
     */
    @Bean(destroyMethod = "shutdown")
    @Lazy
    public RedissonClient redisson() {
        Config config = new Config();
        // Redisson 4.x：密码设置在 Config 上（SingleServerConfig.setPassword 已弃用）
        config.setPassword(redisPassword);
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort);
        return Redisson.create(config);
    }
}
