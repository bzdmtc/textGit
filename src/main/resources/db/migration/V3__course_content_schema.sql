-- 模块 2：课程与内容
-- 讲师、课程、章节、课时、文章、资料、Banner

CREATE TABLE teacher (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_teacher PRIMARY KEY,
    name            NVARCHAR(64)  NOT NULL,
    title           NVARCHAR(64)  NULL,
    avatar          NVARCHAR(500) NULL,
    subject         NVARCHAR(32)  NULL,
    introduction    NVARCHAR(MAX) NULL,
    sort            INT           NOT NULL CONSTRAINT df_teacher_sort DEFAULT 0,
    status          NVARCHAR(16)  NOT NULL CONSTRAINT df_teacher_status DEFAULT 'ACTIVE',
    owner_user_id   BIGINT        NULL,
    created_at      DATETIME2(3)  NOT NULL CONSTRAINT df_teacher_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2(3)  NOT NULL CONSTRAINT df_teacher_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_teacher_owner FOREIGN KEY (owner_user_id) REFERENCES sys_user(id)
);

CREATE INDEX ix_teacher_status_sort ON teacher(status, sort);
CREATE INDEX ix_teacher_owner ON teacher(owner_user_id);

CREATE TABLE course (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_course PRIMARY KEY,
    title           NVARCHAR(200) NOT NULL,
    subtitle        NVARCHAR(255) NULL,
    subject         NVARCHAR(32)  NOT NULL,
    stage           NVARCHAR(32)  NOT NULL,
    cover_url       NVARCHAR(500) NULL,
    description     NVARCHAR(MAX) NULL,
    price           DECIMAL(10,2) NOT NULL CONSTRAINT df_course_price DEFAULT 0,
    original_price  DECIMAL(10,2) NULL,
    rating          DECIMAL(3,2)  NOT NULL CONSTRAINT df_course_rating DEFAULT 5.00,
    student_count   INT           NOT NULL CONSTRAINT df_course_students DEFAULT 0,
    status          NVARCHAR(16)  NOT NULL CONSTRAINT df_course_status DEFAULT 'DRAFT',
    owner_user_id   BIGINT        NULL,
    created_at      DATETIME2(3)  NOT NULL CONSTRAINT df_course_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2(3)  NOT NULL CONSTRAINT df_course_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_course_owner FOREIGN KEY (owner_user_id) REFERENCES sys_user(id)
);

CREATE INDEX ix_course_status_subject_stage ON course(status, subject, stage);
CREATE INDEX ix_course_owner ON course(owner_user_id);
CREATE INDEX ix_course_created ON course(created_at);

CREATE TABLE course_teacher (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_course_teacher PRIMARY KEY,
    course_id       BIGINT NOT NULL,
    teacher_id      BIGINT NOT NULL,
    sort            INT    NOT NULL CONSTRAINT df_course_teacher_sort DEFAULT 0,
    CONSTRAINT fk_course_teacher_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_course_teacher_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id)
);

CREATE UNIQUE INDEX ux_course_teacher ON course_teacher(course_id, teacher_id);
CREATE INDEX ix_course_teacher_teacher ON course_teacher(teacher_id);

CREATE TABLE course_chapter (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_course_chapter PRIMARY KEY,
    course_id       BIGINT NOT NULL,
    title           NVARCHAR(200) NOT NULL,
    sort            INT    NOT NULL CONSTRAINT df_chapter_sort DEFAULT 0,
    created_at      DATETIME2(3) NOT NULL CONSTRAINT df_chapter_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2(3) NOT NULL CONSTRAINT df_chapter_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_chapter_course FOREIGN KEY (course_id) REFERENCES course(id)
);

CREATE INDEX ix_chapter_course_sort ON course_chapter(course_id, sort);

CREATE TABLE course_lesson (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_course_lesson PRIMARY KEY,
    chapter_id      BIGINT NOT NULL,
    title           NVARCHAR(200) NOT NULL,
    video_url       NVARCHAR(500) NULL,
    duration_sec    INT    NOT NULL CONSTRAINT df_lesson_duration DEFAULT 0,
    is_trial        BIT    NOT NULL CONSTRAINT df_lesson_trial DEFAULT 0,
    sort            INT    NOT NULL CONSTRAINT df_lesson_sort DEFAULT 0,
    status          NVARCHAR(16) NOT NULL CONSTRAINT df_lesson_status DEFAULT 'ACTIVE',
    created_at      DATETIME2(3) NOT NULL CONSTRAINT df_lesson_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2(3) NOT NULL CONSTRAINT df_lesson_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_lesson_chapter FOREIGN KEY (chapter_id) REFERENCES course_chapter(id)
);

CREATE INDEX ix_lesson_chapter_sort ON course_lesson(chapter_id, sort);

CREATE TABLE article_category (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_article_category PRIMARY KEY,
    name            NVARCHAR(64) NOT NULL,
    sort            INT          NOT NULL CONSTRAINT df_article_category_sort DEFAULT 0,
    created_at      DATETIME2(3) NOT NULL CONSTRAINT df_article_category_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2(3) NOT NULL CONSTRAINT df_article_category_updated DEFAULT SYSUTCDATETIME()
);

CREATE TABLE article (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_article PRIMARY KEY,
    category_id     BIGINT        NULL,
    title           NVARCHAR(200) NOT NULL,
    summary         NVARCHAR(500) NULL,
    cover_url       NVARCHAR(500) NULL,
    content_html    NVARCHAR(MAX) NULL,
    author          NVARCHAR(64)  NULL,
    status          NVARCHAR(16)  NOT NULL CONSTRAINT df_article_status DEFAULT 'DRAFT',
    published_at    DATETIME2(3)  NULL,
    view_count      INT           NOT NULL CONSTRAINT df_article_views DEFAULT 0,
    owner_user_id   BIGINT        NULL,
    created_at      DATETIME2(3)  NOT NULL CONSTRAINT df_article_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2(3)  NOT NULL CONSTRAINT df_article_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_article_category FOREIGN KEY (category_id) REFERENCES article_category(id),
    CONSTRAINT fk_article_owner FOREIGN KEY (owner_user_id) REFERENCES sys_user(id)
);

CREATE INDEX ix_article_status_published ON article(status, published_at);
CREATE INDEX ix_article_category ON article(category_id);
CREATE INDEX ix_article_owner ON article(owner_user_id);

CREATE TABLE material (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_material PRIMARY KEY,
    title           NVARCHAR(200) NOT NULL,
    subject         NVARCHAR(32)  NULL,
    file_url        NVARCHAR(500) NOT NULL,
    file_size       BIGINT        NOT NULL CONSTRAINT df_material_size DEFAULT 0,
    download_count  INT           NOT NULL CONSTRAINT df_material_downloads DEFAULT 0,
    status          NVARCHAR(16)  NOT NULL CONSTRAINT df_material_status DEFAULT 'ACTIVE',
    owner_user_id   BIGINT        NULL,
    created_at      DATETIME2(3)  NOT NULL CONSTRAINT df_material_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2(3)  NOT NULL CONSTRAINT df_material_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_material_owner FOREIGN KEY (owner_user_id) REFERENCES sys_user(id)
);

CREATE INDEX ix_material_status_subject ON material(status, subject);
CREATE INDEX ix_material_owner ON material(owner_user_id);

CREATE TABLE banner (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_banner PRIMARY KEY,
    title           NVARCHAR(128) NOT NULL,
    image_url       NVARCHAR(500) NOT NULL,
    link_url        NVARCHAR(500) NULL,
    sort            INT           NOT NULL CONSTRAINT df_banner_sort DEFAULT 0,
    status          NVARCHAR(16)  NOT NULL CONSTRAINT df_banner_status DEFAULT 'ACTIVE',
    start_at        DATETIME2(3)  NULL,
    end_at          DATETIME2(3)  NULL,
    created_at      DATETIME2(3)  NOT NULL CONSTRAINT df_banner_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2(3)  NOT NULL CONSTRAINT df_banner_updated DEFAULT SYSUTCDATETIME()
);

CREATE INDEX ix_banner_status_sort ON banner(status, sort);
