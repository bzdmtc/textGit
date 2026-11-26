-- 模块 3：交易权限 + 优惠券种子数据

INSERT INTO sys_priv (module, method, name, type, menu_path, icon, sort, status)
SELECT * FROM (VALUES
    ('order',  'browse',  N'订单管理', 'VIEW',   '/admin/orders',  'List',         310, 'ACTIVE'),
    ('refund', 'browse',  N'退款审核', 'VIEW',   '/admin/refunds', 'RefreshRight', 320, 'ACTIVE'),
    ('refund', 'approve', N'处理退款', 'ACTION', NULL,             NULL,           321, 'ACTIVE'),
    ('coupon', 'browse',  N'优惠券管理', 'VIEW', '/admin/coupons', 'Ticket',       330, 'ACTIVE'),
    ('coupon', 'create',  N'新增优惠券', 'ACTION', NULL,           NULL,           331, 'ACTIVE'),
    ('coupon', 'edit',    N'编辑优惠券', 'ACTION', NULL,           NULL,           332, 'ACTIVE'),
    ('coupon', 'delete',  N'停用优惠券', 'ACTION', NULL,           NULL,           333, 'ACTIVE')
) AS v(module, method, name, type, menu_path, icon, sort, status)
WHERE NOT EXISTS (SELECT 1 FROM sys_priv p WHERE p.module = v.module AND p.method = v.method);

-- 管理员自动补齐新权限
INSERT INTO sys_group_priv (group_id, priv_id)
SELECT g.id, p.id FROM sys_group g CROSS JOIN sys_priv p
WHERE g.code = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM sys_group_priv gp WHERE gp.group_id = g.id AND gp.priv_id = p.id);

INSERT INTO coupon (name, type, value_amount, min_amount, total_quantity, remaining_quantity,
                    per_user_limit, start_at, end_at, subject, description, status)
VALUES
(N'新人专享 · 满 500 减 50', 'FIXED', 50.00, 500.00, 1000, 1000, 1,
 DATEADD(DAY, -1, SYSUTCDATETIME()), DATEADD(DAY, 365, SYSUTCDATETIME()), NULL,
 N'全站课程通用，单笔订单限用一张', 'ON'),
(N'数学专享 · 9 折', 'PERCENT', 90.00, 0.00, 500, 500, 1,
 DATEADD(DAY, -1, SYSUTCDATETIME()), DATEADD(DAY, 180, SYSUTCDATETIME()), N'数学一',
 N'仅限数学一课程，9 折优惠（支付原价的 90%）', 'ON'),
(N'政治强化 · 满 300 减 30', 'FIXED', 30.00, 300.00, 300, 300, 1,
 DATEADD(DAY, -1, SYSUTCDATETIME()), DATEADD(DAY, 120, SYSUTCDATETIME()), N'政治',
 N'政治类课程通用，可与促销价叠加使用', 'ON');
