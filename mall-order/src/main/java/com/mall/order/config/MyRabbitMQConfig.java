package com.mall.order.config;

import com.mall.order.entity.OrderEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class MyRabbitMQConfig {

    /**
     * 支付超时关单时限（分钟）：延迟队列 TTL 与订单详情接口下发的支付截止时间共用此值，
     * 前端支付页倒计时以此为准，避免前端写死与后端不一致。
     */
    public static final int PAY_OVERTIME_MINUTES = 5;

    //    @RabbitListener(queues = "order.release.order.queue")
//    public void listener(OrderEntity entity, Channel channel, Message message) throws Exception {
//        System.out.println("收到过期的订单消息：准备关闭订单" + entity.getOrderSn());
//        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
//    }
    // 容器中的Queue、Exchange、Binding 会自动创建（在RabbitMQ）不存在的情况下

    /**
     * 死信队列
     */
    @Bean
    public Queue orderDelayQueue() {
        /*
            Queue(String name,  队列名字
            boolean durable,    是否持久化
            boolean exclusive,  是否排他
            boolean autoDelete, 是否自动删除
            Map<String, Object> arguments) 属性
         */
        HashMap<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", "order-event-exchange");
        arguments.put("x-dead-letter-routing-key", "order.release.order");
        arguments.put("x-message-ttl", PAY_OVERTIME_MINUTES * 60 * 1000L); // 消息过期时间（支付超时自动关单）
        Queue queue = new Queue("order.delay.queue", true, false, false, arguments);

        return queue;
    }

    /**
     * 普通队列
     */
    @Bean
    public Queue orderReleaseQueue() {

        Queue queue = new Queue("order.release.order.queue", true, false, false);

        return queue;
    }

    /**
     * TopicExchange
     */
    @Bean
    public Exchange orderEventExchange() {
        return new TopicExchange("order-event-exchange", true, false);

    }

    @Bean
    public Binding orderCreateBinding() {
        return new Binding("order.delay.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.create.order",
                null);
    }

    @Bean
    public Binding orderReleaseBinding() {
        return new Binding("order.release.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.order",
                null);
    }

    /**
     * 订单释放直接和库存释放进行绑定
     */
    @Bean
    public Binding orderReleaseOtherBinding() {

        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.other.#",
                null);
    }

    /**
     * 商品秒杀队列
     */
    @Bean
    public Queue orderSecKillOrrderQueue() {
        Queue queue = new Queue("order.seckill.order.queue", true, false, false);
        return queue;
    }

    @Bean
    public Binding orderSecKillOrrderQueueBinding() {
        Binding binding = new Binding(
                "order.seckill.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.seckill.order",
                null);

        return binding;
    }

    /**
     * Hello Java 测试队列、交换机、绑定
     */
    @Bean
    public Queue helloJavaQueue() {
        return new Queue("hello-java-queue", true, false, false);
    }

    @Bean
    public Exchange helloJavaExchange() {
        return new TopicExchange("hello-java-exchange", true, false);
    }

    @Bean
    public Binding helloJavaBinding() {
        return new Binding("hello-java-queue",
                Binding.DestinationType.QUEUE,
                "hello-java-exchange",
                "hello.java",
                null);
    }
}
