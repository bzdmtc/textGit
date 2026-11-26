-- 开发环境：重置交易演示/冒烟数据，恢复到干净的种子状态
-- 用法：sqlcmd -S localhost -C -E -d smart_learning -i scripts/reset-trade-data.sql
-- 注意：会删除全部订单、退款、支付流水、选课关系、用户优惠券，并恢复优惠券库存与课程学习人数。

USE smart_learning;
SET NOCOUNT ON;

DELETE FROM refund_request;
DELETE FROM payment_record;
DELETE FROM user_course;
DELETE FROM order_item;
DELETE FROM orders;
DELETE FROM user_coupon;

UPDATE coupon SET remaining_quantity = total_quantity;
DELETE FROM coupon
WHERE name IN (N'自动化测试券', N'定时任务测试券')
  AND NOT EXISTS (SELECT 1 FROM user_coupon uc WHERE uc.coupon_id = coupon.id);

UPDATE course SET student_count = 1280 WHERE title = N'考研政治强化提升班';
UPDATE course SET student_count = 960  WHERE title = N'考研英语一基础精讲';
UPDATE course SET student_count = 1560 WHERE title = N'考研数学一强化班';
UPDATE course SET student_count = 680  WHERE title = N'计算机 408 专业课冲刺';

DBCC CHECKIDENT ('refund_request', RESEED, 0);
DBCC CHECKIDENT ('payment_record', RESEED, 0);
DBCC CHECKIDENT ('user_course', RESEED, 0);
DBCC CHECKIDENT ('order_item', RESEED, 0);
DBCC CHECKIDENT ('orders', RESEED, 0);
DBCC CHECKIDENT ('user_coupon', RESEED, 0);
