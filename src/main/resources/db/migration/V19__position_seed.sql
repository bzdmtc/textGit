-- 职位字典种子：名称与现有演示用户的 position 保持一致
INSERT INTO sys_position (name, group_id, status, sort)
SELECT N'系统管理员', id, 'ACTIVE', 10 FROM sys_group WHERE code = 'ADMIN';

INSERT INTO sys_position (name, group_id, status, sort)
SELECT N'讲师', id, 'ACTIVE', 20 FROM sys_group WHERE code = 'TEACHER';

INSERT INTO sys_position (name, group_id, status, sort)
SELECT N'考研政治讲师', id, 'ACTIVE', 21 FROM sys_group WHERE code = 'TEACHER';

INSERT INTO sys_position (name, group_id, status, sort)
SELECT N'学员', id, 'ACTIVE', 30 FROM sys_group WHERE code = 'STUDENT';

INSERT INTO sys_position (name, group_id, status, sort)
SELECT N'受限用户', id, 'ACTIVE', 40 FROM sys_group WHERE code = 'LIMITED';
