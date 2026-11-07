package com.b2b.supplychain.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：定义交换机、队列和绑定关系。
 * 订单消息会经过这个交换机投递到队列，由下游（WMS）消费。
 */
@Configuration
public class RabbitMQConfig {

    /** 交换机名 */
    public static final String ORDER_EXCHANGE = "b2b.order.exchange";

    /** 队列名 */
    public static final String ORDER_QUEUE = "b2b.order.queue";

    /** 路由键 */
    public static final String ORDER_ROUTING_KEY = "b2b.order.routingKey";

    /** 交换机（直连型，最简单） */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    /** 队列（持久化） */
    @Bean
    public Queue orderQueue() {
        return new Queue(ORDER_QUEUE, true);
    }

    /** 绑定：把队列绑到交换机上，并指定路由键 */
    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue()).to(orderExchange()).with(ORDER_ROUTING_KEY);
    }
}
