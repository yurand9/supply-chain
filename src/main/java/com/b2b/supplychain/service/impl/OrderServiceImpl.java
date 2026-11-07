package com.b2b.supplychain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.b2b.supplychain.common.BusinessException;
import com.b2b.supplychain.common.enums.MessageStatus;
import com.b2b.supplychain.common.enums.OrderStatus;
import com.b2b.supplychain.dto.OrderCreateRequest;
import com.b2b.supplychain.dto.OrderCreateResponse;
import com.b2b.supplychain.entity.BizOrder;
import com.b2b.supplychain.entity.LocalMessage;
import com.b2b.supplychain.mapper.BizOrderMapper;
import com.b2b.supplychain.mapper.LocalMessageMapper;
import com.b2b.supplychain.mq.MessageProducer;
import com.b2b.supplychain.service.OrderService;
import com.b2b.supplychain.util.RedisLockUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 订单业务实现：核心链路。
 * 流程：① 加锁防重 → ② 查重 → ③ 写订单 → ④ 写本地消息 → ⑤ 发 MQ → ⑥ 更新消息状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String LOCK_KEY_PREFIX = "lock:order:";
    private static final String BIZ_TYPE_ORDER = "ORDER";
    private static final long LOCK_EXPIRE_SECONDS = 10;

    private final BizOrderMapper bizOrderMapper;
    private final LocalMessageMapper localMessageMapper;
    private final RedisLockUtil redisLockUtil;
    private final MessageProducer messageProducer;
    private final ObjectMapper objectMapper;

    @Override
    public OrderCreateResponse createOrder(OrderCreateRequest request) {
        String orderNo = request.getOrderNo();
        String lockKey = LOCK_KEY_PREFIX + orderNo;
        String lockValue = UUID.randomUUID().toString();

        // ① 加锁（防客户网络卡顿导致的重复下单）
        boolean locked = redisLockUtil.tryLock(lockKey, lockValue, LOCK_EXPIRE_SECONDS);
        if (!locked) {
            throw new BusinessException("订单正在处理中，请勿重复提交");
        }

        try {
            return doCreateOrder(request, orderNo);
        } finally {
            // 无论如何都释放锁
            redisLockUtil.unlock(lockKey, lockValue);
        }
    }

    private OrderCreateResponse doCreateOrder(OrderCreateRequest request, String orderNo) {
        // ② 查重：订单号是否已存在
        Long count = bizOrderMapper.selectCount(
                new LambdaQueryWrapper<BizOrder>().eq(BizOrder::getOrderNo, orderNo));
        if (count != null && count > 0) {
            throw new BusinessException("订单号已存在，请勿重复下单：" + orderNo);
        }

        // 原始报文 / 消息体（这里统一用请求的 JSON 作为存证内容）
        String payload = toJson(request);

        // ③ 写 biz_order（订单落库）
        BizOrder order = new BizOrder();
        order.setOrderNo(orderNo);
        order.setRequestId(UUID.randomUUID().toString());
        order.setCustomerCode(request.getCustomerCode());
        order.setCustomerName(request.getCustomerName());
        order.setSkuCode(request.getSkuCode());
        order.setQuantity(request.getQuantity());
        order.setStatus(OrderStatus.RECEIVED.getCode());
        order.setRawPayload(payload);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        bizOrderMapper.insert(order);

        // ④ 写 local_message（本地消息存证，保证数据不丢）
        LocalMessage message = new LocalMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setBizType(BIZ_TYPE_ORDER);
        message.setBizId(orderNo);
        message.setPayload(payload);
        message.setStatus(MessageStatus.PENDING.getCode());
        message.setRetryCount(0);
        message.setNextRetryTime(LocalDateTime.now());
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        localMessageMapper.insert(message);

        // ⑤ 发 RabbitMQ 消息 + ⑥ 更新消息状态
        try {
            messageProducer.send(payload);
            // 发送成功
            message.setStatus(MessageStatus.SUCCESS.getCode());
            message.setUpdateTime(LocalDateTime.now());
            localMessageMapper.updateById(message);
        } catch (Exception e) {
            // 发送失败：记录错误，交给阶段 5 的定时任务兜底重试
            log.error("发送消息失败，订单号：{}", orderNo, e);
            message.setStatus(MessageStatus.FAILED.getCode());
            message.setErrorMsg(e.getMessage());
            message.setNextRetryTime(LocalDateTime.now().plusSeconds(5));
            message.setUpdateTime(LocalDateTime.now());
            localMessageMapper.updateById(message);
            // 不抛异常：订单已成功落库，消息由定时任务重试
        }

        OrderCreateResponse response = new OrderCreateResponse();
        response.setOrderNo(orderNo);
        return response;
    }

    @Override
    public BizOrder getByOrderNo(String orderNo) {
        BizOrder order = bizOrderMapper.selectOne(
                new LambdaQueryWrapper<BizOrder>().eq(BizOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException("订单不存在：" + orderNo);
        }
        return order;
    }

    @Override
    public Page<BizOrder> listOrders(long pageNum, long pageSize, String customerCode) {
        Page<BizOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BizOrder> wrapper = new LambdaQueryWrapper<>();
        if (customerCode != null && !customerCode.isBlank()) {
            wrapper.eq(BizOrder::getCustomerCode, customerCode);
        }
        wrapper.orderByDesc(BizOrder::getId);
        return bizOrderMapper.selectPage(page, wrapper);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new BusinessException("序列化订单信息失败");
        }
    }
}
