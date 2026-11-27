# 模块 4：秒杀（Redis + Kafka）

> 目标：后台配置限时秒杀课程，学员到点抢购；Redis 原子预扣防止超卖，Kafka 异步建单削峰，结果轮询反馈，超时/取消自动回补库存。

## 1. 数据模型

| 表 | 关键字段 | 说明 |
|---|---|---|
| `seckill_activity` | `course_id`、`seckill_price`、`total_stock`、`available_stock`、`per_user_limit`、`start_at`、`end_at`、`status` | 活动库存以 DB `available_stock` 为最终账本；Redis 是并发闸门 |
| `seckill_record` | `user_id`、`activity_id`、`order_no`、`status`、`reason` | `UNIQUE(user_id, activity_id)` 兜底限购与消费幂等 |

抢购记录状态：`CREATED`（已建待支付订单）→ `PAID`；`CREATED` → `CANCELED`（取消/超时，名额释放）；建单校验失败写 `FAILED`（带原因）。接口层轮询还会返回 `QUEUED`（Redis 已占名额、Kafka 尚未消费完）和 `NOT_JOINED`。

**每人限购说明**：计划中的 `UNIQUE(user_id, activity_id)` 与「每人限购 1 份」一致，本期后台固定 `per_user_limit = 1`；字段保留，后续如需多份需调整唯一约束与记录模型。

时间字段与全项目一致：DB 存 UTC，Java 使用 `LocalDateTime.now(ZoneOffset.UTC)`；前端展示转本地时区。

## 2. Redis 设计

| Key | 类型 | 内容 |
|---|---|---|
| `seckill:stock:{activityId}` | String | 可抢库存，活动创建/上线/启动时从 DB `available_stock` 预热 |
| `seckill:users:{activityId}` | Hash | `userId -> 已抢数量`，实现限购与结果轮询判定 |

抢购 Lua 脚本原子完成：**校验活动时间 → 读取用户已抢数 → 库存判断 → 扣库存 → 用户计数 +1**，返回码：

| 返回值 | 含义 | 接口错误码 |
|---|---|---|
| `>= 0` | 成功，返回剩余库存 | 返回 `QUEUED` |
| `-1` | 未开始 | 70002 |
| `-2` | 已结束 | 70003 |
| `-3` | 超过限购 | 70005 |
| `-4` | 已抢光 | 70004 |
| `-5` | 未预热 | 70006 |

回补：
- 消息发送明确失败 / 消费者建单失败：库存 +1 **且**撤销用户限购计数（允许重试）。
- 订单取消/超时：库存 +1，保留用户限购计数（避免反复抢购占位）。

## 3. Kafka 设计

| 项 | 值 |
|---|---|
| Topic | `seckill-order`（3 分区，key = `activityId`，保证同活动有序） |
| 死信 Topic | `seckill-order-dlq` |
| 消费者组 | `seckill-order-consumer` |
| 生产 | `acks=all` + `enable.idempotence=true`；同步等待发送结果，明确失败才回补 Redis |
| 消费 | 手动 ack（`MANUAL_IMMEDIATE`）；重试 3 次（间隔 1s）后进入死信队列 |

建单流程（`SeckillOrderProcessor`，单事务）：
1. `seckill_record` 已存在 → 幂等跳过（重复消息直接 ack）。
2. 课程已上架、用户未拥有课程、无同课程待支付订单。
3. `UPDATE seckill_activity SET available_stock = available_stock - 1 WHERE available_stock > 0` 原子扣 DB 库存；失败则写 `FAILED` 记录并回补 Redis。
4. 创建 `orders(order_source='SECKILL', seckill_activity_id, 15 分钟支付窗口)`、`order_item`、`seckill_record(CREATED)`。

任一步异常回滚事务，Kafka 重试；超过重试次数进入死信队列（需人工介入/对账）。

## 4. 交易联动

- 支付成功：`OrderPaidEvent` → 秒杀记录 `CREATED → PAID`（库存已在建单时扣减）。
- 用户取消 / 定时任务超时关单：`OrderCanceledEvent` → 记录 `CANCELED`，DB 库存 +1，事务提交后 Redis 库存 +1。
- 退款通过不回补名额（视为已消费）。
- 事件使用 `@TransactionalEventListener(AFTER_COMMIT)` + `REQUIRES_NEW` 处理，交易主链路不受秒杀异常影响。

## 5. 接口

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/v1/seckill/activities` | 公开 | 秒杀专区列表（含 Redis 剩余库存、服务端时间） |
| POST | `/api/v1/seckill/activities/{id}/claim` | 登录 | Redis 预扣 + Kafka 入队，返回 `QUEUED` |
| GET | `/api/v1/seckill/activities/{id}/result` | 登录 | 轮询结果：`NOT_JOINED/QUEUED/CREATED/PAID/CANCELED/FAILED` |
| GET | `/api/v1/admin/seckill/activities` | `seckill:browse` | 后台分页 |
| POST | `/api/v1/admin/seckill/activities` | `seckill:create` | 新建并预热 Redis |
| PUT | `/api/v1/admin/seckill/activities/{id}` | `seckill:edit` | 编辑，按名额差量同步 Redis |
| PUT | `/api/v1/admin/seckill/activities/{id}/status` | `seckill:edit` | 上/下线（下线清 Redis 库存键） |
| POST | `/api/v1/admin/seckill/activities/{id}/reconcile` | `seckill:edit` | 对账：按有效记录重算 DB 库存并覆盖 Redis |
| GET | `/api/v1/admin/seckill/activities/{id}/records` | `seckill:browse` | 抢购记录 |
| DELETE | `/api/v1/admin/seckill/activities/{id}` | `seckill:delete` | 无抢购记录时删除 |

## 6. 启动与降级

```bash
docker compose up -d kafka        # Kafka 4.x KRaft 单节点，端口 9092
./mvnw spring-boot:run            # 启动时自动建 Topic 并预热未结束活动库存
```

- Redis 不可用：抢购返回 70006，活动列表回退 DB 库存；普通下单/支付不受影响。
- Kafka 不可用：生产者同步发送失败后回补 Redis 并提示稍后再试；消费者在后台重试连接，不阻塞应用启动。
- 需要临时关闭消费者时设置 `SECKILL_CONSUMER_ENABLED=false`。

## 7. 验收

```bash
node scripts/seckill-smoke.mjs       # 活动创建/预热 → 抢购 → 轮询建单 → 支付 → 记录 PAID
node scripts/seckill-concurrency.mjs # 20 用户 × 10 并发抢 10 个名额：恰好 10 单、不超卖、无重复
# 清理测试数据（含临时用户、秒杀订单/记录并回补库存）
sqlcmd -S localhost -C -E -b -I -f 65001 -d smart_learning -i scripts/reset-seckill-data.sql
```

> sqlcmd 读取含中文的脚本必须加 `-f 65001`；否则会按本地代码页解码，导致整批 SQL 不执行且不报错。

并发脚本会通过后台接口创建临时用户与活动，结束后清理；也可在 `smart_learning` 库直接核对：

```sql
SELECT id, total_stock, available_stock FROM seckill_activity;
SELECT activity_id, status, COUNT(*) FROM seckill_record GROUP BY activity_id, status;
SELECT COUNT(*) FROM orders WHERE order_source = 'SECKILL' AND seckill_activity_id = ?;
```

对账口径：`available_stock + COUNT(seckill_record WHERE status IN ('CREATED','PAID')) = total_stock`；Redis 库存应与 DB 剩余一致（进行中活动允许存在尚未消费的短暂差值）。
