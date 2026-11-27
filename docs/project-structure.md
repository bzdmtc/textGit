# 项目代码地图

> 本文件随模块交付持续更新。当前覆盖 **模块 1：系统底座**、**模块 2：课程与内容**、**模块 3：交易** 和 **模块 4：秒杀**。

## 一、目录树

```text
smart_learning/
├── pom.xml                                # 后端依赖与构建
├── README.md                              # 启动、演示账号、接口入口
├── docker-compose.yml                     # Kafka（KRaft 单节点，秒杀模块使用）
├── docs/
│   ├── project-structure.md               # 本文件：代码地图
│   ├── permission.md                      # 禅道式权限设计
│   ├── read-write-splitting.md            # 主从读写分离设计
│   ├── seckill.md                         # 秒杀：Redis/Kafka/库存对账
│   └── chat-history-2026-11-25.md         # 本次项目会话记录
├── scripts/
│   ├── init-databases.sql                 # 建库、账号、权限
│   └── sync-read-db.sql                   # 开发环境主库→从库同步
├── src/main/java/com/example/smart_learning/
│   ├── SmartLearningApplication.java      # 启动类
│   ├── common/
│   │   ├── api/                           # ApiResponse、PageResult
│   │   ├── datasource/                    # 路由键、ThreadLocal、@Master/@ReadOnly、RoutingDataSource
│   │   ├── domain/                        # BaseEntity（created_at/updated_at）
│   │   ├── error/                         # ErrorCode、BusinessException、全局异常
│   │   └── web/                           # TraceIdFilter
│   ├── config/
│   │   ├── DataSourceProperties.java      # 主/从 Hikari 配置
│   │   ├── ReadWriteDataSourceConfig.java # 主从 DataSource + Flyway（仅主库）
│   │   ├── DataSourceRoutingAspect.java   # @Master/@ReadOnly 路由切面
│   │   ├── WebConfig.java                 # 权限拦截器注册
│   │   ├── OpenApiConfig.java             # Swagger Bearer 配置
│   │   └── AopConfig.java                 # AspectJ 代理
│   ├── security/
│   │   ├── SecurityConfig.java            # Security 7 Lambda DSL、CORS、放行清单
│   │   ├── JwtService.java                # access token 签发/解析
│   │   ├── JwtAuthenticationFilter.java   # Bearer 认证过滤器
│   │   ├── LoginUser.java                 # UserDetails + 权限快照
│   │   ├── UserDetailsServiceImpl.java    # 用户/权限加载（强制主库）
│   │   ├── RequiresPriv.java              # 控制器权限注解
│   │   ├── PrivilegeInterceptor.java      # 方法级权限拦截
│   │   ├── RestAuthEntryPoint.java        # 401 JSON
│   │   ├── RestAccessDeniedHandler.java   # 403 JSON
│   │   └── SecurityUtils.java             # 当前登录用户
│   ├── auth/
│   │   ├── controller/AuthController.java # 登录/验证码/刷新/登出/me
│   │   ├── dto/                           # 登录、验证码、Token、MeResponse
│   │   ├── entity/RefreshToken.java       # refresh token 持久化
│   │   ├── repository/RefreshTokenRepository.java
│   │   └── service/
│   │       ├── AuthService.java           # 三种登录、令牌轮换、自动注册
│   │       ├── VerificationCodeService.java # Redis 验证码/限流/错误次数
│   │       ├── SmsSender.java             # 短信发送抽象
│   │       ├── MockSmsSender.java         # 模拟短信（日志）
│   │       └── EmailCodeSender.java       # 邮件验证码（可配 SMTP）
│   ├── permission/
│   │   ├── controller/                    # 分组、权限字典、用户、审计接口
│   │   ├── dto/                           # Group/Priv/User/PermissionSnapshot 等
│   │   ├── entity/                        # sys_group/sys_user_group/sys_priv/sys_group_priv/sys_dept/sys_audit_log
│   │   ├── repository/                    # JPA Repository
│   │   └── service/
│   │       ├── PrivilegeRegistry.java     # 代码声明的权限点清单
│   │       ├── PrivilegeSyncService.java  # 启动同步 sys_priv，废弃标记
│   │       ├── PermissionService.java     # 分组权限并集、数据范围、Caffeine 快照
│   │       ├── GroupService.java          # 分组 CRUD + 整组授权 + 版本递增
│   │       ├── UserAdminService.java      # 用户分页/创建/编辑/分组分配
│   │       ├── PrivService.java           # 权限字典
│   │       ├── DataScopeService.java      # ALL/OWN/SELF 数据范围判断
│   │       └── AuditService.java          # 权限变更审计
│   ├── user/
│   │   ├── entity/SysUser.java            # sys_user
│   │   └── repository/SysUserRepository.java
│   └── system/controller/SystemController.java # 主/从路由自检、Kafka 生产者冒烟
├── src/main/resources/
│   ├── application.yml                    # 数据源、Redis、Kafka、JWT、邮件
│   └── db/migration/
│       ├── V1__rbac_schema.sql            # 权限/用户/令牌表
│       └── V2__rbac_seed.sql              # 分组、权限、演示账号
└── src/test/                              # 集成测试（随模块补充）
```

前端：

```text
frontend/
├── vite.config.ts                         # /api 代理到 8080
├── src/
│   ├── main.ts                            # Vue/Pinia/Element Plus/路由/指令
│   ├── App.vue
│   ├── types.ts                           # 前后端 DTO 类型
│   ├── api/
│   │   ├── http.ts                        # Axios、Token、401 自动刷新
│   │   ├── auth.ts                        # 登录/验证码/me
│   │   ├── permission.ts                  # 分组/权限点
│   │   ├── user.ts                        # 用户/审计
│   │   └── system.ts                      # 路由自检
│   ├── stores/auth.ts                     # 登录态、权限、菜单
│   ├── directives/privilege.ts            # v-priv 按钮权限
│   ├── router/index.ts                    # 路由守卫
│   ├── layouts/AdminLayout.vue            # 后台布局/动态菜单
│   └── views/
│       ├── LoginView.vue                  # 三种登录
│       ├── ForbiddenView.vue
│       ├── NotFoundView.vue
│       └── admin/
│           ├── DashboardView.vue          # 概览 + 读写路由自检
│           ├── GroupListView.vue          # 权限分组/权限树
│           ├── UserListView.vue           # 用户/分组/权限预览
│           └── AuditLogView.vue           # 审计日志
```

## 二、路径 ↔ 模块/功能对照

| 路径/文件 | 模块 | 功能 | 关联接口/表/页面 |
|---|---|---|---|
| `config/ReadWriteDataSourceConfig` | 系统底座 | 主从数据源、Flyway 仅主库 | `application.yml`、`smart_learning_read` |
| `common/datasource/RoutingDataSource` | 系统底座 | 按事务只读标记路由，从库故障回退主库 | `/api/v1/system/route-check/*` |
| `system/controller/SystemController` | 系统底座 | 读写路由自检、Kafka 生产者冒烟 | `/api/v1/system/kafka-check` |
| `security/JwtAuthenticationFilter` | 认证 | Bearer 解析、加载权限 | `Authorization` 头、`sys_user` |
| `auth/service/AuthService` | 认证 | 密码/短信/邮箱登录、刷新轮换 | `/api/v1/auth/*`、`refresh_token` |
| `auth/service/VerificationCodeService` | 认证 | Redis 验证码、限流、错误次数 | `auth:code:*`、`auth:send:*` |
| `permission/service/PrivilegeSyncService` | 权限 | 启动同步权限字典 | `sys_priv` |
| `permission/service/PermissionService` | 权限 | 多分组并集、数据范围、快照缓存 | `sys_group_priv`、`sys_user_group` |
| `permission/service/GroupService` | 权限 | 分组 CRUD、整组覆盖授权 | `/api/v1/admin/groups*`、`sys_group_priv` |
| `permission/service/UserAdminService` | 权限 | 用户管理、分组分配、权限预览 | `/api/v1/admin/users*`、`sys_user_group` |
| `permission/service/DataScopeService` | 权限 | ALL/OWN/SELF 数据范围与资源归属判断 | 后续课程/题目/文章模块 |
| `permission/service/AuditService` | 权限 | 权限变更审计 | `sys_audit_log`、`/api/v1/admin/audit-logs` |
| `security/RequiresPriv` + `PrivilegeInterceptor` | 权限 | 接口级 VIEW/ACTION 校验 | 所有 `/api/v1/admin/**` |
| `views/admin/GroupListView.vue` | 权限 | 权限分组管理页 | `/admin/groups` |
| `views/admin/UserListView.vue` | 权限 | 用户与分组管理页 | `/admin/users` |
| `views/admin/AuditLogView.vue` | 权限 | 审计日志页 | `/admin/logs` |
| `views/LoginView.vue` | 认证 | 三种登录入口 | `/login` |
| `layouts/AdminLayout.vue` | 系统底座 | 后端菜单驱动的后台布局 | `/me` 返回的 `menus` |

## 三、模块 2：课程与内容代码地图

### 后端新增

```text
src/main/java/com/example/smart_learning/
├── course/
│   ├── controller/
│   │   ├── CourseAdminController.java      # 课程/章节/课时管理
│   │   └── TeacherAdminController.java     # 讲师管理
│   ├── dto/                                # Course/Chapter/Lesson/Teacher DTO
│   ├── entity/                             # course/teacher/course_teacher/chapter/lesson
│   ├── repository/                         # 课程/讲师/章节/课时 Repository
│   └── service/
│       ├── CourseAdminService.java         # 后台课程 CRUD + OWN 数据权限
│       ├── CoursePublicService.java        # 前台课程列表/详情（READ）
│       ├── TeacherAdminService.java        # 讲师管理
│       └── TeacherPublicService.java       # 前台讲师（READ）
├── content/
│   ├── controller/ContentAdminController.java # 文章/资料/Banner 后台
│   ├── dto/                                # Article/Material/Banner/Home
│   ├── entity/                             # article/article_category/material/banner
│   ├── repository/
│   └── service/
│       ├── ContentAdminService.java        # 内容 CRUD + Jsoup 清洗
│       └── ContentPublicService.java       # 前台资讯/资料/Banner
├── storage/
│   ├── FileStorageService.java             # 存储抽象
│   ├── LocalFileStorage.java               # 本地 uploads/ 实现
│   └── UploadController.java               # POST /admin/upload
└── publicapi/controller/PublicController.java # /api/v1/public/**
```

### 前端新增

```text
frontend/src/
├── layouts/PublicLayout.vue                # 前台导航/页脚
├── components/
│   ├── CourseCard.vue                      # 课程卡片
│   └── FileUploader.vue                    # 通用文件上传
├── views/public/
│   ├── HomeView.vue                        # 首页
│   ├── CourseListView.vue                  # 课程中心
│   ├── CourseDetailView.vue                # 课程详情/目录
│   ├── TeacherListView.vue                 # 名师团队
│   ├── ArticleListView.vue                 # 资讯列表
│   ├── ArticleDetailView.vue               # 资讯详情
│   └── MaterialListView.vue                # 资料下载
├── views/admin/
│   ├── CourseListView.vue                  # 课程/章节课时管理
│   ├── TeacherListView.vue                 # 讲师管理
│   ├── ArticleListView.vue                 # 文章管理
│   ├── MaterialListView.vue                # 资料管理
│   └── BannerListView.vue                  # Banner 管理
├── api/course.ts                           # 课程/讲师接口
├── api/content.ts                          # 文章/资料/Banner 接口
├── api/public.ts                           # 前台聚合接口
└── api/upload.ts                           # 文件上传
```

### 路径 ↔ 功能对照

| 路径/文件 | 模块 | 功能 | 关联接口/表/页面 |
|---|---|---|---|
| `course/service/CourseAdminService` | 课程 | 课程 CRUD、章节课时、上下架、OWN 数据范围 | `/api/v1/admin/courses*`、`course*` 表 |
| `course/service/CoursePublicService` | 课程 | 前台课程列表/详情，只读从库 | `/api/v1/public/courses*` |
| `course/service/TeacherAdminService` | 讲师 | 讲师维护、归属校验 | `/api/v1/admin/teachers*`、`teacher` |
| `content/service/ContentAdminService` | 内容 | 文章/资料/Banner、HTML 清洗 | `/api/v1/admin/articles|materials|banners` |
| `content/service/ContentPublicService` | 内容 | 前台资讯/资料/Banner | `/api/v1/public/articles|materials|banners` |
| `storage/LocalFileStorage` | 内容 | 本地文件存储与路径校验 | `/api/v1/admin/upload`、`/uploads/**` |
| `views/public/HomeView.vue` | 前台 | 首页 Banner/热门课程/名师/资讯 | `/api/v1/public/home` |
| `views/admin/CourseListView.vue` | 课程 | 课程与章节课时后台页 | `/admin/courses` |

## 四、模块 3：交易代码地图

### 后端新增

```text
src/main/java/com/example/smart_learning/
├── coupon/
│   ├── controller/
│   │   ├── CouponAdminController.java        # 后台优惠券 CRUD / 启停
│   │   └── CouponUserController.java         # 学员领券
│   ├── dto/                                  # CouponDto/CouponRequest/CouponStatusRequest/UserCouponDto
│   ├── entity/                               # Coupon、UserCoupon
│   ├── repository/                           # CouponRepository、UserCouponRepository（悲观锁）
│   └── service/
│       ├── CouponAdminService.java           # 后台建券/编辑/停用 + 审计
│       ├── CouponUserService.java            # 领券限领、我的券、锁定/核销/释放、过期展示
│       └── CouponCalculator.java             # 满减/折扣/适用科目纯计算（单测覆盖）
├── order/
│   ├── domain/                               # OrderStatus、OrderStateMachine
│   ├── entity/                               # Order、OrderItem、RefundRequest
│   ├── repository/                           # 订单/明细/退款仓储（行锁、后台检索）
│   ├── dto/                                  # 下单/预览/订单/退款/购买状态 DTO
│   ├── controller/
│   │   ├── OrderController.java              # 预览/下单/取消/申请退款
│   │   ├── MyController.java                 # 我的订单/优惠券/课程/购买状态
│   │   ├── OrderAdminController.java         # 后台订单列表/详情
│   │   └── RefundAdminController.java        # 后台退款审核
│   └── service/
│       ├── OrderService.java                 # 服务端计价、下单、取消、退款申请
│       ├── OrderAdminService.java            # 订单检索、退款通过/驳回
│       ├── OrderAssembler.java               # 批量装配明细/优惠券/退款/用户
│       ├── TradeScheduler.java               # @Scheduled 薄壳入口
│       └── TradeSweepService.java            # 独立事务：超时关单/券过期/7 天自动完成
├── payment/
│   ├── controller/PaymentController.java     # 渠道列表/模拟支付
│   ├── dto/                                  # PayRequest/ChannelDto/PaymentResultDto
│   ├── entity/PaymentRecord.java
│   ├── gateway/                              # PaymentChannel 抽象 + 支付宝/微信模拟实现
│   ├── repository/PaymentRecordRepository.java
│   └── service/PaymentService.java           # 幂等支付、流水、状态机、开通课程
└── enrollment/
    ├── entity/UserCourse.java
    ├── repository/UserCourseRepository.java
    ├── dto/MyCourseDto.java
    └── service/EnrollmentService.java        # 支付开通/退款撤销/我的课程（主库强一致）
```

### 前端新增

```text
frontend/src/
├── api/trade.ts                              # 优惠券/订单/支付/我的/后台接口
├── utils/format.ts                           # UTC 解析、状态标签、优惠券展示
├── views/public/
│   ├── CheckoutView.vue                      # 确认订单/选券/服务端计价
│   ├── PayView.vue                           # 模拟收银台/倒计时/支付结果
│   ├── UserCenterView.vue                    # 我的订单/优惠券/课程 + 退款申请
│   └── CourseDetailView.vue                  # 增加领券/购买/待支付回跳
└── views/admin/
    ├── OrderListView.vue                     # 订单列表/详情
    ├── RefundListView.vue                    # 退款审核
    └── CouponListView.vue                    # 优惠券管理
```

### 路径 ↔ 功能对照

| 路径/文件 | 模块 | 功能 | 关联接口/表/页面 |
|---|---|---|---|
| `coupon/service/CouponUserService` | 营销 | 原子领券（悲观锁 + 限领）、锁定/核销/释放 | `/api/v1/coupons/{id}/claim`、`/api/v1/my/coupons`、`coupon`、`user_coupon` |
| `coupon/service/CouponCalculator` | 营销 | 满减/折扣/适用科目/最低消费计算 | `CouponCalculatorTest` |
| `coupon/service/CouponAdminService` | 营销 | 后台建券/编辑/停用、库存守恒 | `/api/v1/admin/coupons*`、`/admin/coupons` |
| `order/service/OrderService` | 交易 | 预览计价、创建订单、取消、申请退款、购买状态 | `/api/v1/orders*`、`/api/v1/my/*`、`orders`、`order_item`、`refund_request` |
| `order/domain/OrderStateMachine` | 交易 | 订单状态流转集中校验 | `OrderStateMachineTest` |
| `order/service/OrderAdminService` | 交易 | 订单检索、退款通过/驳回、撤销课程 | `/api/v1/admin/orders`、`/api/v1/admin/refunds` |
| `order/service/OrderAssembler` | 交易 | 订单 DTO 批量装配（明细/优惠券/退款/用户） | 订单列表/详情 |
| `payment/service/PaymentService` | 支付 | 幂等支付、流水、状态机、开通课程 | `/api/v1/payments/*`、`payment_record` |
| `payment/gateway/*` | 支付 | `PaymentChannel` 抽象；后续接真实支付宝/微信 | `MockAlipayChannel`、`MockWechatChannel` |
| `enrollment/service/EnrollmentService` | 学习 | 支付开通、退款撤销、我的课程（固定主库） | `/api/v1/my/courses`、`user_course` |
| `order/service/TradeSweepService` | 交易 | 超时关单、优惠券过期、支付满 7 天自动完成 | `TradeScheduler` |
| `views/public/CheckoutView.vue` | 交易 | 确认订单、选券、金额展示 | `/checkout` |
| `views/public/PayView.vue` | 交易 | 模拟收银台、支付倒计时、支付结果 | `/pay/:orderNo` |
| `views/public/UserCenterView.vue` | 交易 | 我的订单/优惠券/课程、退款申请 | `/user?tab=orders\|coupons\|courses` |
| `views/admin/OrderListView.vue` | 交易 | 订单列表与详情 | `/admin/orders` |
| `views/admin/RefundListView.vue` | 交易 | 退款审核（通过/驳回） | `/admin/refunds` |
| `views/admin/CouponListView.vue` | 营销 | 优惠券创建/编辑/启停 | `/admin/coupons` |

### 自动化验收脚本

| 脚本 | 说明 |
|---|---|
| `scripts/reset-trade-data.sql` | 重置交易数据，恢复优惠券库存/课程人数，便于重复跑冒烟 |
| `scripts/trade-smoke.mjs` | API 全链路：登录 → 领券 → 预览/下单 → 支付 → 开通 → 退款 → 审核 → 取消 |
| `scripts/trade-scheduler-test.mjs` | 超时关单、优惠券释放与过期 |
| `frontend/scripts/trade-ui-smoke.cjs` | 浏览器 UI 全链路（Playwright/Edge），截图输出到 `frontend/screenshots/` |
| `scripts/sync-read-db.sql` | 主库备份还原到只读从库（重置数据后需重新执行） |

> 时间约定：`DATETIME2` 统一存 UTC，Java 使用 `LocalDateTime.now(ZoneOffset.UTC)`；不使用 `hibernate.jdbc.time_zone`，避免 `LocalDateTime` 二次时区换算导致 SQL 时间比较（订单超时、券有效期）错位。前端通过 `utils/format.ts` 转为本地时间展示。

## 四之二、模块 4：秒杀代码地图

### 后端新增

| 路径 | 说明 |
|---|---|
| `seckill/config/SeckillKafkaConfig` | `seckill-order` / `seckill-order-dlq` Topic、生产者/消费者工厂、手动 ack、重试 3 次死信 |
| `seckill/service/SeckillStockService` | Redis 库存预热、Lua 原子扣减/限购、回补、库存调整 |
| `seckill/service/SeckillPurchaseService` | 抢购接口：活动/时间/重复校验 → Lua → Kafka 入队；结果轮询 |
| `seckill/service/SeckillOrderProcessor` | 消费者单事务建单：DB 原子扣库存、`orders/order_item/seckill_record`、唯一约束幂等 |
| `seckill/service/SeckillKafkaConsumer` | `@KafkaListener` 手动 ack、失败回补 Redis、重复消息忽略 |
| `seckill/service/SeckillOrderEventListener` | 支付/取消事件：记录 CREATED→PAID / CREATED→CANCELED 并回补库存 |
| `seckill/service/SeckillActivityService` | 后台活动 CRUD、上下线、对账、抢购记录；前台列表 |
| `seckill/service/SeckillWarmupRunner` | 启动时把未结束活动的 DB 库存预热到 Redis |
| `seckill/controller/SeckillController` | `/api/v1/seckill/activities*` |
| `seckill/controller/SeckillAdminController` | `/api/v1/admin/seckill/activities*` |
| `order/event/OrderPaidEvent`、`OrderCanceledEvent` | 交易 → 秒杀解耦事件（AFTER_COMMIT + REQUIRES_NEW 处理） |
| `order/service/OrderNumbers` | 订单号生成（普通/秒杀共用） |

### 前端新增

| 文件 | 说明 |
|---|---|
| `views/public/SeckillView.vue` | 秒杀专区：倒计时、剩余名额、抢购排队与结果轮询、跳转收银台 |
| `views/admin/SeckillListView.vue` | 后台活动 CRUD、上下线、库存对账、抢购记录抽屉 |
| `api/seckill.ts`、`types.ts` | 秒杀 DTO 与接口封装 |
| `router/index.ts` | `/seckill`、`/admin/seckill`（`seckill:browse`） |

### 路径 ↔ 功能对照

| 路径 | 模块 | 功能 |
|---|---|---|
| `/seckill` | 秒杀 | 前台秒杀专区 + 排队结果轮询 |
| `/api/v1/seckill/activities` | 秒杀 | 公开列表（剩余库存取 Redis，缺失回退 DB） |
| `/api/v1/seckill/activities/{id}/claim` | 秒杀 | 登录抢购：Lua 预扣 + Kafka `seckill-order` |
| `/api/v1/seckill/activities/{id}/result` | 秒杀 | `NOT_JOINED/QUEUED/CREATED/PAID/CANCELED/FAILED` |
| `/admin/seckill` | 秒杀 | 后台活动管理、对账、记录 |
| `/api/v1/admin/seckill/activities*` | 秒杀 | CRUD/状态/对账/记录，权限 `seckill:*` |

### 自动化验收脚本

| 脚本 | 说明 |
|---|---|
| `scripts/seckill-smoke.mjs` | 建活动/预热 → 抢购 → Kafka 建单 → 支付 → PAID；取消 → CANCELED + 库存回补 |
| `scripts/seckill-concurrency.mjs` | 20 用户 × 10 并发抢 10 名额：恰好 10 单、无超卖/重复；重复 Kafka 消息不重复建单 |
| `scripts/reset-seckill-data.sql` | 清理秒杀订单/记录/临时用户并回补库存（保留 V11 演示活动） |

## 五、数据库迁移对照

| 脚本 | 模块 | 内容 |
|---|---|---|
| `V1__rbac_schema.sql` | 权限 + 认证 | `sys_user`、`sys_group`、`sys_user_group`、`sys_priv`、`sys_group_priv`、`sys_dept`、`sys_audit_log`、`refresh_token` |
| `V2__rbac_seed.sql` | 权限 + 认证 | 4 个分组、15 个权限点、4 个演示账号、分组授权 |
| `V3__course_content_schema.sql` | 课程 + 内容 | teacher/course/course_teacher/course_chapter/course_lesson/article/article_category/material/banner |
| `V4__course_content_seed.sql` | 课程 + 内容 | 23 个新权限点、3 位讲师、4 门课程、章节课时、3 篇文章、2 份资料、2 个 Banner |
| `V5__course_content_polish.sql` | 内容 | 补充政治第二章节课时 |
| `V6__trade_schema.sql` | 交易 | `coupon`、`user_coupon`、`orders`、`order_item`、`payment_record`、`refund_request`、`user_course` 及索引/唯一约束 |
| `V7__trade_seed.sql` | 交易 | 7 个新权限点（订单/退款/优惠券）、管理员授权、3 张演示优惠券 |
| `V8__user_position.sql` | 系统底座 | `sys_user.position`（职位，独立于权限分组） |
| `V9__user_position_seed.sql` | 系统底座 | 补充演示账号职位（独立迁移，避免同批编译时列不存在） |
| `V10__seckill_schema.sql` | 秒杀 | `seckill_activity`、`seckill_record`、库存/时间/限购约束与索引 |
| `V11__seckill_seed.sql` | 秒杀 | 4 个秒杀权限点 + ADMIN 授权 + 2 个演示活动（进行中/即将开始） |

## 六、后续模块规划

| 模块 | 预计新增目录/文件 | 状态 |
|---|---|---|
| 系统底座 | `security/`、`permission/`、`common/`、`config/` | 已完成 |
| 课程与内容 | `course/`、`content/`、`storage/`、前台页面 | 已完成 |
| 交易 | `order/`、`payment/`、`coupon/`、`enrollment/` | 已完成 |
| 秒杀 | `seckill/`、Kafka 生产者/消费者、Redis Lua | 已完成 |
| 学习 | `learn/`、播放器/进度/笔记/评价 | 待实现（下一模块） |
| 题库考试 | `question/`、`exam/` | 待实现 |
| 院校看板 | `university/`、`stats/` | 待实现 |
| 实时聊天 | `chat/`（WebSocket/STOMP） | 后续 |
| Elasticsearch | `search/` 的 ES 实现 | 后续 |
