-- 模块 6：题库考试权限点、演示题目与试卷

INSERT INTO sys_priv (module, method, name, type, menu_path, icon, sort, status)
SELECT * FROM (VALUES
    ('question', 'browse',  N'题库管理', 'VIEW',   '/admin/questions', 'EditPen', 360, 'ACTIVE'),
    ('question', 'create',  N'新增题目', 'ACTION', NULL,               NULL,      361, 'ACTIVE'),
    ('question', 'edit',    N'编辑题目', 'ACTION', NULL,               NULL,      362, 'ACTIVE'),
    ('question', 'delete',  N'停用题目', 'ACTION', NULL,               NULL,      363, 'ACTIVE'),
    ('paper',    'browse',  N'试卷管理', 'VIEW',   '/admin/papers',    'Tickets', 370, 'ACTIVE'),
    ('paper',    'create',  N'新增试卷', 'ACTION', NULL,               NULL,      371, 'ACTIVE'),
    ('paper',    'edit',    N'编辑试卷', 'ACTION', NULL,               NULL,      372, 'ACTIVE'),
    ('paper',    'delete',  N'停用试卷', 'ACTION', NULL,               NULL,      373, 'ACTIVE'),
    ('paper',    'publish', N'发布/下架试卷', 'ACTION', NULL,          NULL,      374, 'ACTIVE')
) AS v(module, method, name, type, menu_path, icon, sort, status)
WHERE NOT EXISTS (SELECT 1 FROM sys_priv p WHERE p.module = v.module AND p.method = v.method);

-- 管理员拥有全部题库权限
INSERT INTO sys_group_priv (group_id, priv_id)
SELECT g.id, p.id FROM sys_group g CROSS JOIN sys_priv p
WHERE g.code = 'ADMIN'
  AND p.module IN ('question', 'paper')
  AND NOT EXISTS (SELECT 1 FROM sys_group_priv gp WHERE gp.group_id = g.id AND gp.priv_id = p.id);

-- 讲师：维护自有题目与试卷（数据范围 OWN）
INSERT INTO sys_group_priv (group_id, priv_id)
SELECT g.id, p.id FROM sys_group g CROSS JOIN sys_priv p
WHERE g.code = 'TEACHER'
  AND p.module IN ('question', 'paper')
  AND NOT EXISTS (SELECT 1 FROM sys_group_priv gp WHERE gp.group_id = g.id AND gp.priv_id = p.id);

-- ============ 演示题目 ============
INSERT INTO question (subject, chapter, type, stem, analysis, difficulty, status, owner_user_id) VALUES
(N'政治', N'马克思主义基本原理', 'SINGLE', N'物质的唯一特性是（ ）。',
 N'物质的唯一特性是客观实在性；运动是物质的根本属性，可知性、多样性是物质的表现或属性。', 2, 'ACTIVE', 1),
(N'政治', N'马克思主义基本原理', 'SINGLE', N'下列不属于实践基本特征的是（ ）。',
 N'实践具有直接现实性、自觉能动性和社会历史性；抽象思辨性不属于实践的基本特征。', 2, 'ACTIVE', 1),
(N'政治', N'马克思主义基本原理', 'MULTIPLE', N'唯物辩证法的基本规律包括（ ）。',
 N'唯物辩证法三大基本规律是对立统一规律、质量互变规律、否定之否定规律；因果联系属于辩证法范畴，不是基本规律。', 3, 'ACTIVE', 1),
(N'政治', N'马克思主义基本原理', 'JUDGE', N'意识是物质的反映，意识的内容是客观的。（ ）',
 N'意识是人脑对客观存在的反映，其内容是客观的，形式是主观的。', 2, 'ACTIVE', 1),
(N'政治', N'毛泽东思想和中国特色社会主义理论体系', 'SINGLE', N'新民主主义革命的三大法宝不包括（ ）。',
 N'新民主主义革命的三大法宝是统一战线、武装斗争、党的建设；土地革命属于革命道路的内容。', 2, 'ACTIVE', 1),
(N'政治', N'毛泽东思想和中国特色社会主义理论体系', 'MULTIPLE', N'毛泽东思想活的灵魂包括（ ）。',
 N'毛泽东思想活的灵魂是实事求是、群众路线、独立自主。', 2, 'ACTIVE', 1),
(N'政治', N'毛泽东思想和中国特色社会主义理论体系', 'SINGLE', N'中国革命的基本问题是（ ）。',
 N'中国革命的基本问题是农民问题，农民问题是中国革命的核心内容。', 2, 'ACTIVE', 1),
(N'英语一', N'核心词汇与长难句', 'SINGLE', N'单词 “abandon” 的含义是（ ）。',
 N'abandon 意为“放弃、抛弃”，与 give up 意思相近。', 1, 'ACTIVE', 1),
(N'英语一', N'核心词汇与长难句', 'SINGLE', N'在句子 “The book which I bought yesterday is interesting.” 中，which 引导的是（ ）。',
 N'which 指代先行词 the book，并在从句中作 bought 的宾语，因此是定语从句。', 2, 'ACTIVE', 1),
(N'英语一', N'核心词汇与长难句', 'MULTIPLE', N'考研英语阅读中常见的题型包括（ ）。',
 N'主旨大意题、细节理解题、推理判断题是阅读常见题型；完形填空属于独立题型。', 2, 'ACTIVE', 1),
(N'英语一', N'核心词汇与长难句', 'JUDGE', N'“inspire” 与 “encourage” 都可以表示“鼓励、激励”。（ ）',
 N'inspire 侧重激发灵感或热情，encourage 侧重给予勇气和支持，二者都可译为鼓励、激励。', 1, 'ACTIVE', 1),
(N'数学一', N'高等数学强化', 'SINGLE', N'极限 lim(x→0) sin x / x 的值为（ ）。',
 N'这是第一重要极限，lim(x→0) sin x / x = 1。', 1, 'ACTIVE', 1),
(N'数学一', N'高等数学强化', 'SINGLE', N'函数 f(x)=x² 在 x=1 处的导数为（ ）。',
 N'f''(x)=2x，因此 f''(1)=2。', 1, 'ACTIVE', 1),
(N'数学一', N'高等数学强化', 'MULTIPLE', N'下列函数在 x=0 处连续的有（ ）。',
 N'sin x、|x|、x² 在 x=0 处均有定义且极限等于函数值；1/x 在 x=0 处无定义。', 2, 'ACTIVE', 1),
(N'数学一', N'高等数学强化', 'JUDGE', N'函数在某点可导，则它在该点一定连续。（ ）',
 N'可导必连续，但连续不一定可导，例如 |x| 在 x=0 处连续但不可导。', 2, 'ACTIVE', 1),
(N'计算机408', N'408 高频考点串讲', 'SINGLE', N'栈的存取特点是（ ）。',
 N'栈遵循后进先出（LIFO）原则，只能在栈顶进行插入和删除。', 1, 'ACTIVE', 1),
(N'计算机408', N'408 高频考点串讲', 'SINGLE', N'下列排序算法中，平均时间复杂度为 O(n log n) 的是（ ）。',
 N'快速排序平均时间复杂度为 O(n log n)；冒泡、选择、插入排序平均为 O(n²)。', 2, 'ACTIVE', 1),
(N'计算机408', N'408 高频考点串讲', 'MULTIPLE', N'操作系统实现进程同步的机制包括（ ）。',
 N'信号量、互斥锁、条件变量均可用于进程/线程同步；页表用于地址转换。', 3, 'ACTIVE', 1),
(N'计算机408', N'408 高频考点串讲', 'JUDGE', N'TCP 三次握手可以防止已失效的连接请求报文段突然又传到服务端。（ ）',
 N'三次握手能够确认双方的收发能力，并避免历史失效连接请求造成资源浪费。', 2, 'ACTIVE', 1);

-- ============ 题目选项 ============
INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'运动性', 0, 1 FROM question WHERE stem = N'物质的唯一特性是（ ）。'
UNION ALL SELECT id, N'B', N'客观实在性', 1, 2 FROM question WHERE stem = N'物质的唯一特性是（ ）。'
UNION ALL SELECT id, N'C', N'可知性', 0, 3 FROM question WHERE stem = N'物质的唯一特性是（ ）。'
UNION ALL SELECT id, N'D', N'多样性', 0, 4 FROM question WHERE stem = N'物质的唯一特性是（ ）。';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'直接现实性', 0, 1 FROM question WHERE stem = N'下列不属于实践基本特征的是（ ）。'
UNION ALL SELECT id, N'B', N'自觉能动性', 0, 2 FROM question WHERE stem = N'下列不属于实践基本特征的是（ ）。'
UNION ALL SELECT id, N'C', N'社会历史性', 0, 3 FROM question WHERE stem = N'下列不属于实践基本特征的是（ ）。'
UNION ALL SELECT id, N'D', N'抽象思辨性', 1, 4 FROM question WHERE stem = N'下列不属于实践基本特征的是（ ）。';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'对立统一规律', 1, 1 FROM question WHERE stem = N'唯物辩证法的基本规律包括（ ）。'
UNION ALL SELECT id, N'B', N'质量互变规律', 1, 2 FROM question WHERE stem = N'唯物辩证法的基本规律包括（ ）。'
UNION ALL SELECT id, N'C', N'否定之否定规律', 1, 3 FROM question WHERE stem = N'唯物辩证法的基本规律包括（ ）。'
UNION ALL SELECT id, N'D', N'因果规律', 0, 4 FROM question WHERE stem = N'唯物辩证法的基本规律包括（ ）。';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'正确', 1, 1 FROM question WHERE stem = N'意识是物质的反映，意识的内容是客观的。（ ）'
UNION ALL SELECT id, N'B', N'错误', 0, 2 FROM question WHERE stem = N'意识是物质的反映，意识的内容是客观的。（ ）';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'统一战线', 0, 1 FROM question WHERE stem = N'新民主主义革命的三大法宝不包括（ ）。'
UNION ALL SELECT id, N'B', N'武装斗争', 0, 2 FROM question WHERE stem = N'新民主主义革命的三大法宝不包括（ ）。'
UNION ALL SELECT id, N'C', N'党的建设', 0, 3 FROM question WHERE stem = N'新民主主义革命的三大法宝不包括（ ）。'
UNION ALL SELECT id, N'D', N'土地革命', 1, 4 FROM question WHERE stem = N'新民主主义革命的三大法宝不包括（ ）。';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'实事求是', 1, 1 FROM question WHERE stem = N'毛泽东思想活的灵魂包括（ ）。'
UNION ALL SELECT id, N'B', N'群众路线', 1, 2 FROM question WHERE stem = N'毛泽东思想活的灵魂包括（ ）。'
UNION ALL SELECT id, N'C', N'独立自主', 1, 3 FROM question WHERE stem = N'毛泽东思想活的灵魂包括（ ）。'
UNION ALL SELECT id, N'D', N'改革开放', 0, 4 FROM question WHERE stem = N'毛泽东思想活的灵魂包括（ ）。';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'农民问题', 1, 1 FROM question WHERE stem = N'中国革命的基本问题是（ ）。'
UNION ALL SELECT id, N'B', N'土地问题', 0, 2 FROM question WHERE stem = N'中国革命的基本问题是（ ）。'
UNION ALL SELECT id, N'C', N'政权问题', 0, 3 FROM question WHERE stem = N'中国革命的基本问题是（ ）。'
UNION ALL SELECT id, N'D', N'武装斗争问题', 0, 4 FROM question WHERE stem = N'中国革命的基本问题是（ ）。';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'放弃', 1, 1 FROM question WHERE stem = N'单词 “abandon” 的含义是（ ）。'
UNION ALL SELECT id, N'B', N'建立', 0, 2 FROM question WHERE stem = N'单词 “abandon” 的含义是（ ）。'
UNION ALL SELECT id, N'C', N'扩大', 0, 3 FROM question WHERE stem = N'单词 “abandon” 的含义是（ ）。'
UNION ALL SELECT id, N'D', N'承认', 0, 4 FROM question WHERE stem = N'单词 “abandon” 的含义是（ ）。';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'主语从句', 0, 1 FROM question WHERE stem = N'在句子 “The book which I bought yesterday is interesting.” 中，which 引导的是（ ）。'
UNION ALL SELECT id, N'B', N'定语从句', 1, 2 FROM question WHERE stem = N'在句子 “The book which I bought yesterday is interesting.” 中，which 引导的是（ ）。'
UNION ALL SELECT id, N'C', N'宾语从句', 0, 3 FROM question WHERE stem = N'在句子 “The book which I bought yesterday is interesting.” 中，which 引导的是（ ）。'
UNION ALL SELECT id, N'D', N'状语从句', 0, 4 FROM question WHERE stem = N'在句子 “The book which I bought yesterday is interesting.” 中，which 引导的是（ ）。';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'主旨大意题', 1, 1 FROM question WHERE stem = N'考研英语阅读中常见的题型包括（ ）。'
UNION ALL SELECT id, N'B', N'细节理解题', 1, 2 FROM question WHERE stem = N'考研英语阅读中常见的题型包括（ ）。'
UNION ALL SELECT id, N'C', N'推理判断题', 1, 3 FROM question WHERE stem = N'考研英语阅读中常见的题型包括（ ）。'
UNION ALL SELECT id, N'D', N'完形填空题', 0, 4 FROM question WHERE stem = N'考研英语阅读中常见的题型包括（ ）。';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'正确', 1, 1 FROM question WHERE stem = N'“inspire” 与 “encourage” 都可以表示“鼓励、激励”。（ ）'
UNION ALL SELECT id, N'B', N'错误', 0, 2 FROM question WHERE stem = N'“inspire” 与 “encourage” 都可以表示“鼓励、激励”。（ ）';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'0', 0, 1 FROM question WHERE stem = N'极限 lim(x→0) sin x / x 的值为（ ）。'
UNION ALL SELECT id, N'B', N'1', 1, 2 FROM question WHERE stem = N'极限 lim(x→0) sin x / x 的值为（ ）。'
UNION ALL SELECT id, N'C', N'无穷大', 0, 3 FROM question WHERE stem = N'极限 lim(x→0) sin x / x 的值为（ ）。'
UNION ALL SELECT id, N'D', N'不存在', 0, 4 FROM question WHERE stem = N'极限 lim(x→0) sin x / x 的值为（ ）。';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'1', 0, 1 FROM question WHERE stem = N'函数 f(x)=x² 在 x=1 处的导数为（ ）。'
UNION ALL SELECT id, N'B', N'2', 1, 2 FROM question WHERE stem = N'函数 f(x)=x² 在 x=1 处的导数为（ ）。'
UNION ALL SELECT id, N'C', N'0', 0, 3 FROM question WHERE stem = N'函数 f(x)=x² 在 x=1 处的导数为（ ）。'
UNION ALL SELECT id, N'D', N'1/2', 0, 4 FROM question WHERE stem = N'函数 f(x)=x² 在 x=1 处的导数为（ ）。';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'sin x', 1, 1 FROM question WHERE stem = N'下列函数在 x=0 处连续的有（ ）。'
UNION ALL SELECT id, N'B', N'|x|', 1, 2 FROM question WHERE stem = N'下列函数在 x=0 处连续的有（ ）。'
UNION ALL SELECT id, N'C', N'1/x', 0, 3 FROM question WHERE stem = N'下列函数在 x=0 处连续的有（ ）。'
UNION ALL SELECT id, N'D', N'x²', 1, 4 FROM question WHERE stem = N'下列函数在 x=0 处连续的有（ ）。';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'正确', 1, 1 FROM question WHERE stem = N'函数在某点可导，则它在该点一定连续。（ ）'
UNION ALL SELECT id, N'B', N'错误', 0, 2 FROM question WHERE stem = N'函数在某点可导，则它在该点一定连续。（ ）';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'先进先出', 0, 1 FROM question WHERE stem = N'栈的存取特点是（ ）。'
UNION ALL SELECT id, N'B', N'后进先出', 1, 2 FROM question WHERE stem = N'栈的存取特点是（ ）。'
UNION ALL SELECT id, N'C', N'随机存取', 0, 3 FROM question WHERE stem = N'栈的存取特点是（ ）。'
UNION ALL SELECT id, N'D', N'双端插入', 0, 4 FROM question WHERE stem = N'栈的存取特点是（ ）。';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'冒泡排序', 0, 1 FROM question WHERE stem = N'下列排序算法中，平均时间复杂度为 O(n log n) 的是（ ）。'
UNION ALL SELECT id, N'B', N'快速排序', 1, 2 FROM question WHERE stem = N'下列排序算法中，平均时间复杂度为 O(n log n) 的是（ ）。'
UNION ALL SELECT id, N'C', N'选择排序', 0, 3 FROM question WHERE stem = N'下列排序算法中，平均时间复杂度为 O(n log n) 的是（ ）。'
UNION ALL SELECT id, N'D', N'插入排序', 0, 4 FROM question WHERE stem = N'下列排序算法中，平均时间复杂度为 O(n log n) 的是（ ）。';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'信号量', 1, 1 FROM question WHERE stem = N'操作系统实现进程同步的机制包括（ ）。'
UNION ALL SELECT id, N'B', N'互斥锁', 1, 2 FROM question WHERE stem = N'操作系统实现进程同步的机制包括（ ）。'
UNION ALL SELECT id, N'C', N'条件变量', 1, 3 FROM question WHERE stem = N'操作系统实现进程同步的机制包括（ ）。'
UNION ALL SELECT id, N'D', N'页表', 0, 4 FROM question WHERE stem = N'操作系统实现进程同步的机制包括（ ）。';

INSERT INTO question_option (question_id, label, content, is_correct, sort)
SELECT id, N'A', N'正确', 1, 1 FROM question WHERE stem = N'TCP 三次握手可以防止已失效的连接请求报文段突然又传到服务端。（ ）'
UNION ALL SELECT id, N'B', N'错误', 0, 2 FROM question WHERE stem = N'TCP 三次握手可以防止已失效的连接请求报文段突然又传到服务端。（ ）';

-- ============ 演示试卷 ============
INSERT INTO paper (title, type, subject, chapter, description, total_score, duration_min, status, owner_user_id) VALUES
(N'政治 · 马原章节自测', 'CHAPTER', N'政治', N'马克思主义基本原理', N'马原基础概念与辩证法专项自测，适合学完第一章后练习。', 30, 15, 'PUBLISHED', 1),
(N'考研政治基础阶段测试', 'MOCK', N'政治', NULL, N'覆盖马原与毛中特的基础阶段模拟测试。', 70, 30, 'PUBLISHED', 1),
(N'考研英语阅读与词汇专项', 'PAST', N'英语一', N'核心词汇与长难句', N'词汇、长难句与阅读题型专项训练。', 30, 12, 'PUBLISHED', 1),
(N'考研数学一基础自测', 'MOCK', N'数学一', NULL, N'高等数学基础概念与连续可导性自测。', 50, 20, 'PUBLISHED', 1),
(N'408 数据结构章节练习', 'CHAPTER', N'计算机408', N'408 高频考点串讲', N'栈、排序与操作系统同步机制基础练习。', 30, 10, 'PUBLISHED', 1),
(N'考研英语二冲刺模拟卷（草稿）', 'MOCK', N'英语二', NULL, N'草稿示例：供后台演示编辑与发布流程。', 25, 15, 'DRAFT', 1);

INSERT INTO paper_question (paper_id, question_id, score, sort)
SELECT p.id, q.id, 5, 1 FROM paper p JOIN question q ON q.stem = N'物质的唯一特性是（ ）。' WHERE p.title = N'政治 · 马原章节自测'
UNION ALL SELECT p.id, q.id, 5, 2 FROM paper p JOIN question q ON q.stem = N'下列不属于实践基本特征的是（ ）。' WHERE p.title = N'政治 · 马原章节自测'
UNION ALL SELECT p.id, q.id, 10, 3 FROM paper p JOIN question q ON q.stem = N'唯物辩证法的基本规律包括（ ）。' WHERE p.title = N'政治 · 马原章节自测'
UNION ALL SELECT p.id, q.id, 10, 4 FROM paper p JOIN question q ON q.stem = N'意识是物质的反映，意识的内容是客观的。（ ）' WHERE p.title = N'政治 · 马原章节自测';

INSERT INTO paper_question (paper_id, question_id, score, sort)
SELECT p.id, q.id, 10, 1 FROM paper p JOIN question q ON q.stem = N'物质的唯一特性是（ ）。' WHERE p.title = N'考研政治基础阶段测试'
UNION ALL SELECT p.id, q.id, 10, 2 FROM paper p JOIN question q ON q.stem = N'下列不属于实践基本特征的是（ ）。' WHERE p.title = N'考研政治基础阶段测试'
UNION ALL SELECT p.id, q.id, 10, 3 FROM paper p JOIN question q ON q.stem = N'唯物辩证法的基本规律包括（ ）。' WHERE p.title = N'考研政治基础阶段测试'
UNION ALL SELECT p.id, q.id, 10, 4 FROM paper p JOIN question q ON q.stem = N'意识是物质的反映，意识的内容是客观的。（ ）' WHERE p.title = N'考研政治基础阶段测试'
UNION ALL SELECT p.id, q.id, 10, 5 FROM paper p JOIN question q ON q.stem = N'新民主主义革命的三大法宝不包括（ ）。' WHERE p.title = N'考研政治基础阶段测试'
UNION ALL SELECT p.id, q.id, 10, 6 FROM paper p JOIN question q ON q.stem = N'毛泽东思想活的灵魂包括（ ）。' WHERE p.title = N'考研政治基础阶段测试'
UNION ALL SELECT p.id, q.id, 10, 7 FROM paper p JOIN question q ON q.stem = N'中国革命的基本问题是（ ）。' WHERE p.title = N'考研政治基础阶段测试';

INSERT INTO paper_question (paper_id, question_id, score, sort)
SELECT p.id, q.id, 5, 1 FROM paper p JOIN question q ON q.stem = N'单词 “abandon” 的含义是（ ）。' WHERE p.title = N'考研英语阅读与词汇专项'
UNION ALL SELECT p.id, q.id, 10, 2 FROM paper p JOIN question q ON q.stem = N'在句子 “The book which I bought yesterday is interesting.” 中，which 引导的是（ ）。' WHERE p.title = N'考研英语阅读与词汇专项'
UNION ALL SELECT p.id, q.id, 10, 3 FROM paper p JOIN question q ON q.stem = N'考研英语阅读中常见的题型包括（ ）。' WHERE p.title = N'考研英语阅读与词汇专项'
UNION ALL SELECT p.id, q.id, 5, 4 FROM paper p JOIN question q ON q.stem = N'“inspire” 与 “encourage” 都可以表示“鼓励、激励”。（ ）' WHERE p.title = N'考研英语阅读与词汇专项';

INSERT INTO paper_question (paper_id, question_id, score, sort)
SELECT p.id, q.id, 10, 1 FROM paper p JOIN question q ON q.stem = N'极限 lim(x→0) sin x / x 的值为（ ）。' WHERE p.title = N'考研数学一基础自测'
UNION ALL SELECT p.id, q.id, 10, 2 FROM paper p JOIN question q ON q.stem = N'函数 f(x)=x² 在 x=1 处的导数为（ ）。' WHERE p.title = N'考研数学一基础自测'
UNION ALL SELECT p.id, q.id, 15, 3 FROM paper p JOIN question q ON q.stem = N'下列函数在 x=0 处连续的有（ ）。' WHERE p.title = N'考研数学一基础自测'
UNION ALL SELECT p.id, q.id, 15, 4 FROM paper p JOIN question q ON q.stem = N'函数在某点可导，则它在该点一定连续。（ ）' WHERE p.title = N'考研数学一基础自测';

INSERT INTO paper_question (paper_id, question_id, score, sort)
SELECT p.id, q.id, 5, 1 FROM paper p JOIN question q ON q.stem = N'栈的存取特点是（ ）。' WHERE p.title = N'408 数据结构章节练习'
UNION ALL SELECT p.id, q.id, 10, 2 FROM paper p JOIN question q ON q.stem = N'下列排序算法中，平均时间复杂度为 O(n log n) 的是（ ）。' WHERE p.title = N'408 数据结构章节练习'
UNION ALL SELECT p.id, q.id, 10, 3 FROM paper p JOIN question q ON q.stem = N'操作系统实现进程同步的机制包括（ ）。' WHERE p.title = N'408 数据结构章节练习'
UNION ALL SELECT p.id, q.id, 5, 4 FROM paper p JOIN question q ON q.stem = N'TCP 三次握手可以防止已失效的连接请求报文段突然又传到服务端。（ ）' WHERE p.title = N'408 数据结构章节练习';

INSERT INTO paper_question (paper_id, question_id, score, sort)
SELECT p.id, q.id, 5, 1 FROM paper p JOIN question q ON q.stem = N'单词 “abandon” 的含义是（ ）。' WHERE p.title = N'考研英语二冲刺模拟卷（草稿）'
UNION ALL SELECT p.id, q.id, 10, 2 FROM paper p JOIN question q ON q.stem = N'在句子 “The book which I bought yesterday is interesting.” 中，which 引导的是（ ）。' WHERE p.title = N'考研英语二冲刺模拟卷（草稿）'
UNION ALL SELECT p.id, q.id, 10, 3 FROM paper p JOIN question q ON q.stem = N'考研英语阅读中常见的题型包括（ ）。' WHERE p.title = N'考研英语二冲刺模拟卷（草稿）';
