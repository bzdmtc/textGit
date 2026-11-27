# 模块 5：学习（进度、笔记、收藏、评价）

> 目标：支付成功后进入学习页播放视频、记录学习进度与笔记；课程可收藏；学完可评价，评价经后台审核后展示并影响课程平均评分。

## 1. 数据模型

| 表 | 关键字段 | 说明 |
|---|---|---|
| `lesson_progress` | `user_id`、`lesson_id`、`watched_sec`、`completed` | `UNIQUE(user_id, lesson_id)`，进度只增不减 |
| `note` | `user_id`、`lesson_id`、`content` | 学员私有笔记，按课时查询 |
| `course_favorite` | `user_id`、`course_id` | `UNIQUE(user_id, course_id)` |
| `course_review` | `course_id`、`user_id`、`order_id`、`rating`、`content`、`status` | `UNIQUE(user_id, course_id)` 一单一评；状态 `PENDING/APPROVED/REJECTED` |

课件与章节沿用模块 2 的 `course_chapter` / `course_lesson`；`user_course.progress_percent` 由已完成课时数重算。

## 2. 访问控制

- 学习页、进度、笔记、评价提交都要求**已购买**：`user_course` 存在且 `status = ACTIVE`，否则返回 30009「请先购买课程后再学习」。
- 收藏允许任何登录用户操作已上架课程。
- 评价仅已购学员可提交；同一用户同一课程只有一条评价，修改后重新进入待审核。
- 后台审核需 `review:browse` / `review:approve`；前台评价列表只返回 `APPROVED`。

## 3. 进度规则

1. 前端播放时每 10 秒、暂停、切换课时、播放结束时上报 `watchedSec`（当前播放秒数）。
2. 后端校验课时属于该课程，`watchedSec` 截断在 `[0, durationSec]`，取历史最大值写入。
3. 完成判定：前端显式 `completed=true`，或观看达到时长 95%，或此前已完成；完成后不再回退。
4. 课程进度 = 已完成课时数 / 课程有效课时数 × 100，保留 2 位小数，写回 `user_course.progress_percent`。
5. 视频续播：学习页加载时把播放器定位到上次 `watchedSec`。

## 4. 评价与评分

- `POST /learn/courses/{id}/reviews` 新建/更新自己的评价 → `PENDING`，清空审核备注。
- 管理员通过/驳回后写 `reviewed_by/reviewed_at/admin_note`。
- 每次评价变更都按 `APPROVED` 评价重算课程 `rating`（四舍五入 2 位）；没有通过评价时保留原评分。
- 公共接口 `GET /public/courses/{id}/reviews` 返回平均分、通过数量和评价列表（含昵称与时间）。

## 5. 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/learn/courses/{courseId}` | 学习页数据：章节/课时/进度/收藏/我的评价 |
| POST | `/api/v1/learn/courses/{courseId}/lessons/{lessonId}/progress` | 上报进度（`watchedSec`、`completed`） |
| GET/POST | `/api/v1/learn/courses/{courseId}/lessons/{lessonId}/notes` | 笔记列表 / 新增 |
| PUT/DELETE | `/api/v1/learn/notes/{noteId}` | 修改 / 删除自己的笔记 |
| GET | `/api/v1/learn/favorites` | 我的收藏（含已购状态与进度） |
| GET | `/api/v1/learn/favorites/{courseId}/status` | 收藏状态 |
| POST/DELETE | `/api/v1/learn/favorites/{courseId}` | 收藏 / 取消收藏 |
| POST | `/api/v1/learn/courses/{courseId}/reviews` | 提交/更新课程评价 |
| GET | `/api/v1/public/courses/{courseId}/reviews` | 公开评价汇总（仅已通过） |
| GET | `/api/v1/admin/reviews` | 后台评价分页（`review:browse`） |
| PUT | `/api/v1/admin/reviews/{id}/status` | 通过/驳回（`review:approve`） |

## 6. 页面

- `/learn/:courseId/:lessonId?`：视频播放、章节切换、自动续播、进度条、标记完成、笔记增删改、收藏、课程评价。
- `/courses/:id`：已购显示「去学习」；新增收藏按钮与学员评价区；评分取已通过评价平均分。
- `/user?tab=courses`：我的课程显示进度与「继续学习」；新增 `/user?tab=favorites` 我的收藏。
- `/admin/reviews`：按状态/关键词筛选，通过或填写原因驳回。

## 7. 验收

```bash
# 建议先重置交易/学习数据
sqlcmd -S localhost -C -E -b -I -f 65001 -d smart_learning -i scripts/reset-trade-data.sql
node scripts/learn-smoke.mjs
```

冒烟覆盖：购课开通 → 未购拒绝 → 进度上报与课程进度 → 笔记增改删 → 收藏/取消与用户隔离 → 评价提交（不公开）→ 后台通过（公开、平均分与课程评分更新）→ 修改评价重新待审 → 我的课程进度 → 后台菜单权限。

UI 冒烟（需前后端已启动）：

```bash
node frontend/scripts/learn-ui-smoke.cjs
```
