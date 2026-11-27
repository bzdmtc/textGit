-- 模块 4：秒杀（活动、抢购记录）

CREATE TABLE seckill_activity (
    id               BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_seckill_activity PRIMARY KEY,
    course_id        BIGINT        NOT NULL,
    seckill_price    DECIMAL(10,2) NOT NULL,
    total_stock      INT           NOT NULL,
    available_stock  INT           NOT NULL,
    per_user_limit   INT           NOT NULL CONSTRAINT df_seckill_per_user DEFAULT 1,
    start_at         DATETIME2(3)  NOT NULL,
    end_at           DATETIME2(3)  NOT NULL,
    status           NVARCHAR(16)  NOT NULL CONSTRAINT df_seckill_status DEFAULT 'ON', -- ON/OFF
    created_at       DATETIME2(3)  NOT NULL CONSTRAINT df_seckill_created DEFAULT SYSUTCDATETIME(),
    updated_at       DATETIME2(3)  NOT NULL CONSTRAINT df_seckill_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_seckill_activity_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT ck_seckill_activity_stock CHECK (total_stock >= 0 AND available_stock >= 0 AND available_stock <= total_stock),
    CONSTRAINT ck_seckill_activity_time CHECK (end_at > start_at),
    CONSTRAINT ck_seckill_activity_limit CHECK (per_user_limit > 0)
);

CREATE INDEX ix_seckill_activity_status_start ON seckill_activity(status, start_at);
CREATE INDEX ix_seckill_activity_course ON seckill_activity(course_id);

CREATE TABLE seckill_record (
    id          BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_seckill_record PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    activity_id BIGINT       NOT NULL,
    order_no    NVARCHAR(40) NULL,
    status      NVARCHAR(16) NOT NULL CONSTRAINT df_seckill_record_status DEFAULT 'CREATED', -- CREATED/PAID/CANCELED/FAILED
    reason      NVARCHAR(200) NULL,
    created_at  DATETIME2(3) NOT NULL CONSTRAINT df_seckill_record_created DEFAULT SYSUTCDATETIME(),
    updated_at  DATETIME2(3) NOT NULL CONSTRAINT df_seckill_record_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ux_seckill_record_user_activity UNIQUE (user_id, activity_id),
    CONSTRAINT fk_seckill_record_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_seckill_record_activity FOREIGN KEY (activity_id) REFERENCES seckill_activity(id)
);

CREATE INDEX ix_seckill_record_activity_status ON seckill_record(activity_id, status);
CREATE INDEX ix_seckill_record_order ON seckill_record(order_no);

-- 秒杀订单按活动查询/对账
CREATE INDEX ix_orders_source_activity ON orders(order_source, seckill_activity_id);
