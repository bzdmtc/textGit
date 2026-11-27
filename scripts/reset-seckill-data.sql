-- 开发环境：重置秒杀演示/冒烟数据（保留 V11 种子的演示活动 id 1、2）
-- 用法：sqlcmd -S localhost -C -E -b -I -f 65001 -d smart_learning -i scripts/reset-seckill-data.sql
-- （脚本含中文，必须加 -f 65001，否则 sqlcmd 按本地代码页读取会让整批语句不执行）
-- 会删除秒杀订单、支付流水、选课关系、抢购记录与临时测试用户，并恢复活动库存。

USE smart_learning;
SET NOCOUNT ON;

DELETE FROM refund_request
WHERE order_id IN (SELECT id FROM orders WHERE order_source = 'SECKILL');
DELETE FROM payment_record
WHERE order_no IN (SELECT order_no FROM orders WHERE order_source = 'SECKILL');
DELETE FROM user_course
WHERE order_id IN (SELECT id FROM orders WHERE order_source = 'SECKILL');
DELETE FROM order_item
WHERE order_id IN (SELECT id FROM orders WHERE order_source = 'SECKILL');
DELETE FROM orders WHERE order_source = 'SECKILL';

DELETE FROM seckill_record;
UPDATE seckill_activity SET available_stock = total_stock;
DELETE FROM seckill_activity WHERE id NOT IN (1, 2);

-- 恢复演示课程学习人数（与 reset-trade-data.sql 保持一致）
UPDATE course SET student_count = 1280 WHERE title = N'考研政治强化提升班';
UPDATE course SET student_count = 960  WHERE title = N'考研英语一基础精讲';
UPDATE course SET student_count = 1560 WHERE title = N'考研数学一强化班';
UPDATE course SET student_count = 680  WHERE title = N'计算机 408 专业课冲刺';

-- 清理并发冒烟临时用户
DELETE FROM sys_user_group
WHERE user_id IN (SELECT id FROM sys_user WHERE account LIKE 'conc_test_%');
DELETE FROM sys_audit_log
WHERE target_type = 'USER'
  AND target_id IN (SELECT id FROM sys_user WHERE account LIKE 'conc_test_%');
DELETE FROM refresh_token
WHERE user_id IN (SELECT id FROM sys_user WHERE account LIKE 'conc_test_%');
DELETE FROM sys_user WHERE account LIKE 'conc_test_%';

DBCC CHECKIDENT ('seckill_record', RESEED, 0);
DBCC CHECKIDENT ('seckill_activity', RESEED, 2);
