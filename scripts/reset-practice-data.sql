-- 模块 6：重置练习/考试数据，并清理冒烟脚本创建的题目与试卷
-- 用法（需 -I 以启用 QUOTED_IDENTIFIER，用于删除带索引/约束的数据）：
--   sqlcmd -S localhost -C -E -b -I -f 65001 -d smart_learning -i scripts\reset-practice-data.sql

DELETE FROM exam_answer;
DELETE FROM exam_record;
DELETE FROM wrong_question;

-- 冒烟试卷引用的题目关系
DELETE pq FROM paper_question pq
JOIN paper p ON p.id = pq.paper_id
WHERE p.title LIKE N'SMOKE %' OR p.title LIKE N'UI测试-%';

-- 冒烟题目被其他试卷引用时先解除引用
DELETE pq FROM paper_question pq
JOIN question q ON q.id = pq.question_id
WHERE q.stem LIKE N'SMOKE %' OR q.subject LIKE N'自动化测试-%' OR q.subject LIKE N'UI测试-%';

DELETE FROM paper WHERE title LIKE N'SMOKE %' OR title LIKE N'UI测试-%';

DELETE qo FROM question_option qo
JOIN question q ON q.id = qo.question_id
WHERE q.stem LIKE N'SMOKE %' OR q.subject LIKE N'自动化测试-%' OR q.subject LIKE N'UI测试-%';

DELETE FROM question
WHERE stem LIKE N'SMOKE %' OR subject LIKE N'自动化测试-%' OR subject LIKE N'UI测试-%';

PRINT 'practice data reset';
