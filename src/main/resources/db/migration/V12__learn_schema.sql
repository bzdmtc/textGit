-- 模块 5：学习（课时进度、笔记、收藏、课程评价）

CREATE TABLE lesson_progress (
    id          BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_lesson_progress PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    lesson_id   BIGINT      NOT NULL,
    watched_sec INT         NOT NULL CONSTRAINT df_lesson_progress_watched DEFAULT 0,
    completed   BIT         NOT NULL CONSTRAINT df_lesson_progress_completed DEFAULT 0,
    created_at  DATETIME2(3) NOT NULL CONSTRAINT df_lesson_progress_created DEFAULT SYSUTCDATETIME(),
    updated_at  DATETIME2(3) NOT NULL CONSTRAINT df_lesson_progress_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ux_lesson_progress_user_lesson UNIQUE (user_id, lesson_id),
    CONSTRAINT fk_lesson_progress_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_lesson_progress_lesson FOREIGN KEY (lesson_id) REFERENCES course_lesson(id)
);

CREATE INDEX ix_lesson_progress_user ON lesson_progress(user_id);
CREATE INDEX ix_lesson_progress_lesson ON lesson_progress(lesson_id);

CREATE TABLE note (
    id         BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_note PRIMARY KEY,
    user_id    BIGINT        NOT NULL,
    lesson_id  BIGINT        NOT NULL,
    content    NVARCHAR(2000) NOT NULL,
    created_at DATETIME2(3)  NOT NULL CONSTRAINT df_note_created DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(3)  NOT NULL CONSTRAINT df_note_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_note_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_note_lesson FOREIGN KEY (lesson_id) REFERENCES course_lesson(id)
);

CREATE INDEX ix_note_user_lesson ON note(user_id, lesson_id);

CREATE TABLE course_favorite (
    id         BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_course_favorite PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    course_id  BIGINT      NOT NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_course_favorite_created DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_course_favorite_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ux_course_favorite_user_course UNIQUE (user_id, course_id),
    CONSTRAINT fk_course_favorite_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_course_favorite_course FOREIGN KEY (course_id) REFERENCES course(id)
);

CREATE INDEX ix_course_favorite_course ON course_favorite(course_id);

CREATE TABLE course_review (
    id          BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_course_review PRIMARY KEY,
    course_id   BIGINT        NOT NULL,
    user_id     BIGINT        NOT NULL,
    order_id    BIGINT        NULL,
    rating      INT           NOT NULL,
    content     NVARCHAR(1000) NOT NULL,
    status      NVARCHAR(16)  NOT NULL CONSTRAINT df_course_review_status DEFAULT 'PENDING', -- PENDING/APPROVED/REJECTED
    admin_note  NVARCHAR(500) NULL,
    reviewed_by BIGINT        NULL,
    reviewed_at DATETIME2(3)  NULL,
    created_at  DATETIME2(3)  NOT NULL CONSTRAINT df_course_review_created DEFAULT SYSUTCDATETIME(),
    updated_at  DATETIME2(3)  NOT NULL CONSTRAINT df_course_review_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ux_course_review_user_course UNIQUE (user_id, course_id),
    CONSTRAINT ck_course_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_course_review_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_course_review_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_course_review_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX ix_course_review_course_status ON course_review(course_id, status);
CREATE INDEX ix_course_review_status_created ON course_review(status, created_at);
