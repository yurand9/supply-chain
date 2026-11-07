package com.b2b.supplychain.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.b2b.supplychain.common.Result;
import com.b2b.supplychain.dto.OrderCreateRequest;
import com.b2b.supplychain.dto.OrderCreateResponse;
import com.b2b.supplychain.entity.BizOrder;
import com.b2b.supplychain.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口。
 */
@Tag(name = "订单接口")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单（防重接单）。
     */
    @Operation(summary = "创建订单（防重接单）")
    @PostMapping("/create")
    public Result<OrderCreateResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        return Result.success(orderService.createOrder(request));
    }

    /**
     * 按订单号查询单个订单。
     */
    @Operation(summary = "按订单号查询订单")
    @GetMapping("/{orderNo}")
    public Result<BizOrder> getByOrderNo(@PathVariable String orderNo) {
        return Result.success(orderService.getByOrderNo(orderNo));
    }

    /**
     * 分页查询订单列表（可按客户编码筛选）。
     */
    @Operation(summary = "分页查询订单列表")
    @GetMapping("/list")
    public Result<Page<BizOrder>> listOrders(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String customerCode) {
        return Result.success(orderService.listOrders(pageNum, pageSize, customerCode));
    }
}
