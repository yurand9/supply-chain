package com.b2b.supplychain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 下单入参。
 */
@Data
public class OrderCreateRequest {

    @Schema(description = "订单号（唯一）", example = "SO20261107001")
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @Schema(description = "客户编码", example = "CUST001")
    private String customerCode;

    @Schema(description = "客户名称", example = "富士康科技")
    private String customerName;

    @Schema(description = "商品编码", example = "SKU-A-100")
    private String skuCode;

    @Schema(description = "订货数量", example = "100")
    @NotNull(message = "订货数量不能为空")
    private Integer quantity;
}
