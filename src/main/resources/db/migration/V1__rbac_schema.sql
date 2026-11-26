-- 系统底座：禅道式权限分组 + 用户 + 刷新令牌
-- SQL Server 2025 / T-SQL

CREATE TABLE sys_user (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sys_user PRIMARY KEY,
    account         NVARCHAR(64)  NOT NULL,
    password_hash   NVARCHAR(100) NULL,
    phone           NVARCHAR(32)  NULL,
    email           NVARCHAR(128) NULL,
    realname        NVARCHAR(64)  NULL,
    nickname        NVARCHAR(64)  NULL,
    avatar          NVARCHAR(500) NULL,
    dept_id         BIGINT        NULL,
    status          NVARCHAR(32)  NOT NULL CONSTRAINT df_sys_user_status DEFAULT 'ACTIVE',
    priv_version    BIGINT        NOT NULL CONSTRAINT df_sys_user_priv_version DEFAULT 0,
    last_login_at   DATETIME2(3)  NULL,
    created_at      DATETIME2(3)  NOT NULL CONSTRAINT df_sys_user_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2(3)  NOT NULL CONSTRAINT df_sys_user_updated DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX ux_sys_user_account ON sys_user(account);
CREATE UNIQUE INDEX ux_sys_user_phone ON sys_user(phone) WHERE phone IS NOT NULL;
CREATE UNIQUE INDEX ux_sys_user_email ON sys_user(email) WHERE email IS NOT NULL;

CREATE TABLE sys_group (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sys_group PRIMARY KEY,
    code            NVARCHAR(64)  NOT NULL,
    name            NVARCHAR(128) NOT NULL,
    role            NVARCHAR(32)  NULL,
    data_scope      NVARCHAR(16)  NOT NULL CONSTRAINT df_sys_group_scope DEFAULT 'SELF',
    limited         BIT           NOT NULL CONSTRAINT df_sys_group_limited DEFAULT 0,
    scope_type      NVARCHAR(16)  NOT NULL CONSTRAINT df_sys_group_scope_type DEFAULT 'GLOBAL',
    scope_id        BIGINT        NOT NULL CONSTRAINT df_sys_group_scope_id DEFAULT 0,
    status          NVARCHAR(16)  NOT NULL CONSTRAINT df_sys_group_status DEFAULT 'ACTIVE',
    sort            INT           NOT NULL CONSTRAINT df_sys_group_sort DEFAULT 0,
    created_at      DATETIME2(3)  NOT NULL CONSTRAINT df_sys_group_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2(3)  NOT NULL CONSTRAINT df_sys_group_updated DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX ux_sys_group_code ON sys_group(code);
CREATE INDEX ix_sys_group_status_sort ON sys_group(status, sort);

CREATE TABLE sys_user_group (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sys_user_group PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    group_id        BIGINT NOT NULL,
    CONSTRAINT fk_user_group_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_user_group_group FOREIGN KEY (group_id) REFERENCES sys_group(id)
);

CREATE UNIQUE INDEX ux_sys_user_group ON sys_user_group(user_id, group_id);
CREATE INDEX ix_sys_user_group_group ON sys_user_group(group_id);

CREATE TABLE sys_priv (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sys_priv PRIMARY KEY,
    module          NVARCHAR(64)  NOT NULL,
    method          NVARCHAR(64)  NOT NULL,
    name            NVARCHAR(128) NOT NULL,
    type            NVARCHAR(16)  NOT NULL CONSTRAINT df_sys_priv_type DEFAULT 'ACTION',
    parent_id       BIGINT        NULL,
    menu_path       NVARCHAR(255) NULL,
    icon            NVARCHAR(64)  NULL,
    sort            INT           NOT NULL CONSTRAINT df_sys_priv_sort DEFAULT 0,
    status          NVARCHAR(16)  NOT NULL CONSTRAINT df_sys_priv_status DEFAULT 'ACTIVE',
    created_at      DATETIME2(3)  NOT NULL CONSTRAINT df_sys_priv_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2(3)  NOT NULL CONSTRAINT df_sys_priv_updated DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX ux_sys_priv_module_method ON sys_priv(module, method);
CREATE INDEX ix_sys_priv_status ON sys_priv(status);

CREATE TABLE sys_group_priv (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sys_group_priv PRIMARY KEY,
    group_id        BIGINT NOT NULL,
    priv_id         BIGINT NOT NULL,
    CONSTRAINT fk_group_priv_group FOREIGN KEY (group_id) REFERENCES sys_group(id),
    CONSTRAINT fk_group_priv_priv FOREIGN KEY (priv_id) REFERENCES sys_priv(id)
);

CREATE UNIQUE INDEX ux_sys_group_priv ON sys_group_priv(group_id, priv_id);
CREATE INDEX ix_sys_group_priv_group ON sys_group_priv(group_id);

CREATE TABLE sys_dept (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sys_dept PRIMARY KEY,
    name            NVARCHAR(128) NOT NULL,
    parent_id       BIGINT        NULL,
    sort            INT           NOT NULL CONSTRAINT df_sys_dept_sort DEFAULT 0,
    created_at      DATETIME2(3)  NOT NULL CONSTRAINT df_sys_dept_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2(3)  NOT NULL CONSTRAINT df_sys_dept_updated DEFAULT SYSUTCDATETIME()
);

CREATE TABLE sys_audit_log (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_sys_audit_log PRIMARY KEY,
    user_id         BIGINT        NULL,
    action          NVARCHAR(64)  NOT NULL,
    target_type     NVARCHAR(64)  NOT NULL,
    target_id       NVARCHAR(64)  NULL,
    before_json     NVARCHAR(MAX) NULL,
    after_json      NVARCHAR(MAX) NULL,
    ip              NVARCHAR(64)  NULL,
    created_at      DATETIME2(3)  NOT NULL CONSTRAINT df_sys_audit_created DEFAULT SYSUTCDATETIME()
);

CREATE INDEX ix_sys_audit_log_created ON sys_audit_log(created_at);
CREATE INDEX ix_sys_audit_log_target ON sys_audit_log(target_type, target_id);

CREATE TABLE refresh_token (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_refresh_token PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    token_hash      NVARCHAR(128) NOT NULL,
    expires_at      DATETIME2(3)  NOT NULL,
    revoked         BIT           NOT NULL CONSTRAINT df_refresh_token_revoked DEFAULT 0,
    rotated_at      DATETIME2(3)  NULL,
    created_at      DATETIME2(3)  NOT NULL CONSTRAINT df_refresh_token_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE UNIQUE INDEX ux_refresh_token_hash ON refresh_token(token_hash);
CREATE INDEX ix_refresh_token_user ON refresh_token(user_id, revoked);
