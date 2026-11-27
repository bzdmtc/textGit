-- 内置权限分组的描述（独立迁移：SQL Server 同批 ADD 列后立即 UPDATE 会报列名无效）
UPDATE sys_group SET description = N'系统管理员分组，拥有全部后台权限，数据范围为全部数据。'
WHERE code = 'ADMIN' AND description IS NULL;

UPDATE sys_group SET description = N'讲师分组，维护自有课程、文章、资料、题目与试卷，数据范围为本人的数据。'
WHERE code = 'TEACHER' AND description IS NULL;

UPDATE sys_group SET description = N'学员分组，可购买课程、学习、做题、管理个人资料，数据范围仅本人。'
WHERE code = 'STUDENT' AND description IS NULL;

UPDATE sys_group SET description = N'受限用户分组，数据范围为本人的数据，用于演示受限分组。'
WHERE code = 'LIMITED' AND description IS NULL;
