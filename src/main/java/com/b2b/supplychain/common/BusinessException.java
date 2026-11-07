package com.b2b.supplychain.common;

import lombok.Getter;

/**
 * 业务异常。
 * 业务逻辑出错时抛出，后续由全局异常处理器统一转成 Result 返回。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 状态码 */
    private final int code;

    public BusinessException(String message) {
        this(500, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
