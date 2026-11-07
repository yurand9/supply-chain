package com.b2b.supplychain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 下单返回。
 */
@Data
public class OrderCreateResponse {

    @Schema(description = "订单号")
    private String orderNo;
}
