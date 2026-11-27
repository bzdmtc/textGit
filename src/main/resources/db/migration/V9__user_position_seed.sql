-- 模块 1 增强：演示账号职位（独立迁移，确保 position 列已提交后再写入）
UPDATE sys_user SET position = N'系统管理员' WHERE account = 'admin';
UPDATE sys_user SET position = N'考研政治讲师' WHERE account = 'teacher';
UPDATE sys_user SET position = N'学员' WHERE account IN ('student', 'teacher2');
