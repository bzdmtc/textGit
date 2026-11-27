# 模块 6：题库与考试

> 覆盖题目/选项、试卷、章节练习、限时套卷、服务端判分、成绩报告与错题本。迁移：`V14__practice_schema.sql`、`V15__practice_seed.sql`。

## 一、功能范围

### 学员端

| 能力 | 说明 |
|---|---|
| 题库首页 `/practice` | 科目/章节题量统计、已发布试卷、我的练习统计、最近做题记录 |
| 章节练习 | 按科目/章节抽题（1–50 题），每题作答后立即判题并展示正确答案与解析 |
| 错题重做 | 只抽取未掌握错题；可不限科目；重做答对自动标记掌握 |
| 限时套卷 | 从已发布试卷开考，开考时固化题目快照，倒计时结束自动交卷 |
| 服务端判分 | 交卷时统一判分，未作答按 0 分计但不计入「答错」 |
| 成绩报告 `/practice/result/:recordId` | 得分、得分率、答对/答错/未作答、用时、逐题回顾与解析 |
| 错题本 `/wrong-book` | 按科目/掌握状态筛选、查看答案、标记掌握、移除、错题重做 |
| 个人中心 | 「我的错题」入口与最近错题列表 |

### 管理端

| 能力 | 说明 |
|---|---|
| 题库管理 `/admin/questions` | 单选/多选/判断题 CRUD、选项与正确答案、难度、解析、启停用 |
| 试卷管理 `/admin/papers` | 从题库组卷、逐题分值、发布/下架、停用、时长与说明 |
| 讲师数据范围 | `TEACHER` 分组拥有题库/试卷权限，但只能查看和修改 `owner_user_id` 为自己的数据 |

## 二、数据模型

| 表 | 说明 |
|---|---|
| `question` | 科目、章节、题型（SINGLE/MULTIPLE/JUDGE）、题干、解析、难度 1–5、状态（ACTIVE/DISABLED）、`owner_user_id` |
| `question_option` | `question_id` + `label` 联合唯一，`is_correct` 标记正确项 |
| `paper` | 标题、类型（CHAPTER/PAST/MOCK）、科目/章节、总分、时长、状态（DRAFT/PUBLISHED/OFF）、`owner_user_id` |
| `paper_question` | `paper_id` + `question_id` 联合唯一，逐题分值 `score` |
| `exam_record` | 用户、试卷（练习为空）、模式（PRACTICE/EXAM）、来源（NORMAL/WRONG）、开始/交卷/截止时间、得分、正确/错误/题数、状态（IN_PROGRESS/SUBMITTED） |
| `exam_answer` | `record_id` + `question_id` 联合唯一，用户答案、是否正确、得分、题目快照 JSON |
| `wrong_question` | `user_id` + `question_id` 联合唯一，错误次数、最后错误时间、是否掌握 |

关键约束：

- 题目选项标签同一题内唯一；`paper_question` 同一试卷内题目不重复。
- `exam_answer.question_snapshot` 保存开考时的题干、选项、正确答案、解析与分值，题目后续修改/停用不影响历史成绩。
- 错题本按「用户 + 题目」唯一，重复答错累计 `wrong_count`。

## 三、判分规则

答案统一存储为按字母排序的选项标签串：

- 单选：`A`、`B`……
- 多选：`AC`、`ABD`……
- 判断：固定两个选项，`A` = 正确、`B` = 错误。

判分逻辑（`exam/service/QuestionGrader`，有单元测试）：

1. 忽略大小写、顺序与分隔符，例如 `"c, a"` 归一化为 `AC`。
2. 与正确答案标签集合完全相等才得分；多选少选/多选均不得分。
3. 空答案不得分，且不写入错题本。
4. 标签必须属于该题选项，否则返回 `50011 答案格式不正确`。

练习与套卷的分值：

- 章节练习/错题重做：每题固定 5 分，总分 = 题数 × 5。
- 限时套卷：每题分值来自 `paper_question.score`，试卷总分由组卷时累加。

成绩口径：

- `correctCount`：答对题数。
- `wrongCount`：**已作答**但答错的题数。
- `unansweredCount`：未作答题数（仅成绩报告计算，不入错题本）。
- 三者之和 = 本次题目总数。

## 四、流程

### 章节练习

1. `POST /api/v1/practice/sessions` 传 `subject`、可选 `chapter`、`count`；错题重做传 `source=WRONG`（`subject` 可空表示全部科目）。
2. 服务端按科目/章节抽取 ACTIVE 题目（错题重做按最近错误时间取未掌握错题），创建 `exam_record`（PRACTICE）并为每题写入快照。
3. `PUT /api/v1/exams/{recordId}/answers` 保存答案；练习模式立即判分并返回正确答案与解析，答错写入错题本。
4. 重新选择答案会重新判分；同一题在一次练习中只累计一次错误次数。
5. `POST /api/v1/exams/{recordId}/submit` 结束练习并生成成绩。

### 限时套卷

1. `POST /api/v1/exams` 传 `paperId`；试卷必须为 PUBLISHED 且至少一题。
2. 服务端创建 `exam_record`（EXAM），`deadline_at = started_at + duration_min`，逐题固化快照与分值。
3. 作答只保存不判题；离截止时间超过 60 秒后保存答案会触发服务端自动判分并返回 `50009`。
4. 倒计时结束前端自动交卷；服务端在查询会话时发现已超时也会自动判分，避免用户关闭页面后记录悬挂。
5. `POST /api/v1/exams/{recordId}/submit` 幂等：已交卷再次提交直接返回成绩。

### 错题本

- 练习答错（非空答案）或考试已作答答错时自动写入/累计。
- 未作答不进入错题本。
- `PUT /api/v1/wrong-questions/{id}/mastered` 标记掌握/恢复待攻克。
- `DELETE /api/v1/wrong-questions/{id}` 移除记录（删除掌握跟踪，不删除题目）。
- 错题重做答对自动标记掌握；再次答错重新置为待攻克并累计次数。

## 五、接口

### 学员

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/practice/overview` | 科目/章节统计、已发布试卷、我的统计、最近记录 |
| POST | `/api/v1/practice/sessions` | 开始章节练习 / 错题重做 |
| POST | `/api/v1/exams` | 开始限时套卷 |
| GET | `/api/v1/exams/{recordId}` | 获取答题会话（含题目、已保存答案、剩余秒数） |
| PUT | `/api/v1/exams/{recordId}/answers` | 保存答案；练习模式返回即时判题结果 |
| POST | `/api/v1/exams/{recordId}/submit` | 交卷判分（幂等） |
| GET | `/api/v1/exams/{recordId}/result` | 成绩报告 |
| GET | `/api/v1/exams/records` | 我的练习/考试记录（分页） |
| GET | `/api/v1/wrong-questions` | 错题本（按科目/掌握状态筛选、分页） |
| PUT | `/api/v1/wrong-questions/{id}/mastered` | 标记掌握 |
| DELETE | `/api/v1/wrong-questions/{id}` | 移除错题 |

### 管理端

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | `/api/v1/admin/questions`、`/{id}` | `question:browse` |
| POST | `/api/v1/admin/questions` | `question:create` |
| PUT | `/api/v1/admin/questions/{id}` | `question:edit` |
| PUT | `/api/v1/admin/questions/{id}/status` | `question:delete` |
| GET | `/api/v1/admin/papers`、`/{id}` | `paper:browse` |
| POST | `/api/v1/admin/papers` | `paper:create` |
| PUT | `/api/v1/admin/papers/{id}` | `paper:edit` |
| PUT | `/api/v1/admin/papers/{id}/status` | `paper:publish` |
| DELETE | `/api/v1/admin/papers/{id}` | `paper:delete` |

## 六、权限与数据范围

- 权限点：`question:browse/create/edit/delete`、`paper:browse/create/edit/delete/publish`，菜单 `/admin/questions`、`/admin/papers`。
- 启动时 `PrivilegeSyncService` 同步注册；`V15` 为 ADMIN 授予全部，为 TEACHER 授予题库/试卷权限。
- `DataScopeService`：ADMIN（ALL）不限；TEACHER（OWN）只能看到 `owner_user_id = 当前用户` 的题目/试卷，修改他人数据返回 403。
- 学员端接口仅要求登录，不额外要求后台权限。

## 七、边界与约定

- 页面 `/practice`、`/practice/session/:id`、`/practice/result/:id`、`/wrong-book` 均要求登录；游客跳转登录页。
- 题目被任意试卷引用（含草稿/下架）时不允许停用，避免组卷引用失效；需先解除试卷引用。
- 已发布试卷下架后不再出现在题库首页，也不能开始新考试；已有成绩与快照不受影响。
- 试卷组卷只能选择 ACTIVE 题目；修改试卷题目列表采用整组覆盖并重算总分。
- 题库/考试读写均走主库（服务类标注 `@Master`），保证后台新建题目、开考、判分、错题状态立即可见，不依赖主从复制延迟。
- 时间字段统一 UTC `LocalDateTime` 写入 `DATETIME2`；前端通过 `parseUtc` 转本地展示与倒计时。
- 删除策略：题目/试卷用状态停用，不物理删除；`paper_question` 在修改试卷时整组覆盖；重置脚本会清理冒烟数据。

## 八、验证

```bash
# API 全链路
node scripts/practice-smoke.mjs

# 浏览器 UI 全链路（需后端 8080、前端 5173 已启动）
node frontend/scripts/practice-ui-smoke.cjs

# 清理练习/考试数据与冒烟创建的题目、试卷
sqlcmd -S localhost -C -E -b -I -f 65001 -d smart_learning -i scripts/reset-practice-data.sql
```

冒烟覆盖：

- 后台建题（单选/多选/判断）、编辑、组卷、发布、下架、停用保护。
- 练习即时判题（正确/错误/多选部分选对）、非法选项拒绝。
- 错题自动归档、标记掌握、不限科目重做、重做答对自动掌握。
- 练习与套卷成绩、未作答口径、成绩快照、记录分页。
- 用户隔离（他人记录 50007）、学员后台 403、讲师 OWN 数据范围、讲师越权 403。
- UI：题库首页、章节练习、即时判题、成绩报告、错题本、个人中心、限时考试、后台题库/试卷弹窗、游客跳登录。

单元测试：`src/test/java/com/example/smart_learning/exam/service/QuestionGraderTest.java`。

## 九、演示数据

`V15__practice_seed.sql` 提供：

- 19 道题目：政治（马原/毛中特）、英语一、数学一、计算机 408，覆盖单选/多选/判断。
- 6 套试卷：政治马原章节自测、政治基础阶段测试、英语阅读词汇专项、数学一基础自测、408 章节练习（均已发布），另有一套英语二草稿用于后台发布演示。
