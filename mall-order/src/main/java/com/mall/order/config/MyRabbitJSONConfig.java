package com.mall.order.config;

import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ JSON 消息转换器配置
 * 使用 Spring AMQP 官方推荐的方式
 *
 * Spring AMQP 4.x 中 JacksonJsonMessageConverter 为推荐类
 * （Jackson2JsonMessageConverter 已过时并标记删除）
 * 官方建议使用默认构造函数，让 Spring Boot 自动配置 ObjectMapper
 */
@Configuration
public class MyRabbitJSONConfig {
    
    /**
     * 使用 JSON 序列化机制进行消息转换
     * 使用 JacksonJsonMessageConverter 的默认构造函数
     */
    @Bean
    public MessageConverter messageConverter() {
        // Spring Boot 4.0 会自动配置 ObjectMapper 和 JacksonJsonMessageConverter
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        // Spring AMQP 默认只信任 java.util/java.lang，必须放行自定义 DTO 包，
        // 否则消费时抛 ListenerExecutionFailedException: Failed to convert message
        // OrderCloseListener 消费 OrderEntity，OrderSeckillListener 消费 SeckillOrderTo
        typeMapper.setTrustedPackages("com.mall.common.to.mq", "com.mall.order.entity");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
