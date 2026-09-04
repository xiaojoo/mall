package com.mall.order.config;

import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class MyRabbitConfig {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 定制RabbitTemplate
     */
    @PostConstruct // MyRabbitConfig对象创建完成之后，执行这个方法
    public void initRabbitTemplate() {
        // 设置确认回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             * @param correlationData 当前消息的唯一关联数据，这个是消息的唯一id
             * @param ack             消息是否成功收到
             * @param cause           时报的原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                System.out.println("confirm……correlationData=" + correlationData + ", ack=" + ack + ", cause=" + cause);
            }
        });

        // 设置消息抵达队列的确认回调
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
            /**
             * ReturnedMessage
             * message        投递失败的消息详细信息
             * replyCode      回复的状态码
             * replyText      回复的文本内容
             * exchange       当时这个消息发给哪个交换机
             * routingKey     当时这个消息用哪个路邮键
             */
            @Override
            public void returnedMessage(ReturnedMessage message) {
                System.out.println("Fail Message[" + message.getMessage() + "]==>replyCode[" + message.getReplyCode() + "]" +
                        "==>replyText[" + message.getReplyText() + "]==>exchange[" + message.getExchange() + "]==>routingKey[" + message.getRoutingKey() + "]");
            }
        });
    }
}