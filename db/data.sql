-- Interastral Peace Mall（星际和平商城）演示数据
-- 前置条件：已执行 db/schema.sql 建库建表。
-- 统一演示密码：PetStore123!   （BCrypt cost=10，每个账号独立盐）
-- 校验方式：BCryptPasswordEncoder.matches(rawPassword, password_hash)
-- 仅为本地教学数据，不可作为真实部署凭据。
-- 全部写入在一个事务中提交；MySQL 的 DDL 会独立提交，建表回滚不在本文件承诺范围内。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET SESSION time_zone = '+08:00';

USE interastral_mall;

START TRANSACTION;

-- ===== 1. 账号 =====
-- admin / alice / bob / disabled，密码统一 PetStore123!
INSERT INTO store_user
    (id, username, password_hash, display_name, role, status, email, phone)
VALUES
    (1, 'alice',   '$2a$10$L2rlMCJruRZDIL2pXB.BBOm7LiOuhGNmxWJHJH2n2QQ.BS5JLlsOe', '林星野', 'CUSTOMER', 'ENABLED',  'alice@example.test',   NULL),
    (2, 'bob',     '$2a$10$5f6JeeY8hi.NpV0nS7IbXep9siN.7Df4P8PLQVfhmZdq7EaLl80pm', '陈开拓', 'CUSTOMER', 'ENABLED',  'bob@example.test',     NULL),
    (3, 'disabled','$2a$10$nyqfhZXjqmbrVaijpcWHVOSo7nwuOgFLyFkkp9EM3wf5y0yiao.lS', '禁用教学账号', 'CUSTOMER', 'DISABLED', 'disabled@example.test', NULL),
    (4, 'admin',   '$2a$10$TWF0IW1UuBapXm2v.rwh2u9CemHHGhrjybt9CLxmNcXG1t6boGrtK', '商城管理员', 'ADMIN',   'ENABLED',  'admin@example.test',   NULL);

-- ===== 2. 分类（先一级，后二级）=====
INSERT INTO category (id, code, parent_id, name, sort_order) VALUES
    (1, 'IPC_DIGITAL', NULL, '星轨数码', 30),
    (2, 'IPC_FOOD',    NULL, '和平食刻', 20),
    (3, 'IPC_MERCH',   NULL, '舰载周边', 10);

INSERT INTO category (id, code, parent_id, name, sort_order) VALUES
    (101, 'IPC_DIGITAL_POWER',    1, '能源配件', 10),
    (102, 'IPC_DIGITAL_AUDIO',    1, '影音装备', 20),
    (201, 'IPC_FOOD_COFFEE',      2, '咖啡冲饮', 10),
    (202, 'IPC_FOOD_SNACK',       2, '零食甜点', 20),
    (301, 'IPC_MERCH_PLUSH',      3, '毛绒公仔', 10),
    (302, 'IPC_MERCH_STATIONERY', 3, '文具杂货', 20);

-- ===== 3. 商品 =====
-- 14 个上架（其中 1 个零库存用于售罄提示）、1 个下架用于后台筛选与上架练习。
-- 图片统一使用占位图，等有真实素材再把 cover_image 换成 images/products/<SKU>/1.jpg。
INSERT INTO product
    (id, sku, category_id, name, summary, description, cover_image, price, stock, status, sort_order, version)
VALUES
    (1,  'IPC-CB-01', 101, '曜金磁吸充电宝 20000mAh', '星轨数码能源主推款，支持边充边放。', '教学商品：20000mAh 磁吸无线充电宝，用于演示正常购买、加购与库存扣减。商品及价格仅用于教学演示。', 'images/products/placeholder-1.jpg', 219.00, 40, 'ON_SALE', 10, 0),
    (2,  'IPC-CB-02', 101, '星际探索者氮化镓充电器 65W', '双口快充，出差通勤都够用。', '教学商品：65W 氮化镓充电器，用于演示两位小数金额与常规下单。', 'images/products/placeholder-1.jpg', 149.00, 60, 'ON_SALE', 20, 0),
    (3,  'IPC-HP-01', 102, '星际漫游者主动降噪耳机', '长续航降噪，影音装备类目主推。', '教学商品：主动降噪蓝牙耳机，用于演示高价商品下单与订单金额计算。', 'images/products/placeholder-1.jpg', 599.00, 25, 'ON_SALE', 30, 0),
    (4,  'IPC-HP-02', 102, '开拓者轻量蓝牙耳机', '上架但零库存，用于售罄提示与加购拦截。', '教学商品：轻量蓝牙耳机，库存为 0，专门验证"售罄"分支与前端禁用按钮。', 'images/products/placeholder-1.jpg', 199.00, 0, 'ON_SALE', 40, 0),
    (5,  'IPC-CF-01', 201, '星穹挂耳咖啡 12 包', '中深烘焙，办公室常备装。', '教学商品：12 包挂耳咖啡，用于演示购物车合并数量与整车下单。', 'images/products/placeholder-1.jpg', 79.00, 200, 'ON_SALE', 50, 0),
    (6,  'IPC-CF-02', 201, '星穹挂耳咖啡 24 包', '整箱囤货款，单价更划算。', '教学商品：24 包挂耳咖啡，用于演示数量修改与库存不足场景。', 'images/products/placeholder-1.jpg', 149.00, 120, 'ON_SALE', 60, 0),
    (7,  'IPC-CF-03', 201, '帕姆拿铁冷萃液', '开瓶即饮，冷萃工艺。', '教学商品：冷萃咖啡液，用于演示低库存商品的购买上限。', 'images/products/placeholder-1.jpg', 89.00, 80, 'ON_SALE', 70, 0),
    (8,  'IPC-SN-01', 202, '星际补给站零食大礼包', '出差补给一站配齐。', '教学商品：零食大礼包，用于演示多件商品同单与金额合计。', 'images/products/placeholder-1.jpg', 129.00, 50, 'ON_SALE', 80, 0),
    (9,  'IPC-SN-02', 202, '黑塔曲奇礼盒', '黄油曲奇，附赠贴纸。', '教学商品：曲奇礼盒，用于演示购物车多商品勾选与部分结算。', 'images/products/placeholder-1.jpg', 69.00, 35, 'ON_SALE', 90, 0),
    (10, 'IPC-PL-01', 301, '帕姆毛绒公仔 30cm', '舰载周边人气款。', '教学商品：30cm 毛绒公仔，用于演示周边类商品的浏览与下单。', 'images/products/placeholder-1.jpg', 129.00, 80, 'ON_SALE', 100, 0),
    (11, 'IPC-PL-02', 301, '帕姆毛绒公仔 50cm', '加大版，抱起来刚好。', '教学商品：50cm 毛绒公仔，用于演示与 30cm 款的对比浏览。', 'images/products/placeholder-1.jpg', 229.00, 30, 'ON_SALE', 110, 0),
    (12, 'IPC-PL-03', 302, '星穹列车徽章套装', '五枚装，金属烤漆。', '教学商品：徽章套装，用于演示低单价商品的数量累加。', 'images/products/placeholder-1.jpg', 59.00, 150, 'ON_SALE', 120, 0),
    (13, 'IPC-ST-01', 302, '星穹主题硬壳笔记本', 'A5 尺寸，内页可平摊。', '教学商品：硬壳笔记本，用于演示文具类商品的详情页。', 'images/products/placeholder-1.jpg', 45.00, 100, 'ON_SALE', 130, 0),
    (14, 'IPC-ST-02', 302, '星际和平商社钢笔', '金属笔身，附礼盒。', '教学商品：商社钢笔，用于演示礼盒类商品的加购流程。', 'images/products/placeholder-1.jpg', 128.00, 20, 'ON_SALE', 140, 0),
    (15, 'IPC-ST-03', 302, '限定亚克力立牌', '桌面小摆件，限量发售。', '教学商品：亚克力立牌，用于演示低库存商品的购买上限提示。', 'images/products/placeholder-1.jpg', 39.00, 60, 'ON_SALE', 150, 0),
    (16, 'IPC-LTD-01', 301, '开拓者典藏礼盒', '下架商品，不得出现在用户端上架列表。', '教学商品：典藏礼盒，状态 DRAFT，用于验证后台筛选与"上架"操作。', 'images/products/placeholder-1.jpg', 999.00, 5, 'DRAFT', 160, 0);

-- ===== 4. 商品相册 =====
-- 只给前 6 个商品配相册，其余商品详情页按规约回退到封面，正好覆盖两种渲染分支。
INSERT INTO product_image (id, product_id, image_path, alt_text, sort_order) VALUES
    (1,  1, 'images/products/placeholder-1.jpg', '曜金磁吸充电宝·展示图1', 10),
    (2,  1, 'images/products/placeholder-2.jpg', '曜金磁吸充电宝·展示图2', 20),
    (3,  2, 'images/products/placeholder-1.jpg', '星际探索者氮化镓充电器·展示图1', 10),
    (4,  2, 'images/products/placeholder-2.jpg', '星际探索者氮化镓充电器·展示图2', 20),
    (5,  3, 'images/products/placeholder-1.jpg', '星际漫游者主动降噪耳机·展示图1', 10),
    (6,  3, 'images/products/placeholder-2.jpg', '星际漫游者主动降噪耳机·展示图2', 20),
    (7,  4, 'images/products/placeholder-1.jpg', '开拓者轻量蓝牙耳机·展示图1', 10),
    (8,  4, 'images/products/placeholder-2.jpg', '开拓者轻量蓝牙耳机·展示图2', 20),
    (9,  5, 'images/products/placeholder-1.jpg', '星穹挂耳咖啡 12 包·展示图1', 10),
    (10, 5, 'images/products/placeholder-2.jpg', '星穹挂耳咖啡 12 包·展示图2', 20),
    (11, 6, 'images/products/placeholder-1.jpg', '星穹挂耳咖啡 24 包·展示图1', 10),
    (12, 6, 'images/products/placeholder-2.jpg', '星穹挂耳咖啡 24 包·展示图2', 20);

-- ===== 5. 订单 =====
-- alice 4 单、bob 2 单，覆盖五种状态；已支付订单统一 MOCK 凭证。
-- 时间以本次导入为基准向前推算，状态时间严格满足 ck_order_time_sequence。
-- 外键全部按 username / sku 查找，不写死自增 id。
INSERT INTO sales_order
    (order_no, user_id, status, total_amount, receiver_name, receiver_phone, receiver_address,
     created_at, payment_method, payment_no, paid_at, cancelled_at, shipped_at, completed_at)
SELECT t.order_no,
       u.id,
       t.status,
       t.total_amount,
       t.receiver_name,
       t.receiver_phone,
       t.receiver_address,
       t.created_at,
       CASE WHEN t.status IN ('PAID', 'SHIPPED', 'COMPLETED') THEN 'MOCK' END,
       CASE WHEN t.status IN ('PAID', 'SHIPPED', 'COMPLETED') THEN CONCAT('MOCK-', t.order_no) END,
       CASE WHEN t.status IN ('PAID', 'SHIPPED', 'COMPLETED') THEN DATE_ADD(t.created_at, INTERVAL 10 MINUTE) END,
       CASE WHEN t.status = 'CANCELLED' THEN DATE_ADD(t.created_at, INTERVAL 30 MINUTE) END,
       CASE WHEN t.status IN ('SHIPPED', 'COMPLETED') THEN DATE_ADD(t.created_at, INTERVAL 24 HOUR) END,
       CASE WHEN t.status = 'COMPLETED' THEN DATE_ADD(t.created_at, INTERVAL 72 HOUR) END
FROM (
    SELECT 'DEMO-202611-AL-01' AS order_no, 'alice' AS username, 'PENDING_PAYMENT' AS status,
           217.00 AS total_amount, '林星野' AS receiver_name, '13800000001' AS receiver_phone,
           '教学示例地址：星轨市和平区开拓路 1 号（非真实收货地址）' AS receiver_address,
           NOW() - INTERVAL 1 HOUR AS created_at
    UNION ALL SELECT 'DEMO-202611-AL-02', 'alice', 'PAID',     818.00, '林星野', '13800000001', '教学示例地址：星轨市和平区开拓路 1 号（非真实收货地址）', NOW() - INTERVAL 25 HOUR
    UNION ALL SELECT 'DEMO-202611-AL-03', 'alice', 'SHIPPED',  218.00, '林星野', '13800000001', '教学示例地址：星轨市和平区开拓路 1 号（非真实收货地址）', NOW() - INTERVAL 72 HOUR
    UNION ALL SELECT 'DEMO-202611-AL-04', 'alice', 'COMPLETED',174.00, '林星野', '13800000001', '教学示例地址：星轨市和平区开拓路 1 号（非真实收货地址）', NOW() - INTERVAL 168 HOUR
    UNION ALL SELECT 'DEMO-202611-BO-01', 'bob',   'CANCELLED',599.00, '陈开拓', '13800000002', '教学示例地址：星轨市和平区开拓路 2 号（非真实收货地址）', NOW() - INTERVAL 48 HOUR
    UNION ALL SELECT 'DEMO-202611-BO-02', 'bob',   'PAID',     218.00, '陈开拓', '13800000002', '教学示例地址：星轨市和平区开拓路 2 号（非真实收货地址）', NOW() - INTERVAL 49 HOUR
) t
JOIN store_user u ON u.username = t.username;

-- ===== 6. 订单明细（成交快照）=====
INSERT INTO sales_order_item
    (order_id, product_id, sku_snapshot, product_name_snapshot, cover_image_snapshot,
     unit_price, quantity, line_amount)
SELECT o.id,
       p.id,
       p.sku,
       p.name,
       p.cover_image,
       t.unit_price,
       t.quantity,
       t.unit_price * t.quantity
FROM (
    SELECT 'DEMO-202611-AL-01' AS order_no, 'IPC-CF-01' AS sku,  2 AS quantity,  79.00 AS unit_price
    UNION ALL SELECT 'DEMO-202611-AL-01', 'IPC-PL-03', 1,  59.00
    UNION ALL SELECT 'DEMO-202611-AL-02', 'IPC-CB-01', 1, 219.00
    UNION ALL SELECT 'DEMO-202611-AL-02', 'IPC-HP-01', 1, 599.00
    UNION ALL SELECT 'DEMO-202611-AL-03', 'IPC-SN-01', 1, 129.00
    UNION ALL SELECT 'DEMO-202611-AL-03', 'IPC-CF-03', 1,  89.00
    UNION ALL SELECT 'DEMO-202611-AL-04', 'IPC-PL-01', 1, 129.00
    UNION ALL SELECT 'DEMO-202611-AL-04', 'IPC-ST-01', 1,  45.00
    UNION ALL SELECT 'DEMO-202611-BO-01', 'IPC-HP-01', 1, 599.00
    UNION ALL SELECT 'DEMO-202611-BO-02', 'IPC-CF-02', 1, 149.00
    UNION ALL SELECT 'DEMO-202611-BO-02', 'IPC-SN-02', 1,  69.00
) t
JOIN sales_order o ON o.order_no = t.order_no
JOIN product p ON p.sku = t.sku;

-- ===== 7. 按已成交订单扣减库存 =====
-- 与业务约定一致：下单即扣减，取消订单已恢复，因此 CANCELLED 不计入。
-- 同时显式递增 product.version，模拟业务层"写商品必升版本"的约定。
UPDATE product p
JOIN (
    SELECT oi.sku_snapshot AS sku, SUM(oi.quantity) AS qty
    FROM sales_order_item oi
    JOIN sales_order o ON o.id = oi.order_id
    WHERE o.status <> 'CANCELLED'
    GROUP BY oi.sku_snapshot
) s ON s.sku = p.sku
SET p.stock = p.stock - s.qty,
    p.version = p.version + 1;

-- ===== 8. 当前购物车 =====
-- 与历史订单彼此独立：已下单的商品不会再出现在购物车里。
INSERT INTO cart_item (user_id, product_id, quantity, version)
SELECT u.id, p.id, t.quantity, 0
FROM (
    SELECT 'alice' AS username, 'IPC-HP-01' AS sku, 1 AS quantity
    UNION ALL SELECT 'alice', 'IPC-CF-01', 3
    UNION ALL SELECT 'alice', 'IPC-PL-01', 1
    UNION ALL SELECT 'bob',   'IPC-SN-01', 2
    UNION ALL SELECT 'bob',   'IPC-CF-02', 1
) t
JOIN store_user u ON u.username = t.username
JOIN product p ON p.sku = t.sku;

-- ===== 9. 操作日志示例 =====
-- 演示后台日志列表的展示效果；真实数据由 @OperLog 切面在运行时写入。
INSERT INTO oper_log
    (operator, module, action, method, request_uri, params, ip, cost_ms, success, created_at)
VALUES
    ('admin', '商品管理', '新增商品', 'AdminProductController.create', '/admin/product/create',
     'sku=IPC-LTD-01', '127.0.0.1', 38, 1, NOW() - INTERVAL 3 HOUR),
    ('admin', '订单管理', '订单发货', 'AdminOrderController.ship', '/admin/order/ship',
     'orderNo=DEMO-202611-AL-03', '127.0.0.1', 52, 1, NOW() - INTERVAL 2 HOUR);

COMMIT;

-- ===== 导入后自检 =====
-- SELECT COUNT(*) FROM store_user;        -- 期望 4
-- SELECT COUNT(*) FROM category;          -- 期望 9
-- SELECT COUNT(*) FROM product;           -- 期望 16
-- SELECT COUNT(*) FROM sales_order;       -- 期望 6
-- SELECT COUNT(*) FROM sales_order_item;  -- 期望 11
-- SELECT COUNT(*) FROM cart_item;         -- 期望 5
-- SELECT sku, stock, version FROM product ORDER BY id;   -- IPC-CF-01 期望 198/1
