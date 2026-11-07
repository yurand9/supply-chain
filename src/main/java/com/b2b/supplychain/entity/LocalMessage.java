package com.b2b.supplychain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息实体，对应 local_message 表（存证 + 失败重试）。
 */
@Data
@TableName("local_message")
public class LocalMessage {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息唯一 ID */
    private String messageId;

    /** 业务类型，如 ORDER */
    private String bizType;

    /** 关联业务 ID（订单号） */
    private String bizId;

    /** 消息内容（发给下游的 JSON） */
    private String payload;

    /** 状态：0=待发 1=成功 2=失败 */
    private Integer status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 下次重试时间 */
    private LocalDateTime nextRetryTime;

    /** 失败原因 */
    private String errorMsg;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
