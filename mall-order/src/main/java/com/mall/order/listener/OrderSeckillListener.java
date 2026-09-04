package com.mall.order.listener;

import com.mall.common.to.mq.SeckillOrderTo;
import com.mall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import lombok.RequiredArgsConstructor;

@Slf4j
@Component
@RabbitListener(queues = "order.seckill.order.queue")
@RequiredArgsConstructor
public class OrderSeckillListener {


    private final OrderService orderService;

    @RabbitHandler
    public void listener(SeckillOrderTo orderTo, Channel channel, Message message) throws IOException {

        log.info("准备创建秒杀单的详细信息...");

        try {
            orderService.createSeckillOrder(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 不 requeue：避免坏消息无限重试打爆队列，记日志人工排查
            log.error("创建秒杀订单失败: orderSn={}, err={}", orderTo.getOrderSn(), e.getMessage(), e);
            // 建单失败回滚库存 + 撤销占位（方法内部自兜底，不抛异常，保证 reject 一定执行）
            orderService.rollbackSeckillStock(orderTo);
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        }
    }
}
