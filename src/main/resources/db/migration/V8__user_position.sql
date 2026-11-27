-- 模块 1 增强：用户职位（组织岗位信息，独立于权限分组，不参与鉴权）
ALTER TABLE sys_user ADD position NVARCHAR(64) NULL;
