package com.b2b.supplychain.common.enums;

import lombok.Getter;

/**
 * 本地消息状态枚举。
 */
@Getter
public enum MessageStatus {

    PENDING(0, "待发送"),
    SUCCESS(1, "发送成功"),
    FAILED(2, "发送失败"),
    DEAD(3, "最终失败（放弃重试）");

    private final int code;
    private final String desc;

    MessageStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
