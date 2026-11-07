package com.b2b.supplychain.common;

import lombok.Data;

/**
 * 统一返回结果。
 * 所有接口都返回这个结构，方便前端和测试统一处理。
 */
@Data
public class Result<T> {

    /** 状态码：200 成功，其他为失败 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 数据 */
    private T data;

    /** 成功（带数据） */
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    /** 成功（不带数据） */
    public static <T> Result<T> success() {
        return success(null);
    }

    /** 失败（自定义状态码 + 提示） */
    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }

    /** 失败（默认 500） */
    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }
}
