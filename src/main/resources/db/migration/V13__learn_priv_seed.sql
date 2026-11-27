-- 模块 5：评价审核权限点与管理员授权

INSERT INTO sys_priv (module, method, name, type, menu_path, icon, sort, status)
SELECT * FROM (VALUES
    ('review', 'browse',  N'评价审核', 'VIEW',   '/admin/reviews', 'ChatDotRound', 350, 'ACTIVE'),
    ('review', 'approve', N'审核评价', 'ACTION', NULL,             NULL,           351, 'ACTIVE')
) AS v(module, method, name, type, menu_path, icon, sort, status)
WHERE NOT EXISTS (SELECT 1 FROM sys_priv p WHERE p.module = v.module AND p.method = v.method);

INSERT INTO sys_group_priv (group_id, priv_id)
SELECT g.id, p.id FROM sys_group g CROSS JOIN sys_priv p
WHERE g.code = 'ADMIN'
  AND p.module = 'review'
  AND NOT EXISTS (SELECT 1 FROM sys_group_priv gp WHERE gp.group_id = g.id AND gp.priv_id = p.id);
