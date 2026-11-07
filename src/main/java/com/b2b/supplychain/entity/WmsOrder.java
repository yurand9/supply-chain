package com.b2b.supplychain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WMS 订单实体，对应 wms_order 表（模拟下游 WMS 系统接单后落库）。
 */
@Data
@TableName("wms_order")
public class WmsOrder {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号（唯一） */
    private String orderNo;

    /** 客户编码 */
    private String customerCode;

    /** 客户名称 */
    private String customerName;

    /** 商品编码 */
    private String skuCode;

    /** 订货数量 */
    private Integer quantity;

    /** 处理状态：0=已接收 1=已处理 */
    private Integer status;

    /** WMS 接收时间 */
    private LocalDateTime receiveTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
