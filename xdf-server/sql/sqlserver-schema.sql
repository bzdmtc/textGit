-- sql server 建表脚本
-- 数据库：xdf
-- 执行方式：
-- sqlcmd -S 127.0.0.1,1433 -U xdf_user -P 你的密码 -C -d xdf -f 65001 -i sqlserver-schema.sql
--
-- 注意：sql server 不支持 mysql 的 on update current_timestamp。
-- update_time 先使用默认值，后续可以在 java 代码里用 mybatis-plus 的 MetaObjectHandler 维护。

use xdf;
go
set nocount on;
go

-- 按依赖顺序删除旧表
if object_id(N'dbo.sys_role_permission', N'U') is not null drop table dbo.sys_role_permission;
go
if object_id(N'dbo.sys_user_role', N'U') is not null drop table dbo.sys_user_role;
go
if object_id(N'dbo.sys_permission', N'U') is not null drop table dbo.sys_permission;
go
if object_id(N'dbo.sys_role', N'U') is not null drop table dbo.sys_role;
go
if object_id(N'dbo.sys_user', N'U') is not null drop table dbo.sys_user;
go

-- 用户表
create table dbo.sys_user (
    id bigint identity(1,1) not null,                  -- 用户id
    username nvarchar(50) not null,                    -- 登录名
    password nvarchar(100) not null,                   -- bcrypt 密码哈希
    nickname nvarchar(50) null,                        -- 昵称
    avatar nvarchar(255) null,                         -- 头像url
    email nvarchar(100) null,                          -- 邮箱
    phone nvarchar(20) null,                           -- 手机号
    status tinyint not null constraint df_sys_user_status default 1,          -- 1启用 0禁用
    deleted tinyint not null constraint df_sys_user_deleted default 0,        -- 逻辑删除 0正常 1已删
    create_time datetime2 not null constraint df_sys_user_create_time default sysdatetime(),
    update_time datetime2 not null constraint df_sys_user_update_time default sysdatetime(),
    constraint pk_sys_user primary key (id),
    constraint uk_sys_user_username unique (username)
);
go
create index idx_sys_user_phone on dbo.sys_user(phone);
go

-- 角色表
create table dbo.sys_role (
    id bigint identity(1,1) not null,                  -- 角色id
    role_code nvarchar(50) not null,                   -- 角色编码 admin/teacher/student
    role_name nvarchar(50) not null,                   -- 角色名称
    description nvarchar(255) null,                    -- 描述
    status tinyint not null constraint df_sys_role_status default 1,          -- 1启用 0禁用
    create_time datetime2 not null constraint df_sys_role_create_time default sysdatetime(),
    constraint pk_sys_role primary key (id),
    constraint uk_sys_role_role_code unique (role_code)
);
go

-- 权限表
create table dbo.sys_permission (
    id bigint identity(1,1) not null,                  -- 权限id
    parent_id bigint not null constraint df_sys_permission_parent default 0,  -- 父权限id，0表示顶级
    perm_code nvarchar(100) not null,                  -- 权限标识，如 course:add
    perm_name nvarchar(50) not null,                   -- 权限名称
    type tinyint not null constraint df_sys_permission_type default 1,        -- 1菜单 2按钮 3接口
    path nvarchar(200) null,                           -- 前端路由或接口路径
    sort int not null constraint df_sys_permission_sort default 0,            -- 排序
    status tinyint not null constraint df_sys_permission_status default 1,    -- 1启用 0禁用
    create_time datetime2 not null constraint df_sys_permission_create_time default sysdatetime(),
    constraint pk_sys_permission primary key (id),
    constraint uk_sys_permission_perm_code unique (perm_code)
);
go
create index idx_sys_permission_parent on dbo.sys_permission(parent_id);
go

-- 用户角色关联表
create table dbo.sys_user_role (
    user_id bigint not null,
    role_id bigint not null,
    constraint pk_sys_user_role primary key (user_id, role_id)
);
go
create index idx_sys_user_role_role on dbo.sys_user_role(role_id);
go

-- 角色权限关联表
create table dbo.sys_role_permission (
    role_id bigint not null,
    permission_id bigint not null,
    constraint pk_sys_role_permission primary key (role_id, permission_id)
);
go
create index idx_sys_role_permission_permission on dbo.sys_role_permission(permission_id);
go

-- ============================================================
-- 种子数据
-- ============================================================

-- 角色
set identity_insert dbo.sys_role on;
insert into dbo.sys_role (id, role_code, role_name, description) values
(1, N'ADMIN', N'管理员', N'拥有全部权限'),
(2, N'TEACHER', N'教师', N'管理自己的课程和订单'),
(3, N'STUDENT', N'学生', N'浏览和购买课程');
set identity_insert dbo.sys_role off;
go

-- 菜单权限
set identity_insert dbo.sys_permission on;
insert into dbo.sys_permission (id, parent_id, perm_code, perm_name, type, path, sort) values
(100, 0, N'menu:course', N'课程管理', 1, N'/course', 1),
(101, 0, N'menu:user', N'用户管理', 1, N'/user', 2),
(102, 0, N'menu:order', N'订单管理', 1, N'/order', 3);
go

-- 按钮/接口权限
insert into dbo.sys_permission (id, parent_id, perm_code, perm_name, type, path, sort) values
(1, 100, N'course:view', N'查看课程', 2, null, 1),
(2, 100, N'course:add', N'新增课程', 2, null, 2),
(3, 100, N'course:edit', N'编辑课程', 2, null, 3),
(4, 100, N'course:delete', N'删除课程', 2, null, 4),
(5, 101, N'user:view', N'查看用户', 2, null, 1),
(6, 101, N'user:manage', N'管理用户', 2, null, 2),
(7, 102, N'order:view', N'查看订单', 2, null, 1),
(8, 0, N'system:manage', N'系统管理', 2, null, 9);
set identity_insert dbo.sys_permission off;
go

-- 角色权限
insert into dbo.sys_role_permission (role_id, permission_id) values
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8),
(1, 100), (1, 101), (1, 102);

insert into dbo.sys_role_permission (role_id, permission_id) values
(2, 1), (2, 2), (2, 3), (2, 7), (2, 100), (2, 102);

insert into dbo.sys_role_permission (role_id, permission_id) values
(3, 1), (3, 100);
go

-- 用户（密码占位，第 5 章生成 bcrypt 哈希后 update）
set identity_insert dbo.sys_user on;
insert into dbo.sys_user (id, username, password, nickname, status) values
(1, N'admin', N'TO_BE_GENERATED', N'系统管理员', 1),
(2, N'teacher01', N'TO_BE_GENERATED', N'王老师', 1),
(3, N'student01', N'TO_BE_GENERATED', N'小明', 1);
set identity_insert dbo.sys_user off;
go

-- 用户角色
insert into dbo.sys_user_role (user_id, role_id) values
(1, 1),
(2, 2),
(3, 3);
go

-- ============================================================
-- 验证
-- ============================================================
select 'sys_user' as table_name, count(*) as row_count from dbo.sys_user
union all
select 'sys_role', count(*) from dbo.sys_role
union all
select 'sys_permission', count(*) from dbo.sys_permission
union all
select 'sys_user_role', count(*) from dbo.sys_user_role
union all
select 'sys_role_permission', count(*) from dbo.sys_role_permission;
go
