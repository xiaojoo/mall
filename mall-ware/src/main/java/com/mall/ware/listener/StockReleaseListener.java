package com.mall.ware.listener;

import com.mall.common.to.mq.OrderTo;
import com.mall.common.to.mq.StockLockedTo;
import com.mall.ware.service.WareSkuService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import lombok.RequiredArgsConstructor;

@Slf4j
@RabbitListener(queues = "stock.release.stock.queue")
@Service
@RequiredArgsConstructor
public class StockReleaseListener {

    private final WareSkuService wareSkuService;

    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        log.info("******收到解锁库存的信息******");
        try {
            // 解锁库存
            wareSkuService.unlockStock(to);
            // 手动删除消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 解锁失败，将消息重新放回队列，继续消费
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        log.info("******收到订单关闭，准备解锁库存的信息******");
        try {
            wareSkuService.unlockStock(orderTo);
            // 手动删除消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 解锁失败 将消息重新放回队列，让别人消费
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
