package com.b2b.supplychain.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.b2b.supplychain.config.RabbitMQConfig;
import com.b2b.supplychain.dto.OrderCreateRequest;
import com.b2b.supplychain.entity.WmsOrder;
import com.b2b.supplychain.mapper.WmsOrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 消息消费者：模拟下游 WMS 系统接收订单消息，并真正落库到 wms_order 表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final WmsOrderMapper wmsOrderMapper;
    private final ObjectMapper objectMapper;

    /**
     * 监听订单队列，收到消息后解析并写入 WMS 自己的表。
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void onOrderMessage(String payload) {
        log.info("【WMS】收到订单消息：{}", payload);
        try {
            // 1. 解析消息 JSON 为订单对象
            OrderCreateRequest order = objectMapper.readValue(payload, OrderCreateRequest.class);

            // 2. 幂等判断：如果该订单已入库，跳过（防止重复消息导致重复入库）
            Long count = wmsOrderMapper.selectCount(
                    new LambdaQueryWrapper<WmsOrder>().eq(WmsOrder::getOrderNo, order.getOrderNo()));
            if (count != null && count > 0) {
                log.warn("【WMS】订单已存在，跳过：{}", order.getOrderNo());
                return;
            }

            // 3. 真正落库到 WMS 自己的表
            WmsOrder wmsOrder = new WmsOrder();
            wmsOrder.setOrderNo(order.getOrderNo());
            wmsOrder.setCustomerCode(order.getCustomerCode());
            wmsOrder.setCustomerName(order.getCustomerName());
            wmsOrder.setSkuCode(order.getSkuCode());
            wmsOrder.setQuantity(order.getQuantity());
            wmsOrder.setStatus(0); // 0=已接收
            wmsOrder.setReceiveTime(LocalDateTime.now());
            wmsOrder.setCreateTime(LocalDateTime.now());
            wmsOrderMapper.insert(wmsOrder);

            log.info("【WMS】订单已入库：{}", order.getOrderNo());
        } catch (Exception e) {
            log.error("【WMS】处理消息失败：{}", payload, e);
            // 这里不抛出异常，避免消息无限重投；真实场景可走死信队列
        }
    }
}
