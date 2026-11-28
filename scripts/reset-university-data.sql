-- 清理院校冒烟数据（SMOKE 前缀）与对应审计日志，保留 V21 演示院校
-- 用法：sqlcmd -S localhost -C -E -b -f 65001 -d smart_learning -i scripts\reset-university-data.sql

USE smart_learning;
SET NOCOUNT ON;

DELETE FROM admission_stat
WHERE major_id IN (
    SELECT m.id FROM university_major m
    JOIN university u ON u.id = m.university_id
    WHERE u.name LIKE N'SMOKE%' OR u.name LIKE N'UI测试院校%'
);

DELETE FROM score_line
WHERE major_id IN (
    SELECT m.id FROM university_major m
    JOIN university u ON u.id = m.university_id
    WHERE u.name LIKE N'SMOKE%' OR u.name LIKE N'UI测试院校%'
);

DELETE FROM university_major
WHERE university_id IN (SELECT id FROM university WHERE name LIKE N'SMOKE%' OR name LIKE N'UI测试院校%');

DELETE FROM university WHERE name LIKE N'SMOKE%' OR name LIKE N'UI测试院校%';

-- 院校模块审计仅由冒烟/后台维护产生，重置时一并清理（演示种子数据本身没有审计记录）
DELETE FROM sys_audit_log
WHERE target_type IN ('UNIVERSITY', 'UNIVERSITY_MAJOR', 'SCORE_LINE', 'ADMISSION_STAT');
