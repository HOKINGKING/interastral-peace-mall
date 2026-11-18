-- Interastral Peace Mall（星际和平商城）演示数据
-- 前置：已执行 db/schema.sql。分类与商品直接取自 HTML 原型（prototype/_build.mjs 的 CATS / PRODUCTS）。
-- 演示密码统一为 PetStore123!（BCrypt cost=10），前台 xingye / bob，后台 admin。
-- 说明：item.stock 是"扣除下列示例订单后"的当前库存快照，导入时不再二次扣减，
--       避免与原型展示的库存数字不一致。
-- 先在空库执行；重复执行会因唯一索引/主键冲突而失败，重置请重新建库。

SET NAMES utf8mb4;
SET SESSION time_zone = '+08:00';
USE interastral_mall;

START TRANSACTION;

-- ===== 1. 前台账号 =====
INSERT INTO account (id, username, password, nickname, phone, status, last_login_time) VALUES
    (1, 'xingye',   '$2a$10$L2rlMCJruRZDIL2pXB.BBOm7LiOuhGNmxWJHJH2n2QQ.BS5JLlsOe', '星野',   '13800000001', 1, NOW() - INTERVAL 2 HOUR),
    (2, 'bob',      '$2a$10$5f6JeeY8hi.NpV0nS7IbXep9siN.7Df4P8PLQVfhmZdq7EaLl80pm', '陈开拓', '13800000002', 1, NULL),
    (3, 'disabled', '$2a$10$nyqfhZXjqmbrVaijpcWHVOSo7nwuOgFLyFkkp9EM3wf5y0yiao.lS', '禁用账号', NULL,       0, NULL);

-- ===== 2. 后台账号 =====
INSERT INTO admin (id, username, password, nickname, status, last_login_time) VALUES
    (1, 'admin', '$2a$10$TWF0IW1UuBapXm2v.rwh2u9CemHHGhrjybt9CLxmNcXG1t6boGrtK', '平台管理员', 1, NOW() - INTERVAL 1 HOUR);

-- ===== 3. 分类：6 个一级 + 16 个二级（与原型 CATS 一致）=====
INSERT INTO category (id, parent_id, name, descn, sort, visible) VALUES
    (1, 0, '家居日用', '把星海搬进日常', 10, 1),
    (2, 0, '文创周边', '官方授权文创', 20, 1),
    (3, 0, '服饰箱包', '轨道制衣出品', 30, 1),
    (4, 0, '模型手办', '深空模型社监修', 40, 1),
    (5, 0, '数码配件', '轨道外设联名', 50, 1),
    (6, 0, '礼盒套装', '节日与办公礼盒', 60, 1);

INSERT INTO category (id, parent_id, name, sort, visible) VALUES
    (101, 1, '杯壶',     10, 1),
    (102, 1, '摆件',     20, 1),
    (103, 1, '香薰',     30, 1),
    (201, 2, '徽章',     10, 1),
    (202, 2, '海报',     20, 1),
    (203, 2, '笔记本',   30, 1),
    (301, 3, '帆布包',   10, 1),
    (302, 3, 'T恤',      20, 1),
    (303, 3, '帽子',     30, 1),
    (401, 4, '舰船模型', 10, 1),
    (402, 4, '人物手办', 20, 1),
    (501, 5, '耳机',     10, 1),
    (502, 5, '鼠标垫',   20, 1),
    (503, 5, '手机壳',   30, 1),
    (601, 6, '节日礼盒', 10, 1),
    (602, 6, '办公套装', 20, 1);

-- ===== 4. 商品 SPU（10 个，与原型 PRODUCTS 一致）=====
INSERT INTO product (id, category_id, name, brand, descn, detail, main_image, status) VALUES
    (1,  101, '星际和平水杯 · 500ml', '和平号工作室', '船用级不锈钢内胆，杯盖带星轨浮雕。',
     '官方周边直营：500ml 容量，双层真空保温 6 小时，杯身烫印星际和平公司徽记。', NULL, 1),
    (2,  301, '星轨帆布包 · 加厚版', '轨道制衣', '16 安加厚帆布，可装 14 寸笔电。',
     '加厚帆布 + 内衬分层，肩带加宽不勒肩，正面丝印星轨图案。', NULL, 1),
    (3,  401, '舰桥模型 · 纪念版', '深空模型社', '1:1000 舰桥模型，附亚克力底座。',
     '合金压铸一体成型，底座激光刻印编号，适合桌面陈列。', NULL, 1),
    (4,  201, '驾驶员徽章套装（6 枚）', '和平号工作室', '金属烤漆，六位驾驶员徽记。',
     '锌合金材质，烤漆工艺，背面蝴蝶扣，随附收藏盒。', NULL, 1),
    (5,  103, '深空香薰蜡烛 · 冷杉', '慢燃实验室', '冷杉与雪松调，燃烧时长约 30 小时。',
     '大豆蜡基底，无烟燃烧，玻璃杯可二次利用。', NULL, 1),
    (6,  202, '命运星图海报 · A2', '星图印社', 'A2 尺寸，星轨夜光油墨。',
     '200g 哑粉纸，夜光油墨印刷，暗环境下可见星轨。', NULL, 1),
    (7,  502, '联名机械键盘 · 86 键', '轨道外设', '86 键紧凑布局，热插拔轴座。',
     '全键无冲，PBT 键帽，附赠星轨主题替换键帽一套。', NULL, 1),
    (8,  203, '星际和平笔记本 · 方格', '和平号工作室', 'A5 方格本，可 180 度平摊。',
     '100g 道林纸，硬壳封面，内页 80 页方格。', NULL, 1),
    (9,  102, '轨道托盘 · 合金', '慢燃实验室', '合金压铸，可作首饰/钥匙托。',
     '磨砂阳极氧化处理，底部防滑硅胶垫。', NULL, 1),
    (10, 102, '星球夜灯 · 暖光', '慢燃实验室', '触控三档调光，充电款。',
     '星球纹理手工上色，USB-C 充电，满电可用约 12 小时。', NULL, 1);

-- ===== 5. 商品 SKU（价格与库存都在这一层）=====
INSERT INTO item (id, product_id, spec, price, stock, image, version) VALUES
    (1,  1,  '500ml 星白',            59.00, 128, NULL, 0),
    (2,  1,  '500ml 深空蓝',          59.00,  96, NULL, 0),
    (3,  2,  '加厚版 · 深空蓝',        89.00,  64, NULL, 0),
    (4,  2,  '加厚版 · 曜金',          99.00,  40, NULL, 0),
    (5,  3,  '纪念版 · 标准',         299.00,  12, NULL, 0),
    (6,  4,  '6 枚装',                39.00, 240, NULL, 0),
    (7,  5,  '冷杉 120g',             129.00,  36, NULL, 0),
    (8,  6,  'A2 无框',                49.00,  88, NULL, 0),
    (9,  7,  '86 键 · 白轴',          399.00,  21, NULL, 0),
    (10, 8,  '方格 · A5',              29.00,   0, NULL, 0),
    (11, 9,  '合金 · 小号',            69.00,  54, NULL, 0),
    (12, 10, '暖光 · 充电款',         159.00,  18, NULL, 0);

-- ===== 6. 收货地址 =====
INSERT INTO address (id, account_id, receiver, phone, province, city, district, detail, is_default) VALUES
    (1, 1, '星野', '13800000001', '湖南省', '长沙市', '岳麓区', '星轨大道 88 号和平公寓 3 栋 502', 1),
    (2, 1, '星野', '13800000001', '湖南省', '长沙市', '开福区', '开拓路 12 号 2 单元 901', 0);

-- ===== 7. 订单（覆盖 5 种状态）=====
INSERT INTO orders
    (id, order_no, account_id, total_amount, freight_amount, status,
     receiver_name, receiver_phone, receiver_province, receiver_city, receiver_district, receiver_detail,
     courier_company, tracking_no, pay_time, ship_time, finish_time, cancel_time, create_time)
VALUES
    (1, '20261117201501', 1, 148.00, 0.00, 'PENDING_PAYMENT', '星野', '13800000001', '湖南省', '长沙市', '岳麓区', '星轨大道 88 号和平公寓 3 栋 502',
     NULL, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL 20 MINUTE),
    (2, '20261116102133', 1, 129.00, 0.00, 'COMPLETED', '星野', '13800000001', '湖南省', '长沙市', '岳麓区', '星轨大道 88 号和平公寓 3 栋 502',
     '星际速运', 'IPC202611160001', NOW() - INTERVAL 30 HOUR, NOW() - INTERVAL 20 HOUR, NOW() - INTERVAL 2 HOUR, NULL, NOW() - INTERVAL 31 HOUR),
    (3, '20261115093020', 1,  69.00, 0.00, 'CANCELLED', '星野', '13800000001', '湖南省', '长沙市', '岳麓区', '星轨大道 88 号和平公寓 3 栋 502',
     NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL 50 HOUR, NOW() - INTERVAL 52 HOUR),
    (4, '20261117084412', 1, 299.00, 0.00, 'SHIPPED', '星野', '13800000001', '湖南省', '长沙市', '开福区', '开拓路 12 号 2 单元 901',
     '星际速运', 'IPC202611170002', NOW() - INTERVAL 26 HOUR, NOW() - INTERVAL 3 HOUR, NULL, NULL, NOW() - INTERVAL 27 HOUR),
    (5, '20261117201948', 2,  78.00, 0.00, 'PAID', '陈开拓', '13800000002', '上海市', '上海市', '浦东新区', '航道街 10 号 1 单元 601',
     NULL, NULL, NOW() - INTERVAL 8 MINUTE, NULL, NULL, NULL, NOW() - INTERVAL 15 MINUTE);

-- ===== 8. 订单明细（全快照）=====
INSERT INTO order_item (order_id, item_id, product_name, spec, price, quantity, image, create_time) VALUES
    (1, 1, '星际和平水杯 · 500ml', '500ml 星白',      59.00, 1, NULL, NOW() - INTERVAL 20 MINUTE),
    (1, 3, '星轨帆布包 · 加厚版', '加厚版 · 深空蓝',   89.00, 1, NULL, NOW() - INTERVAL 20 MINUTE),
    (2, 7, '深空香薰蜡烛 · 冷杉', '冷杉 120g',        129.00, 1, NULL, NOW() - INTERVAL 31 HOUR),
    (3, 11, '轨道托盘 · 合金',    '合金 · 小号',        69.00, 1, NULL, NOW() - INTERVAL 52 HOUR),
    (4, 5, '舰桥模型 · 纪念版',   '纪念版 · 标准',     299.00, 1, NULL, NOW() - INTERVAL 27 HOUR),
    (5, 6, '驾驶员徽章套装（6 枚）', '6 枚装',           39.00, 2, NULL, NOW() - INTERVAL 15 MINUTE);

-- ===== 9. 购物车（xingye 当前选购）=====
INSERT INTO cart (account_id, item_id, quantity) VALUES
    (1, 6, 2),
    (1, 8, 1);

-- ===== 10. 投诉与反馈（与原型 FEEDBACKS 一致）=====
INSERT INTO feedback
    (type, account_id, order_id, category, content, contact, status,
     reply, reply_admin_id, reply_time, close_reason, create_time)
VALUES
    ('COMPLAINT', 1, 2, 'QUALITY',
     '蜡烛收到时玻璃杯有一道细裂纹，外包装完好，应该是出厂检验没发现。希望换一个或者退部分款。',
     NULL, 'REPLIED',
     '已核实为运输前瑕疵，为您补发一件并赠送香薰试用装，预计 48 小时内发出。',
     1, NOW() - INTERVAL 26 HOUR, NULL, NOW() - INTERVAL 28 HOUR),
    ('FEEDBACK', 1, NULL, 'OTHER',
     '希望商品详情页能标出模型的实际尺寸，现在只能靠图片估计，容易买大。',
     '13800000001', 'PENDING',
     NULL, NULL, NULL, NULL, NOW() - INTERVAL 4 HOUR);

-- ===== 11. 操作日志示例 =====
INSERT INTO sys_oper_log
    (admin_id, admin_name, module, action, target_id, request_uri, request_method, request_params,
     ip, success, cost_ms, create_time)
VALUES
    (1, 'admin', '订单管理', '发货', 4, '/admin/order/ship', 'POST',
     '{"orderId":4,"courierCompany":"星际速运","trackingNo":"IPC202611170002"}',
     '127.0.0.1', 1, 42, NOW() - INTERVAL 3 HOUR),
    (1, 'admin', '商品管理', '上架', 10, '/admin/product/toggle', 'POST',
     '{"productId":10,"status":1}',
     '127.0.0.1', 1, 18, NOW() - INTERVAL 2 HOUR);

INSERT INTO sys_check (name) VALUES ('星际和平商城初始化成功');

COMMIT;

-- ===== 导入后自检 =====
-- SELECT COUNT(*) FROM account;      -- 3
-- SELECT COUNT(*) FROM admin;        -- 1
-- SELECT COUNT(*) FROM category;     -- 22（6 一级 + 16 二级）
-- SELECT COUNT(*) FROM product;      -- 10
-- SELECT COUNT(*) FROM item;         -- 12
-- SELECT COUNT(*) FROM orders;       -- 5
-- SELECT COUNT(*) FROM order_item;   -- 6
-- SELECT COUNT(*) FROM feedback;     -- 2
-- 校验订单总额 = Σ(明细价 × 数量)：
-- SELECT o.order_no, o.total_amount, SUM(i.price * i.quantity) detail_sum
-- FROM orders o JOIN order_item i ON i.order_id = o.id GROUP BY o.id;
