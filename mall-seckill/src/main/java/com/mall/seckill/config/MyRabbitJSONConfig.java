package com.mall.seckill.config;

import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ JSON 消息转换器配置
 * 使用 Spring AMQP 官方推荐的方式
 */
@Configuration
public class MyRabbitJSONConfig {
    
    /**
     * 使用 JSON 序列化机制进行消息转换
     */
    @Bean
    public MessageConverter messageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        // Spring AMQP 默认只信任 java.util/java.lang，必须放行自定义 DTO 包，
        // 否则消费时抛 ListenerExecutionFailedException: Failed to convert message
        typeMapper.setTrustedPackages("com.mall.common.to.mq");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
