package com.b2b.supplychain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.b2b.supplychain.dto.OrderCreateRequest;
import com.b2b.supplychain.dto.OrderCreateResponse;
import com.b2b.supplychain.entity.BizOrder;

/**
 * 订单业务接口。
 */
public interface OrderService {

    /**
     * 创建订单：防重接单 + 落库 + 存证 + 发消息。
     */
    OrderCreateResponse createOrder(OrderCreateRequest request);

    /**
     * 按订单号查询单个订单。
     */
    BizOrder getByOrderNo(String orderNo);

    /**
     * 分页查询订单列表（可按客户编码筛选）。
     */
    Page<BizOrder> listOrders(long pageNum, long pageSize, String customerCode);
}
