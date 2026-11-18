-- Interastral Peace Mall（星际和平商城）数据库表结构
-- 依据：docs/PRD.md §7 数据模型（10 张业务表 + 2 张辅助表）
-- 适用：MySQL 8.0.16+ / 8.4，InnoDB，utf8mb4 / utf8mb4_general_ci。
-- 用法：先执行本文件，再执行 db/data.sql。
-- 在空库中首次执行；不 DROP、不清空、不覆盖已有表；已有同名表会报错，不要用 --force 跳过。
--
-- 与 PRD 一致的两个约定：
--   1) 公共字段 deleted / create_time / update_time，MyBatis-Plus 逻辑删除 + 自动填充；
--   2) 不建外键约束（PRD 未要求），归属与引用完整性由 Service 校验，索引负责查询性能。
-- 唯一索引与逻辑删除的坑见 PRD §7.5：account/admin 唯一索引建在 (username, deleted)；
-- cart 不建唯一索引，加购时按 (account_id, item_id) 查询含已删除记录后"复活"。

SET NAMES utf8mb4;
SET SESSION time_zone = '+08:00';

CREATE DATABASE IF NOT EXISTS interastral_mall
    CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE interastral_mall;

-- ============================================================
-- 1. account：前台用户（PRD §7.3）
-- ============================================================
CREATE TABLE account (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    username        VARCHAR(20)  NOT NULL COMMENT '登录名',
    password        VARCHAR(60)  NOT NULL COMMENT 'BCrypt 密文（定长 60）',
    nickname        VARCHAR(20)  NULL COMMENT '昵称，缺省取 username',
    phone           VARCHAR(11)  NULL COMMENT '手机号',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 禁用',
    last_login_time DATETIME     NULL,
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '1 已删除',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_username (username, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='前台用户';

-- ============================================================
-- 2. admin：后台卖家账号（PRD §7.3）
-- ============================================================
CREATE TABLE admin (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    username        VARCHAR(20) NOT NULL,
    password        VARCHAR(60) NOT NULL COMMENT 'BCrypt 密文',
    nickname        VARCHAR(20) NULL,
    status          TINYINT     NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 禁用',
    last_login_time DATETIME    NULL,
    deleted         TINYINT     NOT NULL DEFAULT 0,
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_username (username, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='后台卖家账号';

-- ============================================================
-- 3. category：两级分类，parent_id=0 表示一级（PRD §7.3）
-- ============================================================
CREATE TABLE category (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '0=一级；否则指向一级 id',
    name        VARCHAR(30)  NOT NULL COMMENT '同级内唯一（应用层校验）',
    descn       VARCHAR(200) NULL,
    sort        INT          NOT NULL DEFAULT 0 COMMENT '升序排列',
    visible     TINYINT      NOT NULL DEFAULT 1 COMMENT '1 前台可见',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_category_parent_sort (parent_id, sort, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品分类（两级）';

-- ============================================================
-- 4. product：商品 SPU（PRD §7.3）
-- ============================================================
CREATE TABLE product (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    category_id BIGINT       NOT NULL COMMENT '必须挂二级分类，由 Service 校验',
    name        VARCHAR(60)  NOT NULL,
    brand       VARCHAR(30)  NULL,
    descn       VARCHAR(200) NULL COMMENT '短描述，列表/详情顶部',
    detail      TEXT         NULL COMMENT '详情',
    main_image  VARCHAR(255) NULL COMMENT '主图 URL，空则前端用占位图',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 上架 / 0 下架',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_product_category_status (category_id, status),
    KEY idx_product_update_time (update_time),
    KEY idx_product_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品 SPU';

-- ============================================================
-- 5. item：商品 SKU，价格/库存/乐观锁都在这一层（PRD §7.3）
-- ============================================================
CREATE TABLE item (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    product_id  BIGINT        NOT NULL,
    spec        VARCHAR(50)   NOT NULL COMMENT '规格名，如「500ml 星白」，同 SPU 内唯一',
    price       DECIMAL(10,2) NOT NULL,
    stock       INT           NOT NULL DEFAULT 0 COMMENT '可销售库存',
    image       VARCHAR(255)  NULL COMMENT '规格图，空则用 SPU 主图',
    version     INT           NOT NULL DEFAULT 0 COMMENT '乐观锁',
    deleted     TINYINT       NOT NULL DEFAULT 0,
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_item_product (product_id),
    CONSTRAINT ck_item_price CHECK (price > 0),
    CONSTRAINT ck_item_stock CHECK (stock >= 0),
    CONSTRAINT ck_item_version CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品 SKU';

-- ============================================================
-- 6. cart：购物车（逻辑删除即"移出购物车"，PRD §7.3 / §7.5）
-- ============================================================
CREATE TABLE cart (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    account_id  BIGINT   NOT NULL,
    item_id     BIGINT   NOT NULL,
    quantity    INT      NOT NULL DEFAULT 1 COMMENT '1–99',
    deleted     TINYINT  NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_cart_account_item (account_id, item_id),
    CONSTRAINT ck_cart_quantity CHECK (quantity BETWEEN 1 AND 99)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车';

-- ============================================================
-- 7. address：收货地址（PRD §7.3）
-- ============================================================
CREATE TABLE address (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    account_id  BIGINT       NOT NULL,
    receiver    VARCHAR(20)  NOT NULL,
    phone       VARCHAR(11)  NOT NULL,
    province    VARCHAR(20)  NULL,
    city        VARCHAR(20)  NULL,
    district    VARCHAR(20)  NULL,
    detail      VARCHAR(100) NOT NULL,
    is_default  TINYINT      NOT NULL DEFAULT 0 COMMENT '1 默认',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_address_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='收货地址';

-- ============================================================
-- 8. orders：订单，状态存枚举名（PRD §8.1）
-- ============================================================
CREATE TABLE orders (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    order_no         VARCHAR(32)   NOT NULL COMMENT 'yyyyMMddHHmmss + 4 位随机数',
    account_id       BIGINT        NOT NULL,
    total_amount     DECIMAL(10,2) NOT NULL COMMENT '应付 = 商品总额 + 运费',
    freight_amount   DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '运费，本期固定 0',
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING_PAYMENT'
        COMMENT 'PENDING_PAYMENT/PAID/SHIPPED/COMPLETED/CANCELLED',
    receiver_name     VARCHAR(20)  NOT NULL,
    receiver_phone    VARCHAR(11)  NOT NULL,
    receiver_province VARCHAR(20)  NULL,
    receiver_city     VARCHAR(20)  NULL,
    receiver_district VARCHAR(20)  NULL,
    receiver_detail   VARCHAR(100) NOT NULL,
    courier_company  VARCHAR(30)   NULL COMMENT '物流公司（发货写入）',
    tracking_no      VARCHAR(40)   NULL COMMENT '物流单号',
    remark           VARCHAR(200)  NULL,
    pay_time         DATETIME      NULL,
    ship_time        DATETIME      NULL,
    finish_time      DATETIME      NULL,
    cancel_time      DATETIME      NULL,
    deleted          TINYINT       NOT NULL DEFAULT 0,
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_no (order_no),
    KEY idx_orders_account_status_time (account_id, status, create_time),
    KEY idx_orders_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单';

-- ============================================================
-- 9. order_item：订单明细，全快照，无 update_time / deleted（PRD §7.3）
-- ============================================================
CREATE TABLE order_item (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    order_id     BIGINT        NOT NULL,
    item_id      BIGINT        NOT NULL COMMENT '原 SKU id，仅追溯',
    product_name VARCHAR(60)   NOT NULL COMMENT '快照',
    spec         VARCHAR(50)   NOT NULL COMMENT '快照',
    price        DECIMAL(10,2) NOT NULL COMMENT '下单时单价',
    quantity     INT           NOT NULL,
    image        VARCHAR(255)  NULL COMMENT '快照，订单列表展示用',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order_item_order (order_id),
    CONSTRAINT ck_order_item_price CHECK (price > 0),
    CONSTRAINT ck_order_item_qty CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单明细（商品快照）';

-- ============================================================
-- 10. feedback：投诉与反馈，type 区分，投诉绑订单（PRD §7.3 / §8.4）
-- ============================================================
CREATE TABLE feedback (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    type           VARCHAR(20)  NOT NULL COMMENT 'COMPLAINT 投诉 / FEEDBACK 反馈',
    account_id     BIGINT       NOT NULL,
    order_id       BIGINT       NULL COMMENT '投诉必填，反馈为 NULL',
    category       VARCHAR(20)  NULL COMMENT 'QUALITY/LOGISTICS/SERVICE/OTHER',
    content        VARCHAR(500) NOT NULL COMMENT '正文，纯文本',
    images         VARCHAR(500) NULL COMMENT '【P1】逗号分隔 URL，≤3 张',
    contact        VARCHAR(50)  NULL COMMENT '联系方式（选填）',
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING/PROCESSING/REPLIED/CLOSED',
    reply          VARCHAR(500) NULL COMMENT '卖家回复',
    reply_admin_id BIGINT       NULL,
    reply_time     DATETIME     NULL,
    close_reason   VARCHAR(100) NULL,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_feedback_account_time (account_id, create_time),
    KEY idx_feedback_order (order_id),
    KEY idx_feedback_type_status (type, status),
    CONSTRAINT ck_feedback_type CHECK (type IN ('COMPLAINT', 'FEEDBACK')),
    CONSTRAINT ck_feedback_status CHECK (status IN ('PENDING', 'PROCESSING', 'REPLIED', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='投诉与反馈';

-- ============================================================
-- 11. sys_oper_log：操作日志，无 update_time / deleted（PRD §7.3）
-- ============================================================
CREATE TABLE sys_oper_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    admin_id       BIGINT       NOT NULL DEFAULT 0 COMMENT '前台登录日志时 0',
    admin_name     VARCHAR(20)  NOT NULL COMMENT '操作人名称',
    module         VARCHAR(30)  NOT NULL COMMENT '如 订单管理',
    action         VARCHAR(30)  NOT NULL COMMENT '如 发货',
    target_id      BIGINT       NULL COMMENT '目标主键',
    request_uri    VARCHAR(200) NULL,
    request_method VARCHAR(10)  NULL,
    request_params TEXT         NULL COMMENT 'JSON，password 脱敏',
    ip             VARCHAR(45)  NULL COMMENT '支持 IPv6',
    success        TINYINT      NOT NULL DEFAULT 1,
    error_msg      VARCHAR(500) NULL,
    cost_ms        INT          NOT NULL DEFAULT 0,
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_oper_log_admin_time (admin_id, create_time),
    KEY idx_oper_log_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志';

-- ============================================================
-- 12. sys_check：环境自检（辅助表，PRD §7.2）
-- ============================================================
CREATE TABLE sys_check (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(64) NOT NULL,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='环境自检';

-- ============================================================
-- Service 必须保证的规则（数据库不建外键、不建触发器）：
-- 1) 商品只能挂二级分类（parent_id <> 0）；分类最多两级，禁止循环。
-- 2) 库存扣减与订单写入同一事务；取消/关单只回补一次（条件更新判断）。
-- 3) 订单状态只能按 PRD §8.2 迁移，禁止直接 set 字符串，禁止跳级。
-- 4) total_amount = Σ(明细 price × quantity) + freight_amount。
-- 5) 投诉只能绑定本人 PAID/SHIPPED/COMPLETED 的订单；同一订单只允许一条未关闭投诉。
-- 6) 越权防护：订单/地址/购物车/投诉一律按会话中的 accountId 过滤。
-- ============================================================
