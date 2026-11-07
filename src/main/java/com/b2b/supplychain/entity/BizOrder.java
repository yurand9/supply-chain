package com.b2b.supplychain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单实体，对应 biz_order 表。
 */
@Data
@TableName("biz_order")
public class BizOrder {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号（唯一，防重关键） */
    private String orderNo;

    /** 请求唯一标识（链路追踪） */
    private String requestId;

    /** 客户编码 */
    private String customerCode;

    /** 客户名称 */
    private String customerName;

    /** 商品编码 */
    private String skuCode;

    /** 订货数量 */
    private Integer quantity;

    /** 订单状态：0=已接收 1=已通知下游 */
    private Integer status;

    /** 客户原始报文（存证） */
    private String rawPayload;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
