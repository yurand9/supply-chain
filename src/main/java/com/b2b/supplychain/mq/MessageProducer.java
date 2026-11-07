package com.b2b.supplychain.mq;

import com.b2b.supplychain.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 消息生产者：把消息发送到 RabbitMQ 交换机。
 */
@Component
@RequiredArgsConstructor
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送订单消息。
     *
     * @param payload 消息内容（JSON 字符串）
     */
    public void send(String payload) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_ROUTING_KEY, payload);
    }
}
