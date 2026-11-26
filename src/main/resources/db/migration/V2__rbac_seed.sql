-- 系统底座种子数据
-- 演示账号：admin / Admin@123、teacher / Teacher@123、student / Student@123、teacher2 / Teacher@123

INSERT INTO sys_group (code, name, role, data_scope, limited, scope_type, scope_id, status, sort) VALUES
('ADMIN',   N'系统管理员', 'ADMIN',   'ALL',  0, 'GLOBAL', 0, 'ACTIVE', 10),
('TEACHER', N'讲师',       'TEACHER', 'OWN',  0, 'GLOBAL', 0, 'ACTIVE', 20),
('STUDENT', N'学员',       'STUDENT', 'SELF', 0, 'GLOBAL', 0, 'ACTIVE', 30),
('LIMITED', N'受限用户',   'LIMITED', 'OWN',  1, 'GLOBAL', 0, 'ACTIVE', 40);

INSERT INTO sys_dept (name, parent_id, sort) VALUES
(N'教学部', NULL, 10),
(N'运营部', NULL, 20);

INSERT INTO sys_user (account, password_hash, phone, email, realname, nickname, status, priv_version) VALUES
('admin',    '$2a$10$lN8xEvOxEGyU8f9u9CziGOHMY8kWGeWnzFN69eHeyKRjXkOWdypD2', NULL, NULL, N'系统管理员', N'管理员', 'ACTIVE', 0),
('teacher',  '$2a$10$QLqZ.LQRp5nnKLDMjQTH1OKacKTHeM9iBMjg/VtKDFU20MaI.Os8q', NULL, NULL, N'张老师', N'张老师', 'ACTIVE', 0),
('student',  '$2a$10$xJM6aBhkP/tk0odacWkJ2upnRGrK2WQnJAcN8qTTpatqWnQvzT1ie', '13800000000', 'student@example.com', N'李同学', N'小李', 'ACTIVE', 0),
('teacher2', '$2a$10$QLqZ.LQRp5nnKLDMjQTH1OKacKTHeM9iBMjg/VtKDFU20MaI.Os8q', NULL, NULL, N'王老师', N'王老师', 'ACTIVE', 0);

INSERT INTO sys_user_group (user_id, group_id)
SELECT u.id, g.id FROM sys_user u CROSS JOIN sys_group g
WHERE (u.account = 'admin' AND g.code = 'ADMIN')
   OR (u.account = 'teacher' AND g.code = 'TEACHER')
   OR (u.account = 'student' AND g.code = 'STUDENT')
   OR (u.account = 'teacher2' AND g.code IN ('TEACHER', 'STUDENT'));

INSERT INTO sys_priv (module, method, name, type, menu_path, icon, sort, status) VALUES
('admin',   'access',      N'后台访问',       'VIEW',   '/admin/dashboard', 'Setting',     10,  'ACTIVE'),
('manage',  'access',      N'管理端访问',     'VIEW',   '/teacher',         'Management',  20,  'ACTIVE'),
('user',    'browse',      N'用户列表',       'VIEW',   '/admin/users',     'User',        110, 'ACTIVE'),
('user',    'create',      N'新增用户',       'ACTION', NULL,               NULL,          111, 'ACTIVE'),
('user',    'edit',        N'编辑用户',       'ACTION', NULL,               NULL,          112, 'ACTIVE'),
('user',    'disable',     N'禁用/启用用户',  'ACTION', NULL,               NULL,          113, 'ACTIVE'),
('user',    'assign-group',N'分配用户分组',   'ACTION', NULL,               NULL,          114, 'ACTIVE'),
('group',   'browse',      N'权限分组列表',   'VIEW',   '/admin/groups',    'Connection',  120, 'ACTIVE'),
('group',   'create',      N'新增权限分组',   'ACTION', NULL,               NULL,          121, 'ACTIVE'),
('group',   'edit',        N'编辑权限分组',   'ACTION', NULL,               NULL,          122, 'ACTIVE'),
('group',   'delete',      N'删除权限分组',   'ACTION', NULL,               NULL,          123, 'ACTIVE'),
('group',   'assign-priv', N'分配分组权限',   'ACTION', NULL,               NULL,          124, 'ACTIVE'),
('priv',    'browse',      N'权限字典',       'VIEW',   NULL,               NULL,          130, 'ACTIVE'),
('audit',   'browse',      N'审计日志',       'VIEW',   '/admin/logs',      'Document',    140, 'ACTIVE'),
('profile', 'edit',        N'编辑个人资料',   'ACTION', NULL,               NULL,          200, 'ACTIVE');

-- 管理员拥有全部权限
INSERT INTO sys_group_priv (group_id, priv_id)
SELECT g.id, p.id FROM sys_group g CROSS JOIN sys_priv p WHERE g.code = 'ADMIN';

-- 讲师：管理端入口 + 个人资料
INSERT INTO sys_group_priv (group_id, priv_id)
SELECT g.id, p.id FROM sys_group g JOIN sys_priv p ON
    (p.module = 'manage' AND p.method = 'access')
 OR (p.module = 'profile' AND p.method = 'edit')
WHERE g.code = 'TEACHER';

-- 学员 / 受限用户：个人资料
INSERT INTO sys_group_priv (group_id, priv_id)
SELECT g.id, p.id FROM sys_group g JOIN sys_priv p ON p.module = 'profile' AND p.method = 'edit'
WHERE g.code IN ('STUDENT', 'LIMITED');
