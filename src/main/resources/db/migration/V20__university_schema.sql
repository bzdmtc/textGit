-- 模块 7：院校库（院校 / 专业 / 历年分数线 / 报录比）

CREATE TABLE university (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_university PRIMARY KEY,
    name            NVARCHAR(100) NOT NULL,
    province        NVARCHAR(32)  NOT NULL,
    city            NVARCHAR(32)  NULL,
    level           NVARCHAR(32)  NULL, -- 985 / 211 / 双一流 / 普通
    type            NVARCHAR(32)  NULL, -- 综合 / 理工 / 师范 / 财经 / 政法
    logo_url        NVARCHAR(500) NULL,
    website         NVARCHAR(255) NULL,
    introduction    NVARCHAR(MAX) NULL,
    featured        BIT           NOT NULL CONSTRAINT df_university_featured DEFAULT 0,
    sort            INT           NOT NULL CONSTRAINT df_university_sort DEFAULT 0,
    status          NVARCHAR(16)  NOT NULL CONSTRAINT df_university_status DEFAULT 'ACTIVE',
    owner_user_id   BIGINT        NULL,
    created_at      DATETIME2(3)  NOT NULL CONSTRAINT df_university_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2(3)  NOT NULL CONSTRAINT df_university_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ux_university_name UNIQUE (name),
    CONSTRAINT fk_university_owner FOREIGN KEY (owner_user_id) REFERENCES sys_user(id)
);

CREATE INDEX ix_university_status_province ON university(status, province);
CREATE INDEX ix_university_status_level ON university(status, level);
CREATE INDEX ix_university_featured_sort ON university(featured, sort);

CREATE TABLE university_major (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_university_major PRIMARY KEY,
    university_id   BIGINT        NOT NULL,
    name            NVARCHAR(100) NOT NULL,
    code            NVARCHAR(32)  NULL,
    degree_type     NVARCHAR(16)  NOT NULL CONSTRAINT df_university_major_degree DEFAULT N'学硕', -- 学硕/专硕
    duration        NVARCHAR(16)  NULL,
    tuition         DECIMAL(10,2) NULL,
    exam_subjects   NVARCHAR(255) NULL,
    introduction    NVARCHAR(MAX) NULL,
    sort            INT           NOT NULL CONSTRAINT df_university_major_sort DEFAULT 0,
    status          NVARCHAR(16)  NOT NULL CONSTRAINT df_university_major_status DEFAULT 'ACTIVE',
    created_at      DATETIME2(3)  NOT NULL CONSTRAINT df_university_major_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2(3)  NOT NULL CONSTRAINT df_university_major_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ux_university_major_name UNIQUE (university_id, name, degree_type),
    CONSTRAINT fk_university_major_university FOREIGN KEY (university_id) REFERENCES university(id)
);

CREATE INDEX ix_university_major_university ON university_major(university_id, status, sort);

CREATE TABLE score_line (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_score_line PRIMARY KEY,
    university_id   BIGINT       NOT NULL,
    major_id        BIGINT       NOT NULL,
    year            INT          NOT NULL,
    degree_type     NVARCHAR(16) NOT NULL,
    political       INT          NULL,
    english         INT          NULL,
    math            INT          NULL,
    major_score     INT          NULL,
    total           INT          NOT NULL,
    created_at      DATETIME2(3) NOT NULL CONSTRAINT df_score_line_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2(3) NOT NULL CONSTRAINT df_score_line_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ux_score_line_major_year UNIQUE (major_id, year),
    CONSTRAINT fk_score_line_university FOREIGN KEY (university_id) REFERENCES university(id),
    CONSTRAINT fk_score_line_major FOREIGN KEY (major_id) REFERENCES university_major(id)
);

CREATE INDEX ix_score_line_university_year ON score_line(university_id, year);

CREATE TABLE admission_stat (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_admission_stat PRIMARY KEY,
    university_id   BIGINT       NOT NULL,
    major_id        BIGINT       NOT NULL,
    year            INT          NOT NULL,
    applicants      INT          NOT NULL CONSTRAINT df_admission_stat_applicants DEFAULT 0,
    admitted        INT          NOT NULL CONSTRAINT df_admission_stat_admitted DEFAULT 0,
    ratio           DECIMAL(6,2) NULL,
    min_score       INT          NULL,
    created_at      DATETIME2(3) NOT NULL CONSTRAINT df_admission_stat_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2(3) NOT NULL CONSTRAINT df_admission_stat_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ux_admission_stat_major_year UNIQUE (major_id, year),
    CONSTRAINT fk_admission_stat_university FOREIGN KEY (university_id) REFERENCES university(id),
    CONSTRAINT fk_admission_stat_major FOREIGN KEY (major_id) REFERENCES university_major(id)
);

CREATE INDEX ix_admission_stat_university_year ON admission_stat(university_id, year);
