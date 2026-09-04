package com.mall.order.service.impl;

import com.mall.order.entity.OrderEntity;
import com.mall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.order.dao.OrderItemDao;
import com.mall.order.entity.OrderItemEntity;
import com.mall.order.service.OrderItemService;

@RabbitListener(queues = {"hello-java-queue"})
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new LambdaQueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    // @RabbitListener(queues = {"hello-java-queue"}) // 可以放在方法上也可以放在类上，监听那些对列
    @RabbitHandler // 标在方法上（重载区份不同的消息）
    public void receiveMessage(Message message, OrderReturnReasonEntity content, Channel channel) {
        byte[] body = message.getBody();
        MessageProperties messageProperties = message.getMessageProperties();
        // channel内按顺序自增
        long deliveryTag = messageProperties.getDeliveryTag();
        System.out.println("deliveryTag==>" + deliveryTag);
        System.out.println("接受到消息……内容：" + message + "==>类型：" + content);
        // 签收货物，非批量模式
        try {
            if (deliveryTag % 2 == 0) {
                channel.basicAck(deliveryTag, false);
                System.out.println("签收了货物……" + deliveryTag);
            } else {
                // 退货 requeue = false 丢弃， true 发回服务器，重新入队
                channel.basicNack(deliveryTag, false, false);
                System.out.println("没有签收货物……" + deliveryTag);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RabbitHandler
    public void receiveMessage2(OrderEntity content) {
        System.out.println("接受到消息……" + content);
    }
}