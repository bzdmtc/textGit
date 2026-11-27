-- 模块 4：秒杀权限点、管理员授权与演示活动

INSERT INTO sys_priv (module, method, name, type, menu_path, icon, sort, status)
SELECT * FROM (VALUES
    ('seckill', 'browse', N'秒杀活动管理', 'VIEW',   '/admin/seckill', 'Timer', 340, 'ACTIVE'),
    ('seckill', 'create', N'新增秒杀活动', 'ACTION', NULL,             NULL,    341, 'ACTIVE'),
    ('seckill', 'edit',   N'编辑/预热秒杀活动', 'ACTION', NULL,        NULL,    342, 'ACTIVE'),
    ('seckill', 'delete', N'删除/停用秒杀活动', 'ACTION', NULL,        NULL,    343, 'ACTIVE')
) AS v(module, method, name, type, menu_path, icon, sort, status)
WHERE NOT EXISTS (SELECT 1 FROM sys_priv p WHERE p.module = v.module AND p.method = v.method);

INSERT INTO sys_group_priv (group_id, priv_id)
SELECT g.id, p.id FROM sys_group g CROSS JOIN sys_priv p
WHERE g.code = 'ADMIN'
  AND p.module = 'seckill'
  AND NOT EXISTS (SELECT 1 FROM sys_group_priv gp WHERE gp.group_id = g.id AND gp.priv_id = p.id);

-- 演示活动：取前两个已上架课程，一个进行中、一个即将开始
INSERT INTO seckill_activity
    (course_id, seckill_price, total_stock, available_stock, per_user_limit, start_at, end_at, status)
SELECT id, CAST(price * 0.60 AS DECIMAL(10,2)), 50, 50, 1,
       DATEADD(HOUR, -1, SYSUTCDATETIME()), DATEADD(DAY, 7, SYSUTCDATETIME()), 'ON'
FROM course
WHERE status = 'PUBLISHED'
ORDER BY id
OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY;

INSERT INTO seckill_activity
    (course_id, seckill_price, total_stock, available_stock, per_user_limit, start_at, end_at, status)
SELECT id, CAST(price * 0.70 AS DECIMAL(10,2)), 30, 30, 1,
       DATEADD(DAY, 1, SYSUTCDATETIME()), DATEADD(DAY, 8, SYSUTCDATETIME()), 'ON'
FROM course
WHERE status = 'PUBLISHED'
ORDER BY id
OFFSET 1 ROWS FETCH NEXT 1 ROWS ONLY;
