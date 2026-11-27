-- 权限分组描述（对齐禅道权限分组列表：ID/分组名称/分组描述/用户列表/操作）
ALTER TABLE sys_group ADD description NVARCHAR(500) NULL;
