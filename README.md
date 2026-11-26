# 智学考研 · 基础版

仿新东方考研的全栈项目。当前交付 **模块 1：系统底座**、**模块 2：课程与内容** 和 **模块 3：交易**，后续模块按「秒杀 → 学习 → 题库 → 院校看板」逐个实现。

## 当前已实现

- 前后端分离工程骨架（Spring Boot 4.2 + Vue 3 + TypeScript）
- SQL Server 主从双数据源读写分离（`smart_learning` / `smart_learning_read`）
- 禅道式权限分组：用户、分组、权限点、分组授权、用户分组、审计日志全部落库
- 账号密码 + 手机号验证码 + 邮箱验证码三种登录
- JWT access token + 数据库持久化 refresh token（轮换、吊销）
- 权限驱动菜单、路由守卫、按钮权限指令
- **课程与内容模块**：
  - 后台课程 CRUD、章节课时管理、上下架；讲师、文章、资料、Banner 管理
  - 前台首页、课程中心、课程详情、名师团队、考研资讯、资料下载
  - 讲师数据范围 OWN：只能看到和修改自己的课程/文章/资料
  - 本地文件上传（封面、头像、资料）与 `/uploads/**` 访问
  - 文章 HTML 服务端 Jsoup 清洗
- **交易模块**：
  - 优惠券：满减/折扣券、原子领券（并发限领）、有效期与适用科目、下单锁定、支付核销、取消/超时释放、过期流转
  - 下单：服务端重新计价、课程快照、待支付订单 30 分钟有效、同课程待支付订单去重
  - 模拟支付：`PaymentChannel` 抽象 + 支付宝/微信模拟渠道、支付流水、`order_no + transaction_no` 幂等
  - 选课开通：支付成功幂等写入 `user_course` 并累加学习人数；退款通过后撤销权限并回写
  - 售后退款：学员申请 → 管理员审核（通过/驳回）、退款流水、订单状态机集中校验
  - 个人中心：我的订单、我的优惠券、我的课程；后台：订单管理、退款审核、优惠券管理
  - 定时任务：超时关单、优惠券过期、支付满 7 天自动完成
- 后台页面：概览、权限分组、用户管理、审计日志、课程/讲师/文章/资料/Banner、订单、退款、优惠券
- Flyway 迁移与种子数据（V1–V7）

## 技术栈

- 后端：Spring Boot 4.2.0、Java 17、Spring Security 7、Spring Data JPA、Flyway、SQL Server 2025
- 前端：Vue 3、Vite、TypeScript、Element Plus、Pinia、Vue Router、Axios
- 中间件：Redis（验证码/限流）、Kafka（后续秒杀模块使用，本模块提供生产者冒烟接口）

> 时间约定：数据库 `DATETIME2` 统一存 UTC，Java 使用 `LocalDateTime.now(ZoneOffset.UTC)`；不使用 `hibernate.jdbc.time_zone`，避免 `LocalDateTime` 被二次时区换算导致 SQL 时间比较错位。前端展示时统一转为本地时间。

## 环境要求

- JDK 17
- Node.js 22+
- SQL Server 2025（本机实例 `localhost:1433`）
- Redis 7+（默认 `127.0.0.1:6379`；密码通过本地配置或环境变量提供，不提交到仓库）
- 可选：Kafka 4.x（`docker compose up -d kafka`）

## 本地私有配置

数据库密码、Redis 密码、JWT 密钥不写入仓库。复制示例文件后填写本地值：

```bash
cp application-local.yml.example application-local.yml
```

`application-local.yml` 已被 `.gitignore` 忽略，`application.yml` 通过
`spring.config.import: optional:file:./application-local.yml` 加载它。也可以使用环境变量覆盖：

```bash
DB_MASTER_PASSWORD=... DB_READ_PASSWORD=... REDIS_PASSWORD=... JWT_SECRET=... ./mvnw spring-boot:run
```

JWT 密钥需为 Base64 编码、解码后至少 32 字节，可用 `openssl rand -base64 32` 生成。

## 数据库初始化

主库 `smart_learning` 与从库 `smart_learning_read`：

```sql
CREATE DATABASE smart_learning;
CREATE DATABASE smart_learning_read;
```

应用账号（密码请替换为强密码，并通过 `application-local.yml` 或环境变量配置给应用）：

```sql
CREATE LOGIN smart_learning_app WITH PASSWORD = '<MASTER_DB_PASSWORD>', CHECK_POLICY = OFF;
CREATE LOGIN smart_learning_read WITH PASSWORD = '<READ_DB_PASSWORD>', CHECK_POLICY = OFF;

USE smart_learning;
CREATE USER smart_learning_app FOR LOGIN smart_learning_app;
ALTER ROLE db_owner ADD MEMBER smart_learning_app;

USE smart_learning_read;
CREATE USER smart_learning_read FOR LOGIN smart_learning_read;
ALTER ROLE db_datareader ADD MEMBER smart_learning_read;
```

也可以使用脚本（通过 sqlcmd 变量传入密码）：

```bash
sqlcmd -S localhost -E -C -i scripts/init-databases.sql \
  -v MASTER_PASSWORD="<MASTER_DB_PASSWORD>" READ_PASSWORD="<READ_DB_PASSWORD>"
```

首次启动时 Flyway 只对主库执行迁移（当前 V1–V7）。开发环境可把主库备份还原到从库：

```bash
sqlcmd -S localhost -E -C -Q "BACKUP DATABASE smart_learning TO DISK=N'C:\temp\smart_learning.bak' WITH INIT"
sqlcmd -S localhost -E -C -Q "RESTORE DATABASE smart_learning_read FROM DISK=N'C:\temp\smart_learning.bak' WITH REPLACE, MOVE N'smart_learning' TO N'C:\...\smart_learning_read.mdf', MOVE N'smart_learning_log' TO N'C:\...\smart_learning_read_log.ldf'"
```

生产环境请使用 SQL Server Always On 只读副本或事务复制，详见 `docs/read-write-splitting.md`。

## 启动

后端：

```bash
./mvnw spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

- 后端：http://localhost:8080
- 前端：http://localhost:5173
- Swagger：http://localhost:8080/swagger-ui/index.html
- 健康检查：http://localhost:8080/actuator/health

## 演示账号

| 账号 | 密码 | 分组 | 说明 |
|---|---|---|---|
| admin | Admin@123 | ADMIN | 全部权限，ALL 数据范围 |
| teacher | Teacher@123 | TEACHER | 管理端入口，OWN 数据范围 |
| student | Student@123 | STUDENT | 学员，SELF 数据范围 |
| teacher2 | Teacher@123 | TEACHER + STUDENT | 演示多分组权限并集 |

手机号登录演示：`13800000000`；邮箱登录演示：`student@example.com`。
开发环境（`app.auth.dev-mode=true`）验证码会输出到后端日志。

## 交易演示流程

1. `student` 登录 → 课程详情领取优惠券 → 立即报名 → 确认订单页选择优惠券 → 提交订单。
2. 模拟收银台选择「模拟支付宝 / 模拟微信」→ 确认支付 → 支付成功并开通课程。
3. 打开「我的中心」：我的订单、我的优惠券、我的课程均可见；再次进入课程详情显示已购买。
4. 我的订单 → 申请退款；`admin` 登录后台 → 退款审核 → 通过后撤销课程权限，订单变为已退款。
5. 待支付订单 30 分钟未支付会自动关闭并释放优惠券（后台每 60 秒清扫，活动库存与优惠券过期同样处理）。

自动化验收脚本（API/UI 冒烟会创建订单，建议先重置交易数据）：

```bash
sqlcmd -S localhost -C -E -d smart_learning -i scripts/reset-trade-data.sql
node scripts/trade-smoke.mjs              # 交易 API 全链路（领券→下单→支付→退款→审核）
node scripts/trade-scheduler-test.mjs     # 超时关单 + 优惠券释放/过期
node frontend/scripts/trade-ui-smoke.cjs  # 浏览器 UI 全链路（需前后端已启动）
```

`reset-trade-data.sql` 会清空订单/退款/支付/选课/用户优惠券并恢复优惠券库存与课程学习人数，不影响课程与用户数据。注意：主从库同步后需重新执行 `scripts/sync-read-db.sql` 才能让前台只读数据同步。

## 核心接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/auth/login` | 账号密码登录 |
| POST | `/api/v1/auth/sms/code` | 发送短信验证码（模拟） |
| POST | `/api/v1/auth/sms/login` | 手机号验证码登录/自动注册 |
| POST | `/api/v1/auth/email/code` | 发送邮箱验证码 |
| POST | `/api/v1/auth/email/login` | 邮箱验证码登录/自动注册 |
| POST | `/api/v1/auth/refresh` | 刷新令牌（轮换） |
| GET | `/api/v1/auth/me` | 当前用户 + 权限 + 菜单 |
| GET | `/api/v1/admin/groups` | 权限分组列表 |
| PUT | `/api/v1/admin/groups/{id}/privs` | 整组覆盖权限 |
| GET | `/api/v1/admin/users` | 用户分页/搜索 |
| PUT | `/api/v1/admin/users/{id}/groups` | 分配用户分组 |
| GET | `/api/v1/admin/users/{id}/permissions` | 有效权限预览 |
| GET | `/api/v1/admin/audit-logs` | 审计日志 |
| GET/POST/PUT/DELETE | `/api/v1/admin/courses...` | 课程 CRUD、上下架、章节课时 |
| GET/POST/PUT/DELETE | `/api/v1/admin/teachers...` | 讲师管理 |
| GET/POST/PUT/DELETE | `/api/v1/admin/articles...` | 文章管理 |
| GET/POST/PUT/DELETE | `/api/v1/admin/materials...` | 资料管理 |
| GET/POST/PUT/DELETE | `/api/v1/admin/banners...` | Banner 管理 |
| POST | `/api/v1/admin/upload` | 本地文件上传 |
| GET | `/api/v1/public/home` | 首页聚合数据 |
| GET | `/api/v1/public/courses`、`/courses/{id}` | 前台课程列表/详情（走从库） |
| GET | `/api/v1/public/teachers`、`/articles`、`/materials`、`/banners` | 前台内容 |
| GET | `/api/v1/public/coupons?courseId=` | 课程可领优惠券（走从库） |
| POST | `/api/v1/coupons/{id}/claim` | 领取优惠券 |
| GET | `/api/v1/my/coupons`、`/my/orders`、`/my/courses` | 我的优惠券/订单/课程（强一致主库） |
| GET | `/api/v1/my/purchase-status?courseId=` | 课程购买状态（已购/待支付） |
| POST | `/api/v1/orders/preview`、`/orders` | 订单预览计价 / 创建订单 |
| POST | `/api/v1/orders/{orderNo}/cancel`、`/refund` | 取消订单 / 申请退款 |
| GET | `/api/v1/payments/channels`、POST `/payments/{orderNo}/pay` | 支付渠道 / 模拟支付 |
| GET | `/api/v1/admin/orders`、`/admin/refunds` | 后台订单 / 退款审核 |
| GET/POST/PUT | `/api/v1/admin/coupons*` | 后台优惠券管理 |
| GET | `/api/v1/system/route-check/read` | 验证读路由（`smart_learning_read`） |
| GET | `/api/v1/system/route-check/master` | 验证写路由（`smart_learning`） |
| POST | `/api/v1/system/kafka-check` | Kafka 生产者冒烟（发送 `system-check`） |

## 项目代码地图

完整目录与「路径 ↔ 模块/功能」对照见 [docs/project-structure.md](docs/project-structure.md)。

- 权限模型：[docs/permission.md](docs/permission.md)
- 读写分离：[docs/read-write-splitting.md](docs/read-write-splitting.md)
- 2026-11-25 会话记录：[docs/chat-history-2026-11-25.md](docs/chat-history-2026-11-25.md)

## 后续模块

秒杀（Redis + Kafka）、学习、题库考试、院校与看板、实时聊天、Elasticsearch 等，按模块逐个交付，见 `docs/project-structure.md` 末尾规划。
