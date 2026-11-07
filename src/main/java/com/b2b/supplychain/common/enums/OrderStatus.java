package com.b2b.supplychain.common.enums;

import lombok.Getter;

/**
 * 订单状态枚举。
 */
@Getter
public enum OrderStatus {

    RECEIVED(0, "已接收"),
    NOTIFIED(1, "已通知下游");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
