# 数据库读写分离

## 1. 拓扑

| 角色 | 数据库 | 账号 | 用途 |
|---|---|---|---|
| 主库 | `smart_learning` | `smart_learning_app`（db_owner） | 写操作、强一致读、Flyway |
| 从库 | `smart_learning_read` | `smart_learning_read`（db_datareader） | 普通列表/详情/统计 |

- 生产：SQL Server Always On 只读副本或事务复制。
- 开发：同一 SQL Server 实例的两个库；用 `scripts/sync-read-db.sql` 做主库→从库备份还原；也可配置事务复制。
- Flyway 只绑定 `masterDataSource`，从库不执行迁移。

## 2. 代码实现

- `DataSourceProperties`：`app.datasource.master.*` / `app.datasource.read.*` 两套 Hikari 配置。
- `ReadWriteDataSourceConfig`：
  - 创建 `masterDataSource`、`readDataSource`；
  - `RoutingDataSource` 按 `MASTER/READ` 选择；
  - 外层包 `LazyConnectionDataSourceProxy`，确保物理连接在路由决定后才获取；
  - `@Primary DataSource` 交给 JPA / 事务管理器使用；
  - `Flyway` 显式使用 `masterDataSource`。
- `RoutingDataSource.determineCurrentLookupKey()`：
  1. `@Master` / `@ReadOnly` 切面设置的 ThreadLocal 优先；
  2. 当前事务 `readOnly=true` 走 READ；
  3. 默认 MASTER。
- `DataSourceRoutingAspect` 优先级最高，先于事务拦截器执行。

## 3. 路由规则

| 场景 | 路由 | 实现 |
|---|---|---|
| 写操作（增删改、支付、退款） | MASTER | 默认；`@Transactional` |
| 强一致读（登录、权限加载、订单状态、秒杀结果） | MASTER | `@Master` 或非只读事务 |
| 普通列表/详情/统计 | READ | `@ReadOnly` + `@Transactional(readOnly=true)` |
| 前台课程/讲师/资讯/资料/Banner 列表 | READ | `CoursePublicService`、`ContentPublicService` 的只读方法 |
| 前台课程可领优惠券 | READ | `CouponUserService.availableForCourse/availableForSubject` |
| 文章详情（阅读数 +1） | MASTER | `ContentPublicService.articleDetail` |
| 后台课程/内容管理 | MASTER | 管理端 Service 统一 `@Master` |
| 我的订单/优惠券/课程、购买状态 | MASTER | `OrderService`、`CouponUserService`、`EnrollmentService` 固定主库 |
| 后台订单/退款/优惠券管理 | MASTER | `OrderAdminService`、`CouponAdminService` 固定主库 |
| 院校库检索/详情/精选 | MASTER | `UniversityPublicService` 固定主库，后台院校/分数线修改前台即时可见 |
| 后台数据看板 | MASTER | `StatsService` 固定主库，保证看板与订单/用户管理实时一致 |
| 交易定时任务（超时关单/券过期/自动完成） | MASTER | `TradeScheduler` 薄壳调用独立事务 Bean `TradeSweepService`，确保代理与路由生效 |
| 写事务内嵌套查询 | MASTER | 同一事务不切库 |
| 只读事务内写 | 禁止 | 代码规范 |

## 4. 一致性与降级

- 复制延迟内普通列表可能读到旧数据，页面允许最终一致。
- 登录、权限、支付、退款、秒杀结果强制主库，避免越权或状态错误。
- 交易强一致边界：支付成功后立即查询订单/我的课程、退款审核结果、优惠券核销状态全部走主库；前台课程详情里的「可领优惠券」允许从库最终一致。
- 定时任务由 `@Scheduled` 薄壳触发独立 `@Master @Transactional` Bean，避免调度线程直接调用自身事务方法绕过代理而落到从库。
- 从库故障：`RoutingDataSource` 捕获 READ 连接异常后自动回退主库并打 WARN 日志，业务不中断。
- 本版本不做主库故障自动切换。
- 时间字段统一以 UTC 的 `LocalDateTime` 直接写入 `DATETIME2`，不使用 `hibernate.jdbc.time_zone`，保证 Java 时间与 SQL `SYSUTCDATETIME()` 在同一基准上比较。

## 5. 验证

```bash
# 登录拿 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"account":"admin","password":"Admin@123"}' | jq -r .data.accessToken)

curl -s http://localhost:8080/api/v1/system/route-check/read -H "Authorization: Bearer $TOKEN"
# {"route":"READ","database":"smart_learning_read"}

curl -s http://localhost:8080/api/v1/system/route-check/master -H "Authorization: Bearer $TOKEN"
# {"route":"MASTER","database":"smart_learning"}
```

## 6. 开发环境同步从库

```bash
# 停掉应用后执行，避免连接占用
sqlcmd -S localhost -E -C -Q "BACKUP DATABASE smart_learning TO DISK=N'C:\temp\smart_learning.bak' WITH INIT"
sqlcmd -S localhost -E -C -Q "RESTORE DATABASE smart_learning_read FROM DISK=N'C:\temp\smart_learning.bak' WITH REPLACE, MOVE N'smart_learning' TO N'C:\...\smart_learning_read.mdf', MOVE N'smart_learning_log' TO N'C:\...\smart_learning_read_log.ldf'"
```

还原后从库需要重新授权：

```sql
USE smart_learning_read;
CREATE USER smart_learning_read FOR LOGIN smart_learning_read;
ALTER ROLE db_datareader ADD MEMBER smart_learning_read;
```
