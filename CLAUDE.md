# CLAUDE.md — b2b-supply-chain 项目地图与工作说明

> 本文件是项目的「地图 + 使用说明书」。任何开发工作前，请先阅读本文件。
> 每个开发阶段完成后，都要**追加/更新**本文件，保持它是最新、最准确的项目指南。

---

## 1. 项目定位

企业级 B2B 数据交换网关与供应链协同平台，用于模拟制造业（如工业富联）与外部 B2B 客户、
内部 WMS/ERP 系统之间的数据对接场景。

核心业务链路：**接单 → 防重 → 落库存证 → 异步通知下游 → 失败自动重试**。

---

## 2. 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.3.x |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | PostgreSQL（本机 Docker，端口 **5433**，库名 `b2b_db`） |
| 缓存/锁 | Redis（本机 Docker，端口 6379） |
| 消息队列 | RabbitMQ（本机 Docker，端口 5672） |
| 接口文档 | Knife4j 4.5.0（页面地址 `http://localhost:8080/doc.html`） |
| 其他 | Lombok |

---

## 3. 标准目录结构（标准文件路径）

```
src/main/java/com/b2b/supplychain/
├── B2bSupplyChainApplication.java   启动类（入口）
├── controller/                      接口层：接收 HTTP 请求
│   └── OrderController.java         下单接口
├── service/                         业务逻辑层（接口定义）
│   └── OrderService.java
├── service/impl/                    业务逻辑层（实现）
│   └── OrderServiceImpl.java
├── mapper/                          MyBatis-Plus 数据访问层
│   ├── BizOrderMapper.java
│   ├── LocalMessageMapper.java
│   └── WmsOrderMapper.java
├── entity/                          实体类（对应数据库表）
│   ├── BizOrder.java
│   ├── LocalMessage.java
│   └── WmsOrder.java
├── dto/                             接口出入参对象
│   ├── OrderCreateRequest.java
│   └── OrderCreateResponse.java
├── config/                          配置类
│   ├── RedisConfig.java             Redis 序列化配置
│   ├── RabbitMQConfig.java          交换机/队列/绑定配置
│   ├── Knife4jConfig.java           接口文档配置
│   └── MybatisPlusConfig.java       分页插件配置
├── common/                          通用返回 / 异常 / 枚举
│   ├── Result.java                  统一返回结果
│   ├── BusinessException.java       业务异常
│   └── enums/                       枚举（订单状态、消息状态等）
├── mq/                              消息队列
│   ├── MessageProducer.java         生产者（发消息）
│   └── MessageConsumer.java         消费者（模拟 WMS 收消息）
├── task/                            定时任务
│   └── MessageRetryTask.java        失败消息重试兜底
└── util/                            工具类
    └── RedisLockUtil.java           分布式锁工具

src/main/resources/
├── application.yml                  主配置（端口/日志等非敏感项，激活 local profile）
├── application-local.yml            本地敏感配置（真实密码，已被 .gitignore 忽略）
├── application-local-example.yml    本地配置脱敏模板（复制后填真实密码）
├── schema.sql                       建表脚本（程序启动时自动执行）
└── (mapper XML，如需要)
```

> 项目根目录另有两个关键文件：`pom.xml`（依赖清单）、`docker-compose.yml`（Docker 基础设施：Postgres/Redis/RabbitMQ 一键启动）。

---

## 4. 端口约定

| 服务 | 端口 |
|------|------|
| 应用服务 | 8080 |
| PostgreSQL | **5433** |
| Redis | 6379 |
| RabbitMQ | 5672（管理界面 15672） |
| Knife4j 文档 | `http://localhost:8080/doc.html` |

> Docker 容器由项目根目录 `docker-compose.yml` 统一管理，容器名：`b2b-postgres` / `b2b-redis` / `b2b-rabbitmq`。
> 启动：`docker compose up -d`；停止：`docker compose down`。

---

## 5. 数据库表

> 库名：`b2b_db`，账号 `postgres` / `postgres`，端口 5433。表均建在默认 `public` 模式下。
> 建表方式：程序启动时自动执行 `src/main/resources/schema.sql`（用 `CREATE TABLE IF NOT EXISTS`，可重复执行、不会重复建表报错）。

### `biz_order`（订单表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL 主键 | 自增主键 |
| order_no | VARCHAR(64) 唯一 | 订单号（防重关键） |
| request_id | VARCHAR(64) | 请求唯一标识（链路追踪） |
| customer_code | VARCHAR(64) | 客户编码 |
| customer_name | VARCHAR(128) | 客户名称 |
| sku_code | VARCHAR(64) | 商品编码 |
| quantity | INTEGER | 订货数量 |
| status | SMALLINT 默认 0 | 0=已接收 1=已通知下游 |
| raw_payload | TEXT | 客户原始报文（存证） |
| create_time | TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | 更新时间 |

### `local_message`（本地消息表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL 主键 | 自增主键 |
| message_id | VARCHAR(64) 唯一 | 消息唯一 ID |
| biz_type | VARCHAR(32) | 业务类型，如 ORDER |
| biz_id | VARCHAR(64) | 关联业务 ID（订单号） |
| payload | TEXT | 消息内容（发给下游的 JSON） |
| status | SMALLINT 默认 0 | 0=待发 1=成功 2=失败 3=最终失败 |
| retry_count | INTEGER 默认 0 | 已重试次数 |
| next_retry_time | TIMESTAMP | 下次重试时间 |
| error_msg | TEXT | 失败原因 |
| create_time | TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | 更新时间 |

### `wms_order`（WMS 订单表，模拟下游 WMS 落库）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL 主键 | 自增主键 |
| order_no | VARCHAR(64) 唯一 | 订单号（唯一） |
| customer_code | VARCHAR(64) | 客户编码 |
| customer_name | VARCHAR(128) | 客户名称 |
| sku_code | VARCHAR(64) | 商品编码 |
| quantity | INTEGER | 订货数量 |
| status | SMALLINT 默认 0 | 0=已接收 1=已处理 |
| receive_time | TIMESTAMP | WMS 接收时间 |
| create_time | TIMESTAMP | 创建时间 |

---

## 6. 命名规范

- 接口层：`XxxController`
- 业务层：接口 `XxxService`，实现 `XxxServiceImpl`（放 `service/impl` 包）
- 数据访问层：`XxxMapper`
- 实体类：与表名对应（驼峰），如 `BizOrder` 对应 `biz_order`
- 出入参对象：`XxxRequest` / `XxxResponse`
- 配置类：`XxxConfig`
- 常量/枚举：统一放 `common/enums` 或 `common/constants`

---

## 7. 协作规矩（重要）

1. **分步骤推进**：建表 → 配依赖 → 写工具类 → 写核心接口 → 写定时任务，不一次性生成大量代码。
2. **先确认再动手**：每一步动手前，先说明「这一步做什么、动哪些文件」，等用户回复「确认/开始」后再写。
3. **列清单**：一次性修改多个文件时，先列出文件清单。
4. **阶段收尾**：每写完一个阶段，停下来问用户「这部分清楚吗？跑通了吗？」，再继续下一步。
5. **本文件维护**：每个阶段完成后，追加新增文件的标准路径和一句话说明。

---

## 8. 开发进度记录

- [x] 阶段 0：创建 CLAUDE.md（本文件）
- [x] 阶段 1：建表（启动自动建表，已验证：biz_order / local_message 已建好）
- [x] 阶段 2：配置依赖（pom.xml + application.yml）
- [x] 阶段 3：写工具类和基础设施（实体/枚举/Mapper/Result/异常/Redis锁/配置类）
- [x] 阶段 4：写核心接口（防重接单 + 落库 + 存证 + 发 MQ）
- [x] 阶段 5：写定时任务（消息重试兜底）
- [x] 阶段 6：联调与验收
- [x] 扩展：订单查询接口（分页插件 + 查询接口）
- [x] 扩展：WMS 消费端真正落库（wms_order 表 + 消费者落库）
