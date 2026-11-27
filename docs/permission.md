# 禅道式权限分组

## 1. 模型

授权主体是**权限分组**，不是角色或职位。用户可属于多个分组，有效权限取并集，鉴权只查 `sys_group_priv`。`sys_user.position` 是用户的组织岗位信息（仅展示/检索）；`sys_group.role` 为兼容旧数据保留的隐藏字段，后台不再展示或编辑。

```text
sys_user ──< sys_user_group >── sys_group ──< sys_group_priv >── sys_priv
```

| 表 | 说明 |
|---|---|
| `sys_user` | 用户，含 `priv_version`（权限版本）与 `position`（职位，仅组织展示） |
| `sys_group` | 权限分组，含 `name`、`description`（分组描述）、`data_scope`（ALL/OWN/SELF）、`limited` |
| `sys_user_group` | 用户—分组多对多，`UNIQUE(user_id, group_id)` |
| `sys_priv` | 权限字典，`module + method`，`type` 为 VIEW/ACTION |
| `sys_group_priv` | 分组授权，`UNIQUE(group_id, priv_id)`，FK 到 `sys_priv` |
| `sys_position` | 职位字典，`name` 唯一，`group_id` 绑定默认权限分组，`status`/`sort` |
| `sys_dept` | 部门（预留） |
| `sys_audit_log` | 权限变更审计 |

**职位与分组的区别**：`sys_user.position` 是组织岗位信息（如「考研政治讲师」「学员」），本身不直接鉴权；真正的权限仍只由 `sys_user_group → sys_group_priv` 的并集决定。职位通过 `sys_position.group_id` 绑定一个**默认权限分组**，用于新增/编辑用户时自动带出分组，管理员仍可手动调整。

### 分组管理页

后台 `/admin/groups` 的列表对齐禅道权限分组：**ID、分组名称、分组描述、用户列表、操作**。

- 分组名称下显示唯一编码（内部标识，创建时可编辑）。
- 「用户列表」显示人数，点击弹窗查看组内用户（账号/姓名、职位、状态、最后登录）；接口 `GET /api/v1/admin/groups/{id}/users`。
- 数据范围、受限、排序等高级项收进「编辑分组」弹窗；分配权限仍在操作列的「分配权限」。
- 新增 `V16__group_description.sql` / `V17__group_description_seed.sql`，为内置分组补充描述。

### 职位字典与默认分组

对齐禅道「添加用户选择职位后默认带出权限分组」：

- `sys_position`（V18）保存职位名称、默认分组、状态与排序；`V19` 预置系统管理员/讲师/考研政治讲师/学员/受限用户。
- 用户管理页「职位管理」可新增、编辑、启停职位；新增/编辑用户时职位为下拉选择。
- 选择职位后，前端自动把 `sys_position.group_id` 加入用户分组；**切换职位时移除旧职位的默认分组、加入新职位的默认分组**，其他手动分组保留。
- 保存时仍以最终勾选的分组为准，可手动增删。
- 后端兜底：新增用户时若职位有映射且未传分组，自动使用默认分组；编辑用户时若职位变化且未传 `groupIds`，按“移除旧职位默认分组、加入新职位默认分组”处理。显式传 `groupIds` 时整组覆盖。
- 修改职位名称会同步更新现有用户的 `sys_user.position` 显示值；修改职位映射不会回溯改动现有用户的分组。
- 职位管理接口复用 `user:browse`（查询）与 `user:edit`（增改/状态），不新增权限点：
  - `GET /api/v1/admin/positions`
  - `POST /api/v1/admin/positions`
  - `PUT /api/v1/admin/positions/{id}`
  - `PUT /api/v1/admin/positions/{id}/status`

## 2. 权限点

- 粒度：`module + method`，例如 `course/create`、`group/assign-priv`。
- VIEW：菜单/页面可见性，如 `group/browse`。
- ACTION：按钮/写接口，如 `group/create`。
- 权限点由 `PrivilegeRegistry` 在代码中声明，应用启动时 `PrivilegeSyncService` 同步到 `sys_priv`：
  - 缺失则插入；
  - 已注册则更新名称/菜单/排序；
  - 代码中已移除的标记 `DEPRECATED`，不物理删除。
- 后台只能勾选 `sys_priv` 中存在的权限，避免“幽灵权限”。

## 3. 鉴权流程

1. 登录成功签发 access token（JWT，2 小时）和 refresh token（随机串，数据库存哈希，7 天，轮换）。
2. 每次请求 `JwtAuthenticationFilter` 解析 token，按用户 ID 从主库加载用户与权限快照。
3. 权限快照：
   - 查用户所有启用分组；
   - 查分组权限并集；
   - 计算最高数据范围：任一 ALL 即 ALL，否则 OWN，否则 SELF；
   - 仅当用户所有分组都受限时 `limited=true`。
4. 控制器方法标注 `@RequiresPriv(module, method)`，`PrivilegeInterceptor` 统一校验；无权限返回 403。
5. 前端菜单由 `/api/v1/auth/me` 返回的 VIEW 权限生成；路由守卫和 `v-priv` 只做体验层，后端必须二次校验。

## 4. 权限即时生效

- 分组授权、用户分组、分组数据范围变化时，递增受影响用户的 `sys_user.priv_version`。
- access token 带 `pv`；服务端每次请求加载当前快照，不依赖 token 内的旧权限。
- Caffeine 快照按 `userId + privVersion` 缓存，版本变化自动失效；数据库是唯一事实来源。

## 5. 数据范围

`DataScopeService` 约定的规则（课程/订单模块接入时使用）：

| 范围 | 含义 | 查询条件 |
|---|---|---|
| ALL | 全部数据 | 不加限制 |
| OWN | 本人负责的数据 | `owner_user_id = 当前用户` |
| SELF | 仅本人数据 | `user_id = 当前用户` |

RBAC 管“能不能做”，数据范围管“能做哪些行”。讲师只能操作自己的课程/题目/文章。

## 6. 种子数据

分组：`ADMIN`（ALL）、`TEACHER`（OWN）、`STUDENT`（SELF）、`LIMITED`（OWN + limited）。

演示账号：

| 账号 | 密码 | 分组 |
|---|---|---|
| admin | Admin@123 | ADMIN |
| teacher | Teacher@123 | TEACHER |
| student | Student@123 | STUDENT |
| teacher2 | Teacher@123 | TEACHER + STUDENT |

## 7. 模块 2 新增权限

V4 迁移新增 23 个权限点（总数 38）：

- `course/browse|create|edit|delete|publish`
- `teacher/browse|create|edit|delete`
- `article/browse|create|edit|delete|publish`
- `material/browse|create|edit|delete`
- `banner/browse|create|edit|delete`
- `file/upload`

分配：

- `ADMIN`：全部新权限。
- `TEACHER`：课程、文章、资料的浏览/新增/编辑/删除/发布，讲师浏览，文件上传；数据范围 `OWN`，只能操作 `owner_user_id = 当前用户` 的数据。
- `STUDENT`：不授予后台内容权限。

## 8. 模块 3/4 新增权限

- 模块 3 交易（V7）：`order/browse`、`refund/browse|approve`、`coupon/browse|create|edit|delete`
- 模块 4 秒杀（V11）：`seckill/browse`（VIEW，菜单 `/admin/seckill`）、`seckill/create`、`seckill/edit`（编辑/上下线/对账）、`seckill/delete`
- 模块 5 学习（V13）：`review/browse`（VIEW，菜单 `/admin/reviews`）、`review/approve`（通过/驳回评价）
- 模块 6 题库（V15）：`question/browse|create|edit|delete`（菜单 `/admin/questions`）、`paper/browse|create|edit|delete|publish`（菜单 `/admin/papers`）

分配：`ADMIN` 自动获得全部新权限；`TEACHER` 额外获得题库/试卷维护权限（数据范围 `OWN`，只能操作自有题目与试卷）；`STUDENT` 不授予秒杀、评价审核、题库后台权限。前台秒杀浏览公开，抢购仅需登录；学习与评价提交需已购课程；练习与考试仅需登录。

## 9. 验收用例

1. 直接 SQL 查询 `sys_group_priv`、`sys_user_group` 有数据；重启服务后不丢。
2. admin 可访问 `/api/v1/admin/groups`；teacher 访问返回 403。
3. teacher2 的权限为 TEACHER 与 STUDENT 的并集。
4. 给分组移除 `group/browse` 后，该组用户下一次请求访问分组列表返回 403。
5. 无 VIEW 权限时前端菜单不显示；无 ACTION 权限时按钮隐藏且接口 403。
6. 提交未注册的权限点 ID 被拒绝。
7. 每次授权变更在 `sys_audit_log` 有前后差异记录。
8. 分组列表按「ID / 分组名称 / 分组描述 / 用户列表 / 操作」展示；点击用户列表可查看组内用户；编辑弹窗不含角色标签，职位仍在用户管理中维护。
9. 新增/编辑用户选择职位后自动带出默认权限分组；可手动取消或追加分组；保存后权限按最终分组并集计算。
