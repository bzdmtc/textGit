-- 职位字典：职位绑定默认权限分组（对齐禅道「选职位自动带出权限分组」）
CREATE TABLE sys_position (
    id         BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sys_position PRIMARY KEY,
    name       NVARCHAR(64) NOT NULL,
    group_id   BIGINT       NOT NULL,
    status     NVARCHAR(16) NOT NULL CONSTRAINT df_sys_position_status DEFAULT 'ACTIVE', -- ACTIVE / DISABLED
    sort       INT          NOT NULL CONSTRAINT df_sys_position_sort DEFAULT 0,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_sys_position_created DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_sys_position_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ux_sys_position_name UNIQUE (name),
    CONSTRAINT ck_sys_position_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT fk_sys_position_group FOREIGN KEY (group_id) REFERENCES sys_group(id)
);

CREATE INDEX ix_sys_position_status_sort ON sys_position(status, sort);
CREATE INDEX ix_sys_position_group ON sys_position(group_id);
