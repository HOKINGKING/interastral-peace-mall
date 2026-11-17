-- Interastral Peace Mall（星际和平商城）数据库表结构
-- 适用：MySQL 8.0.16+ / 8.4，InnoDB，utf8mb4。
-- 与 db/data.sql 配合：先执行本文件建库建表，再执行 data.sql 写入演示数据。
-- 在空库中首次执行；不 DROP、不清空、不覆盖已有表。
-- 已有同名表时会报错，请检查目标数据库，不要用 mysql --force 跳过错误。
-- 本文件不创建数据库账号，不配置应用密码，也不会由应用启动时自动执行。
--
-- 若你的 MySQL 是 5.7：把 utf8mb4_0900_ai_ci 全部替换为 utf8mb4_general_ci，
-- 并删除所有 CONSTRAINT ck_xxx CHECK (...) 段落（5.7 不支持 CHECK 生效）。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET SESSION time_zone = '+08:00';

CREATE DATABASE IF NOT EXISTS interastral_mall
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE interastral_mall;

-- ===== 八张表的统一结构开始 =====

-- 1. 账号：前台顾客与后台管理员共用一张表，靠 role 区分。
CREATE TABLE store_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '内部主键，Java Long',
    username VARCHAR(50) NOT NULL COMMENT '登录名，创建后不修改，唯一',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt散列，不保存明文密码',
    display_name VARCHAR(50) NOT NULL COMMENT '显示名称',
    role VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'CUSTOMER' COMMENT 'CUSTOMER或ADMIN',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED或DISABLED',
    email VARCHAR(100) NULL,
    phone VARCHAR(30) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    CONSTRAINT ck_user_username CHECK (CHAR_LENGTH(TRIM(username)) > 0),
    CONSTRAINT ck_user_password CHECK (CHAR_LENGTH(TRIM(password_hash)) > 0),
    CONSTRAINT ck_user_display_name CHECK (CHAR_LENGTH(TRIM(display_name)) > 0),
    CONSTRAINT ck_user_role CHECK (role IN ('CUSTOMER', 'ADMIN')),
    CONSTRAINT ck_user_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='顾客与管理员账号';

-- 2. 固定两级分类：parent_id 为空表示一级；二级必须指向一级，由 Service 校验。
-- parent_key 是生成列（COALESCE(parent_id,0)），用来做"同一父级下名称唯一"，插入/更新时不要写它。
CREATE TABLE category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL COMMENT '全局唯一编码，创建后不修改',
    parent_id BIGINT NULL COMMENT '一级为空，二级指向一级，创建后不修改',
    parent_key BIGINT GENERATED ALWAYS AS (COALESCE(parent_id, 0)) STORED,
    name VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0 COMMENT '越大越靠前',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_code (code),
    UNIQUE KEY uk_category_sibling_name (parent_key, name),
    KEY idx_category_parent_sort (parent_id, sort_order, id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_category_code CHECK (CHAR_LENGTH(TRIM(code)) > 0),
    CONSTRAINT ck_category_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_category_sort CHECK (sort_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='固定两级商品分类';

-- 3. 一个商品对应一个可销售 SKU，不做多规格；下架（DRAFT）代替删除。
CREATE TABLE product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sku VARCHAR(40) NOT NULL COMMENT '唯一销售编码，创建后不修改',
    category_id BIGINT NOT NULL COMMENT '必须归属二级分类，由Service校验',
    name VARCHAR(120) NOT NULL,
    summary VARCHAR(500) NULL COMMENT '副标题/卖点',
    description TEXT NULL COMMENT '纯文本详情，模板转义展示',
    cover_image VARCHAR(255) NOT NULL DEFAULT 'images/products/placeholder-1.jpg' COMMENT '站内静态资源相对路径',
    price DECIMAL(12,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0 COMMENT '可销售库存：下单扣减，未支付取消时恢复',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT下架或ON_SALE上架',
    sort_order INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0 COMMENT '所有商品写操作显式递增，防旧值覆盖',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_sku (sku),
    KEY idx_product_category_status_sort (category_id, status, sort_order, id),
    KEY idx_product_status_sort (status, sort_order, id),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_product_sku CHECK (CHAR_LENGTH(TRIM(sku)) > 0),
    CONSTRAINT ck_product_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_product_cover CHECK (CHAR_LENGTH(TRIM(cover_image)) > 0),
    CONSTRAINT ck_product_price CHECK (price > 0),
    CONSTRAINT ck_product_stock CHECK (stock >= 0),
    CONSTRAINT ck_product_status CHECK (status IN ('DRAFT', 'ON_SALE')),
    CONSTRAINT ck_product_sort CHECK (sort_order >= 0),
    CONSTRAINT ck_product_version CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品及可销售库存';

-- 4. 详情相册与封面分开存；多个商品可复用同一张教学图片。
CREATE TABLE product_image (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    image_path VARCHAR(255) NOT NULL,
    alt_text VARCHAR(120) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_image_product_sort (product_id, sort_order, id),
    CONSTRAINT fk_image_product FOREIGN KEY (product_id) REFERENCES product (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_image_path CHECK (CHAR_LENGTH(TRIM(image_path)) > 0),
    CONSTRAINT ck_image_sort CHECK (sort_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品详情相册';

-- 5. 登录用户的持久购物车：价格不存这里（以商品当前价为准）；购物车不占库存。
CREATE TABLE cart_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_user_product (user_id, product_id),
    KEY idx_cart_product (product_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES store_user (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES product (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_cart_quantity CHECK (quantity BETWEEN 1 AND 99),
    CONSTRAINT ck_cart_version CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='按用户持久化的购物车条目';

-- 6. 订单主表：模拟支付成功即 PAID（待发货），没有真实支付渠道或退款流程。
CREATE TABLE sales_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(40) NOT NULL COMMENT '服务端生成的唯一展示订单号',
    user_id BIGINT NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDING_PAYMENT'
        COMMENT 'PENDING_PAYMENT待支付 PAID已支付 SHIPPED已发货 COMPLETED已完成 CANCELLED已取消',
    total_amount DECIMAL(12,2) NOT NULL,
    receiver_name VARCHAR(50) NOT NULL,
    receiver_phone VARCHAR(30) NOT NULL,
    receiver_address VARCHAR(255) NOT NULL COMMENT '下单时完整收货地址快照',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    payment_method VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '成功后固定MOCK',
    payment_no VARCHAR(40) NULL COMMENT '模拟成功凭证，未支付为空，可空唯一',
    paid_at DATETIME NULL,
    cancelled_at DATETIME NULL,
    shipped_at DATETIME NULL,
    completed_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_order_payment_no (payment_no),
    KEY idx_order_user_created (user_id, created_at, id),
    KEY idx_order_status_created (status, created_at, id),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES store_user (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_order_no CHECK (CHAR_LENGTH(TRIM(order_no)) > 0),
    CONSTRAINT ck_order_total CHECK (total_amount > 0),
    CONSTRAINT ck_order_receiver CHECK (
        CHAR_LENGTH(TRIM(receiver_name)) > 0 AND
        CHAR_LENGTH(TRIM(receiver_phone)) > 0 AND
        CHAR_LENGTH(TRIM(receiver_address)) > 0
    ),
    CONSTRAINT ck_order_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'SHIPPED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_order_payment_info CHECK (
        (status IN ('PENDING_PAYMENT', 'CANCELLED')
            AND payment_method IS NULL AND payment_no IS NULL AND paid_at IS NULL)
        OR
        (status IN ('PAID', 'SHIPPED', 'COMPLETED')
            AND payment_method IS NOT NULL AND payment_method = 'MOCK'
            AND payment_no IS NOT NULL AND CHAR_LENGTH(TRIM(payment_no)) > 0
            AND paid_at IS NOT NULL)
    ),
    CONSTRAINT ck_order_cancel_time CHECK (
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL)
        OR (status <> 'CANCELLED' AND cancelled_at IS NULL)
    ),
    CONSTRAINT ck_order_ship_time CHECK (
        (status IN ('SHIPPED', 'COMPLETED') AND shipped_at IS NOT NULL)
        OR (status NOT IN ('SHIPPED', 'COMPLETED') AND shipped_at IS NULL)
    ),
    CONSTRAINT ck_order_complete_time CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL)
        OR (status <> 'COMPLETED' AND completed_at IS NULL)
    ),
    CONSTRAINT ck_order_time_sequence CHECK (
        (paid_at IS NULL OR paid_at >= created_at) AND
        (cancelled_at IS NULL OR cancelled_at >= created_at) AND
        (shipped_at IS NULL OR shipped_at >= paid_at) AND
        (completed_at IS NULL OR completed_at >= shipped_at)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单及收货、模拟支付信息';

-- 7. 订单明细：保存成交时快照，不随商品后续改名改价而变化。
CREATE TABLE sales_order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_snapshot VARCHAR(40) NOT NULL,
    product_name_snapshot VARCHAR(120) NOT NULL,
    cover_image_snapshot VARCHAR(255) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    line_amount DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_item_product (order_id, product_id),
    KEY idx_order_item_product (product_id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES sales_order (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_order_item_sku CHECK (CHAR_LENGTH(TRIM(sku_snapshot)) > 0),
    CONSTRAINT ck_order_item_name CHECK (CHAR_LENGTH(TRIM(product_name_snapshot)) > 0),
    CONSTRAINT ck_order_item_cover CHECK (CHAR_LENGTH(TRIM(cover_image_snapshot)) > 0),
    CONSTRAINT ck_order_item_price CHECK (unit_price > 0),
    CONSTRAINT ck_order_item_quantity CHECK (quantity BETWEEN 1 AND 99),
    CONSTRAINT ck_order_item_amount CHECK (line_amount > 0 AND line_amount = unit_price * quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单商品快照与金额明细';

-- 8. 后台操作日志：由 @OperLog 注解 + AOP 切面写入，供后台审计与答辩演示。
CREATE TABLE oper_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operator VARCHAR(50) NULL COMMENT '操作人登录名，未登录为空',
    module VARCHAR(32) NOT NULL COMMENT '模块，如 商品管理',
    action VARCHAR(64) NOT NULL COMMENT '动作，如 新增商品',
    method VARCHAR(160) NULL COMMENT '类名.方法名',
    request_uri VARCHAR(255) NULL,
    params VARCHAR(2000) NULL COMMENT '入参摘要，注意脱敏',
    ip VARCHAR(64) NULL,
    cost_ms BIGINT NOT NULL DEFAULT 0 COMMENT '执行耗时毫秒',
    success TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_oper_log_created (created_at),
    KEY idx_oper_log_module_created (module, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台操作日志';

-- ===== 八张表的统一结构结束 =====

-- Service 仍需保证（数据库只兜底最基础的不变量）：
-- 1) 商品只能挂二级分类；二级必须指向一级；禁止三级或循环分类。
-- 2) 只有 ON_SALE 且 stock>0 的商品可被加购与下单；库存扣减与订单写入同一事务。
-- 3) 状态迁移合法：PENDING_PAYMENT->PAID->SHIPPED->COMPLETED，PENDING_PAYMENT->CANCELLED；
--    任何其他跳转（如 CANCELLED->PAID）必须拒绝。
-- 4) 取消未支付订单时恢复库存，并把 product.version 显式递增。
-- 5) 订单 total_amount 必须等于其明细 line_amount 之和（数据库无触发器，由事务内校验）。
-- 6) 权限与归属：顾客只能看自己的购物车和订单；后台操作需 ADMIN 角色。
-- 7) 密码用 BCryptPasswordEncoder 校验，绝不在 SQL 里做 MD5/SHA2 比较。
