-- 模块 3：交易（优惠券、订单、支付流水、退款、选课关系）

CREATE TABLE coupon (
    id                  BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_coupon PRIMARY KEY,
    name                NVARCHAR(100) NOT NULL,
    type                NVARCHAR(16)  NOT NULL,                 -- FIXED 满减 / PERCENT 折扣
    value_amount        DECIMAL(10,2) NOT NULL,                 -- FIXED: 减免金额；PERCENT: 折扣百分比（90 = 9 折）
    min_amount          DECIMAL(10,2) NOT NULL CONSTRAINT df_coupon_min_amount DEFAULT 0,
    total_quantity      INT           NOT NULL,
    remaining_quantity  INT           NOT NULL,
    per_user_limit      INT           NOT NULL CONSTRAINT df_coupon_per_user DEFAULT 1,
    start_at            DATETIME2(3)  NOT NULL,
    end_at              DATETIME2(3)  NOT NULL,
    subject             NVARCHAR(32)  NULL,                     -- 为空表示全站通用
    description         NVARCHAR(500) NULL,
    status              NVARCHAR(16)  NOT NULL CONSTRAINT df_coupon_status DEFAULT 'ON',
    created_at          DATETIME2(3)  NOT NULL CONSTRAINT df_coupon_created DEFAULT SYSUTCDATETIME(),
    updated_at          DATETIME2(3)  NOT NULL CONSTRAINT df_coupon_updated DEFAULT SYSUTCDATETIME()
);

CREATE INDEX ix_coupon_status_time ON coupon(status, start_at, end_at);
CREATE INDEX ix_coupon_subject ON coupon(subject);

CREATE TABLE user_coupon (
    id          BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_user_coupon PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    coupon_id   BIGINT      NOT NULL,
    status      NVARCHAR(16) NOT NULL CONSTRAINT df_user_coupon_status DEFAULT 'UNUSED', -- UNUSED/LOCKED/USED/EXPIRED
    received_at DATETIME2(3) NOT NULL CONSTRAINT df_user_coupon_received DEFAULT SYSUTCDATETIME(),
    used_at     DATETIME2(3) NULL,
    order_id    BIGINT      NULL,
    created_at  DATETIME2(3) NOT NULL CONSTRAINT df_user_coupon_created DEFAULT SYSUTCDATETIME(),
    updated_at  DATETIME2(3) NOT NULL CONSTRAINT df_user_coupon_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_user_coupon_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_user_coupon_coupon FOREIGN KEY (coupon_id) REFERENCES coupon(id)
);

CREATE INDEX ix_user_coupon_user_status ON user_coupon(user_id, status);
CREATE INDEX ix_user_coupon_coupon ON user_coupon(coupon_id);
CREATE INDEX ix_user_coupon_order ON user_coupon(order_id);

CREATE TABLE orders (
    id                  BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_orders PRIMARY KEY,
    order_no            NVARCHAR(40)  NOT NULL,
    user_id             BIGINT        NOT NULL,
    total_amount        DECIMAL(10,2) NOT NULL,
    discount_amount     DECIMAL(10,2) NOT NULL CONSTRAINT df_orders_discount DEFAULT 0,
    pay_amount          DECIMAL(10,2) NOT NULL,
    user_coupon_id      BIGINT        NULL,
    order_source        NVARCHAR(16)  NOT NULL CONSTRAINT df_orders_source DEFAULT 'NORMAL', -- NORMAL/SECKILL
    seckill_activity_id BIGINT        NULL,
    status              NVARCHAR(32)  NOT NULL CONSTRAINT df_orders_status DEFAULT 'PENDING_PAYMENT',
    pay_method          NVARCHAR(32)  NULL,
    paid_at             DATETIME2(3)  NULL,
    expire_at           DATETIME2(3)  NULL,
    canceled_at         DATETIME2(3)  NULL,
    completed_at        DATETIME2(3)  NULL,
    created_at          DATETIME2(3)  NOT NULL CONSTRAINT df_orders_created DEFAULT SYSUTCDATETIME(),
    updated_at          DATETIME2(3)  NOT NULL CONSTRAINT df_orders_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ux_orders_order_no UNIQUE (order_no),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE INDEX ix_orders_user_status_created ON orders(user_id, status, created_at);
CREATE INDEX ix_orders_status_expire ON orders(status, expire_at);
CREATE INDEX ix_orders_status_paid ON orders(status, paid_at);

CREATE TABLE order_item (
    id                    BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_order_item PRIMARY KEY,
    order_id              BIGINT        NOT NULL,
    course_id             BIGINT        NOT NULL,
    course_title_snapshot NVARCHAR(200) NOT NULL,
    cover_snapshot        NVARCHAR(500) NULL,
    subject_snapshot      NVARCHAR(32)  NULL,
    price_snapshot        DECIMAL(10,2) NOT NULL,
    created_at            DATETIME2(3)  NOT NULL CONSTRAINT df_order_item_created DEFAULT SYSUTCDATETIME(),
    updated_at            DATETIME2(3)  NOT NULL CONSTRAINT df_order_item_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_item_course FOREIGN KEY (course_id) REFERENCES course(id)
);

CREATE INDEX ix_order_item_order ON order_item(order_id);
CREATE INDEX ix_order_item_course ON order_item(course_id);

CREATE TABLE payment_record (
    id             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_payment_record PRIMARY KEY,
    order_no       NVARCHAR(40)  NOT NULL,
    channel        NVARCHAR(32)  NOT NULL,
    transaction_no NVARCHAR(64)  NOT NULL,
    amount         DECIMAL(10,2) NOT NULL,
    status         NVARCHAR(16)  NOT NULL CONSTRAINT df_payment_status DEFAULT 'SUCCESS',
    paid_at        DATETIME2(3)  NOT NULL,
    created_at     DATETIME2(3)  NOT NULL CONSTRAINT df_payment_created DEFAULT SYSUTCDATETIME(),
    updated_at     DATETIME2(3)  NOT NULL CONSTRAINT df_payment_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ux_payment_transaction_no UNIQUE (transaction_no)
);

CREATE INDEX ix_payment_order_no ON payment_record(order_no);

CREATE TABLE refund_request (
    id         BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_refund_request PRIMARY KEY,
    order_id   BIGINT        NOT NULL,
    order_no   NVARCHAR(40)  NOT NULL,
    user_id    BIGINT        NOT NULL,
    reason     NVARCHAR(500) NOT NULL,
    amount     DECIMAL(10,2) NOT NULL,
    status     NVARCHAR(16)  NOT NULL CONSTRAINT df_refund_status DEFAULT 'PENDING', -- PENDING/APPROVED/REJECTED
    admin_note NVARCHAR(500) NULL,
    handled_by BIGINT        NULL,
    handled_at DATETIME2(3)  NULL,
    created_at DATETIME2(3)  NOT NULL CONSTRAINT df_refund_created DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(3)  NOT NULL CONSTRAINT df_refund_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_refund_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE INDEX ix_refund_status_created ON refund_request(status, created_at);
CREATE INDEX ix_refund_order ON refund_request(order_id);
CREATE INDEX ix_refund_user ON refund_request(user_id);

CREATE TABLE user_course (
    id               BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_user_course PRIMARY KEY,
    user_id          BIGINT        NOT NULL,
    course_id        BIGINT        NOT NULL,
    order_id         BIGINT        NULL,
    enrolled_at      DATETIME2(3)  NOT NULL CONSTRAINT df_user_course_enrolled DEFAULT SYSUTCDATETIME(),
    progress_percent DECIMAL(5,2)  NOT NULL CONSTRAINT df_user_course_progress DEFAULT 0,
    status           NVARCHAR(16)  NOT NULL CONSTRAINT df_user_course_status DEFAULT 'ACTIVE',
    created_at       DATETIME2(3)  NOT NULL CONSTRAINT df_user_course_created DEFAULT SYSUTCDATETIME(),
    updated_at       DATETIME2(3)  NOT NULL CONSTRAINT df_user_course_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ux_user_course_user_course UNIQUE (user_id, course_id),
    CONSTRAINT fk_user_course_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_user_course_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_user_course_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX ix_user_course_course ON user_course(course_id);
CREATE INDEX ix_user_course_order ON user_course(order_id);
