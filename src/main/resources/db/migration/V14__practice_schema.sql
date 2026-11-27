-- 模块 6：题库与考试（题目/选项/试卷/作答/错题本）

CREATE TABLE question (
    id            BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_question PRIMARY KEY,
    subject       NVARCHAR(32)   NOT NULL,
    chapter       NVARCHAR(100)  NOT NULL,
    type          NVARCHAR(16)   NOT NULL, -- SINGLE / MULTIPLE / JUDGE
    stem          NVARCHAR(2000) NOT NULL,
    analysis      NVARCHAR(4000) NULL,
    difficulty    INT            NOT NULL CONSTRAINT df_question_difficulty DEFAULT 3,
    status        NVARCHAR(16)   NOT NULL CONSTRAINT df_question_status DEFAULT 'ACTIVE', -- ACTIVE / DISABLED
    owner_user_id BIGINT         NULL,
    created_at    DATETIME2(3)   NOT NULL CONSTRAINT df_question_created DEFAULT SYSUTCDATETIME(),
    updated_at    DATETIME2(3)   NOT NULL CONSTRAINT df_question_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ck_question_type CHECK (type IN ('SINGLE', 'MULTIPLE', 'JUDGE')),
    CONSTRAINT ck_question_difficulty CHECK (difficulty BETWEEN 1 AND 5),
    CONSTRAINT ck_question_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT fk_question_owner FOREIGN KEY (owner_user_id) REFERENCES sys_user(id)
);

CREATE INDEX ix_question_subject_chapter ON question(subject, chapter);
CREATE INDEX ix_question_type ON question(type);
CREATE INDEX ix_question_status ON question(status);
CREATE INDEX ix_question_owner ON question(owner_user_id);

CREATE TABLE question_option (
    id          BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_question_option PRIMARY KEY,
    question_id BIGINT         NOT NULL,
    label       NVARCHAR(8)    NOT NULL,
    content     NVARCHAR(1000) NOT NULL,
    is_correct  BIT            NOT NULL CONSTRAINT df_question_option_correct DEFAULT 0,
    sort        INT            NOT NULL CONSTRAINT df_question_option_sort DEFAULT 0,
    created_at  DATETIME2(3)   NOT NULL CONSTRAINT df_question_option_created DEFAULT SYSUTCDATETIME(),
    updated_at  DATETIME2(3)   NOT NULL CONSTRAINT df_question_option_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ux_question_option_label UNIQUE (question_id, label),
    CONSTRAINT fk_question_option_question FOREIGN KEY (question_id) REFERENCES question(id)
);

CREATE INDEX ix_question_option_question ON question_option(question_id);

CREATE TABLE paper (
    id            BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_paper PRIMARY KEY,
    title         NVARCHAR(200)  NOT NULL,
    type          NVARCHAR(16)   NOT NULL, -- CHAPTER / PAST / MOCK
    subject       NVARCHAR(32)   NOT NULL,
    chapter       NVARCHAR(100)  NULL,
    description   NVARCHAR(1000) NULL,
    total_score   DECIMAL(8,2)   NOT NULL CONSTRAINT df_paper_total_score DEFAULT 0,
    duration_min  INT            NOT NULL CONSTRAINT df_paper_duration DEFAULT 0,
    status        NVARCHAR(16)   NOT NULL CONSTRAINT df_paper_status DEFAULT 'DRAFT', -- DRAFT / PUBLISHED / OFF
    owner_user_id BIGINT         NULL,
    created_at    DATETIME2(3)   NOT NULL CONSTRAINT df_paper_created DEFAULT SYSUTCDATETIME(),
    updated_at    DATETIME2(3)   NOT NULL CONSTRAINT df_paper_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ck_paper_type CHECK (type IN ('CHAPTER', 'PAST', 'MOCK')),
    CONSTRAINT ck_paper_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFF')),
    CONSTRAINT fk_paper_owner FOREIGN KEY (owner_user_id) REFERENCES sys_user(id)
);

CREATE INDEX ix_paper_type_status ON paper(type, status);
CREATE INDEX ix_paper_subject ON paper(subject);
CREATE INDEX ix_paper_owner ON paper(owner_user_id);

CREATE TABLE paper_question (
    id          BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_paper_question PRIMARY KEY,
    paper_id    BIGINT       NOT NULL,
    question_id BIGINT       NOT NULL,
    score       DECIMAL(6,2) NOT NULL CONSTRAINT df_paper_question_score DEFAULT 5,
    sort        INT          NOT NULL CONSTRAINT df_paper_question_sort DEFAULT 0,
    created_at  DATETIME2(3) NOT NULL CONSTRAINT df_paper_question_created DEFAULT SYSUTCDATETIME(),
    updated_at  DATETIME2(3) NOT NULL CONSTRAINT df_paper_question_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ux_paper_question UNIQUE (paper_id, question_id),
    CONSTRAINT ck_paper_question_score CHECK (score > 0),
    CONSTRAINT fk_paper_question_paper FOREIGN KEY (paper_id) REFERENCES paper(id),
    CONSTRAINT fk_paper_question_question FOREIGN KEY (question_id) REFERENCES question(id)
);

CREATE INDEX ix_paper_question_paper ON paper_question(paper_id);
CREATE INDEX ix_paper_question_question ON paper_question(question_id);

CREATE TABLE exam_record (
    id             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_exam_record PRIMARY KEY,
    user_id        BIGINT        NOT NULL,
    paper_id       BIGINT        NULL,
    mode           NVARCHAR(16)  NOT NULL, -- PRACTICE / EXAM
    source         NVARCHAR(16)  NOT NULL CONSTRAINT df_exam_record_source DEFAULT 'NORMAL', -- NORMAL / WRONG
    subject        NVARCHAR(32)  NULL,
    chapter        NVARCHAR(100) NULL,
    title          NVARCHAR(200) NOT NULL,
    started_at     DATETIME2(3)  NOT NULL,
    submitted_at   DATETIME2(3)  NULL,
    deadline_at    DATETIME2(3)  NULL,
    score          DECIMAL(8,2)  NULL,
    total_score    DECIMAL(8,2)  NOT NULL CONSTRAINT df_exam_record_total DEFAULT 0,
    correct_count  INT           NOT NULL CONSTRAINT df_exam_record_correct DEFAULT 0,
    wrong_count    INT           NOT NULL CONSTRAINT df_exam_record_wrong DEFAULT 0,
    question_count INT           NOT NULL CONSTRAINT df_exam_record_questions DEFAULT 0,
    status         NVARCHAR(16)  NOT NULL CONSTRAINT df_exam_record_status DEFAULT 'IN_PROGRESS', -- IN_PROGRESS / SUBMITTED
    created_at     DATETIME2(3)  NOT NULL CONSTRAINT df_exam_record_created DEFAULT SYSUTCDATETIME(),
    updated_at     DATETIME2(3)  NOT NULL CONSTRAINT df_exam_record_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ck_exam_record_mode CHECK (mode IN ('PRACTICE', 'EXAM')),
    CONSTRAINT ck_exam_record_source CHECK (source IN ('NORMAL', 'WRONG')),
    CONSTRAINT ck_exam_record_status CHECK (status IN ('IN_PROGRESS', 'SUBMITTED')),
    CONSTRAINT fk_exam_record_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_exam_record_paper FOREIGN KEY (paper_id) REFERENCES paper(id)
);

CREATE INDEX ix_exam_record_user_created ON exam_record(user_id, created_at);
CREATE INDEX ix_exam_record_user_status ON exam_record(user_id, status);
CREATE INDEX ix_exam_record_paper ON exam_record(paper_id);

CREATE TABLE exam_answer (
    id                BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_exam_answer PRIMARY KEY,
    record_id         BIGINT        NOT NULL,
    question_id       BIGINT        NOT NULL,
    answer            NVARCHAR(32)  NULL,
    is_correct        BIT           NULL,
    score             DECIMAL(6,2)  NULL,
    question_snapshot NVARCHAR(MAX) NOT NULL,
    created_at        DATETIME2(3)  NOT NULL CONSTRAINT df_exam_answer_created DEFAULT SYSUTCDATETIME(),
    updated_at        DATETIME2(3)  NOT NULL CONSTRAINT df_exam_answer_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ux_exam_answer_record_question UNIQUE (record_id, question_id),
    CONSTRAINT fk_exam_answer_record FOREIGN KEY (record_id) REFERENCES exam_record(id),
    CONSTRAINT fk_exam_answer_question FOREIGN KEY (question_id) REFERENCES question(id)
);

CREATE INDEX ix_exam_answer_record ON exam_answer(record_id);

CREATE TABLE wrong_question (
    id            BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_wrong_question PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    question_id   BIGINT       NOT NULL,
    wrong_count   INT          NOT NULL CONSTRAINT df_wrong_question_count DEFAULT 1,
    last_wrong_at DATETIME2(3) NOT NULL,
    mastered      BIT          NOT NULL CONSTRAINT df_wrong_question_mastered DEFAULT 0,
    created_at    DATETIME2(3) NOT NULL CONSTRAINT df_wrong_question_created DEFAULT SYSUTCDATETIME(),
    updated_at    DATETIME2(3) NOT NULL CONSTRAINT df_wrong_question_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ux_wrong_question_user_question UNIQUE (user_id, question_id),
    CONSTRAINT fk_wrong_question_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_wrong_question_question FOREIGN KEY (question_id) REFERENCES question(id)
);

CREATE INDEX ix_wrong_question_user_mastered ON wrong_question(user_id, mastered);
CREATE INDEX ix_wrong_question_question ON wrong_question(question_id);
