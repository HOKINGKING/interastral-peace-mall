-- ============================================================
-- 增量脚本：支付流水表（PRD §5.12 / §7.3 payment_record）
-- 适用：已经执行过 db/schema.sql + db/data.sql 的已有开发库。
-- 空库不需要执行本文件，直接跑 schema.sql 即可。
-- 可重复执行：CREATE TABLE IF NOT EXISTS + NOT EXISTS 去重。
-- ============================================================
USE interastral_mall;

CREATE TABLE IF NOT EXISTS payment_record (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    order_id       BIGINT        NOT NULL COMMENT '订单 id，一单最多一条成功流水',
    order_no       VARCHAR(32)   NOT NULL COMMENT '订单号快照，对账不必回查 orders',
    account_id     BIGINT        NOT NULL COMMENT '支付用户',
    amount         DECIMAL(10,2) NOT NULL COMMENT '支付金额（订单应付快照）',
    pay_method     VARCHAR(20)   NOT NULL DEFAULT 'MOCK' COMMENT 'MOCK 模拟支付',
    status         VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS 支付成功',
    transaction_no VARCHAR(64)   NOT NULL COMMENT '支付流水号，唯一',
    pay_time       DATETIME      NOT NULL,
    deleted        TINYINT       NOT NULL DEFAULT 0,
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_order (order_id),
    UNIQUE KEY uk_payment_transaction (transaction_no),
    KEY idx_payment_account_time (account_id, pay_time),
    CONSTRAINT ck_payment_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='支付流水';

-- 回填历史已支付订单（PAID / SHIPPED / COMPLETED 且有 pay_time）
INSERT INTO payment_record
    (order_id, order_no, account_id, amount, pay_method, status, transaction_no, pay_time, create_time)
SELECT o.id,
       o.order_no,
       o.account_id,
       o.total_amount,
       'MOCK',
       'SUCCESS',
       CONCAT('PAY', o.order_no),
       o.pay_time,
       o.pay_time
FROM orders o
WHERE o.pay_time IS NOT NULL
  AND o.status IN ('PAID', 'SHIPPED', 'COMPLETED')
  AND NOT EXISTS (SELECT 1 FROM payment_record p WHERE p.order_id = o.id);
