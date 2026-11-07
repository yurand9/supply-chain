-- ============================================
-- b2b-supply-chain 建表脚本
-- 程序启动时自动执行（spring.sql.init.mode=always）
-- 使用 CREATE TABLE IF NOT EXISTS，可重复执行、不会重复建表报错
-- ============================================

-- 订单表
CREATE TABLE IF NOT EXISTS biz_order (
    id            BIGSERIAL PRIMARY KEY,
    order_no      VARCHAR(64)  NOT NULL UNIQUE,
    request_id    VARCHAR(64),
    customer_code VARCHAR(64),
    customer_name VARCHAR(128),
    sku_code      VARCHAR(64),
    quantity      INTEGER,
    status        SMALLINT DEFAULT 0,
    raw_payload   TEXT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 本地消息表（存证 + 失败重试）
CREATE TABLE IF NOT EXISTS local_message (
    id              BIGSERIAL PRIMARY KEY,
    message_id      VARCHAR(64) NOT NULL UNIQUE,
    biz_type        VARCHAR(32),
    biz_id          VARCHAR(64),
    payload         TEXT,
    status          SMALLINT DEFAULT 0,
    retry_count     INTEGER DEFAULT 0,
    next_retry_time TIMESTAMP,
    error_msg       TEXT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- WMS 订单表（模拟下游 WMS 系统接收订单后落库）
CREATE TABLE IF NOT EXISTS wms_order (
    id            BIGSERIAL PRIMARY KEY,
    order_no      VARCHAR(64) NOT NULL UNIQUE,
    customer_code VARCHAR(64),
    customer_name VARCHAR(128),
    sku_code      VARCHAR(64),
    quantity      INTEGER,
    status        SMALLINT DEFAULT 0,
    receive_time  TIMESTAMP,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
