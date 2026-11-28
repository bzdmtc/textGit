# 模块 7：院校库与数据看板

## 一、范围

- 前台院校库：按省份 / 层次 / 类型 / 关键词检索，院校详情包含专业目录、历年复试分数线、报录比。
- 首页「院校推荐」：展示后台标记为精选且启用的院校。
- 后台院校库管理：院校 CRUD、专业目录、分数线、报录比维护，所有变更写审计日志。
- 管理后台数据看板：成交额、订单、新增用户、学习人次、趋势、热销课程、订单状态分布。
- 用户管理完善：状态 / 职位筛选、注册与最近登录时间、用户活跃数据（订单 / 消费 / 选课 / 刷题 / 错题）。
- 审计日志完善：按目标类型、动作关键词、操作人、日期范围检索，目标类型选项来自数据库。

## 二、数据表（V20）

| 表 | 说明 |
|---|---|
| `university` | 院校：名称、省份、城市、层次（985/211/双一流/普通）、类型、官网、校徽、简介、精选、排序、状态 |
| `university_major` | 专业：院校、名称、代码、学硕/专硕、学制、学费、初试科目、简介、状态；`(university_id, name, degree_type)` 唯一 |
| `score_line` | 历年分数线：院校、专业、年份、政治/英语/数学/专业课/总分；`(major_id, year)` 唯一 |
| `admission_stat` | 报录比：院校、专业、年份、报考人数、录取人数、报录比、最低分；`(major_id, year)` 唯一；比例由服务端计算 |

统一软删除：院校/专业通过 `status=DISABLED` 停用，前台不展示；分数线/报录比可直接删除。

演示数据（V21）：10 所院校（含 985/211、综合/理工/财经/师范）、13 个专业、2023–2025 三年分数线与报录比。

## 三、接口

### 前台（无需登录）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/public/universities` | 分页检索：`keyword/province/level/type/page/size`，仅启用院校 |
| GET | `/api/v1/public/universities/options` | 筛选选项：省份/层次/类型（来自启用院校去重） |
| GET | `/api/v1/public/universities/featured?limit=6` | 首页精选院校 |
| GET | `/api/v1/public/universities/{id}` | 院校详情：专业、分数线、报录比 |

`/api/v1/public/home` 新增 `universities` 字段（精选院校，最多 6 所）。

### 后台（权限 `university:browse/create/edit/delete`）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/universities` | 分页检索（含停用），支持 `status` |
| GET | `/api/v1/admin/universities/options` | 筛选选项 |
| GET | `/api/v1/admin/universities/{id}` | 聚合详情（含停用专业） |
| POST / PUT | `/api/v1/admin/universities`、`/{id}` | 新增 / 编辑院校 |
| PUT | `/api/v1/admin/universities/{id}/status` | 启用 / 停用 |
| DELETE | `/api/v1/admin/universities/{id}` | 停用院校 |
| GET/POST | `/api/v1/admin/universities/{id}/majors` | 专业列表 / 新增 |
| PUT/DELETE | `/api/v1/admin/universities/{id}/majors/{majorId}` | 编辑 / 停用专业 |
| GET/POST | `/api/v1/admin/universities/{id}/score-lines` | 分数线列表 / 新增 |
| PUT/DELETE | `/api/v1/admin/universities/{id}/score-lines/{lineId}` | 编辑 / 删除分数线 |
| GET/POST | `/api/v1/admin/universities/{id}/admission-stats` | 报录比列表 / 新增 |
| PUT/DELETE | `/api/v1/admin/universities/{id}/admission-stats/{statId}` | 编辑 / 删除报录比 |

所有写接口校验嵌套归属：专业必须属于该院校，分数线/报录比的专业必须属于该院校，防止跨院校越权修改。

### 数据看板

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/stats/dashboard?days=14` | 经营看板，`days` 取值 7/14/30（最大 90） |

响应字段：

- `summary`：今日成交额/订单/新增用户/学习人次、累计成交额/订单/付费订单、累计用户、累计选课、待处理退款、客单价。
- `trend`：按北京时间自然日的每日成交额、订单数、新增用户、学习人次，缺失日期补零。
- `hotCourses`：所选周期成交金额 TOP 6（课程、科目、成交单数、成交金额）。
- `orderStatus`：所选周期创建订单的状态分布。

统计口径：

- 成交额/订单数只统计 `PAID`、`COMPLETED` 状态，按 `paid_at` 归属日期；退款中/已退款不计入。
- 学习人次取 `user_course.status=ACTIVE`（退款会删除选课关系），按 `enrolled_at` 统计。
- 数据存 UTC，日报按 **北京时间（UTC+8）** 自然日切分；SQL 使用 `DATEADD(HOUR, 8, ...)` 分组。
- 看板查询强制走 **主库**，保证管理员看到的数字与订单/用户管理实时一致。

### 用户管理完善

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/admin/users?status=&position=` | 新增状态、职位筛选 |
| GET | `/api/v1/admin/users/{id}/activity` | 活跃数据：订单数、已支付订单、累计消费、已购课程、练习次数、待攻克错题 |
| GET | `/api/v1/admin/audit-logs?targetType=&action=&userId=&from=&to=` | 审计检索；`from/to` 为北京时间日期 |
| GET | `/api/v1/admin/audit-logs/target-types` | 审计目标类型下拉选项 |

## 四、权限

| module | method | 类型 | 菜单 |
|---|---|---|---|
| university | browse | VIEW | `/admin/universities`（院校库管理） |
| university | create | ACTION | 新增院校/专业/分数线/报录比 |
| university | edit | ACTION | 编辑院校/专业/分数线/报录比、启停用 |
| university | delete | ACTION | 停用院校/专业、删除分数线/报录比 |

数据看板复用 `admin:access`；V21 仅给 ADMIN 分组授权院校权限，讲师/学员访问返回 403。

## 五、前端

| 路径 | 页面 |
|---|---|
| `/universities` | 院校库检索（筛选、卡片、分页） |
| `/universities/:id` | 院校详情（专业目录 / 分数线走势图 / 报录比） |
| `/admin/universities` | 院校管理与「招生数据」抽屉 |
| `/admin/dashboard` | 数据看板（KPI、趋势图、热销课程、订单状态） |
| `/admin/users` | 新增状态/职位筛选、注册/最近登录、活跃数据弹窗 |
| `/admin/logs` | 审计日志多条件检索 |

图表使用 ECharts 6（按需引入 Line/Bar/Grid/Tooltip/Canvas），颜色取自数据可视化调色板：蓝 `#257de0`、橙 `#e76624`（均通过白底对比度与色盲可辨性校验）。

## 六、验收

```bash
# 1. 后端已启动（Kafka 未启动时会等待约 40–50 秒）
# 2. API 全链路
node scripts/university-smoke.mjs

# 3. 清理冒烟数据
sqlcmd -S localhost -C -E -b -f 65001 -i scripts/reset-university-data.sql
```

冒烟覆盖：公开检索与详情、后台 CRUD、重复校验、停用前台隐藏、精选推荐、看板趋势与订单总数一致、用户筛选与活跃数据、审计检索、讲师/学员权限隔离。

新增/修改表后需同步从库：停后端 → `scripts/sync-read-db.sql` → 重启。
