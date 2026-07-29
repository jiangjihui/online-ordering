-- V2: 种子数据

-- 餐桌
INSERT INTO tables (id, number, area, capacity, status) VALUES
(1, 'A1', '大厅', 4, 'idle'),
(2, 'A2', '大厅', 4, 'dining'),
(3, 'A3', '大厅', 6, 'idle'),
(4, 'B1', '大厅', 8, 'reserved'),
(5, 'B2', '包间', 10, 'dining'),
(6, 'B3', '包间', 10, 'idle'),
(7, 'C1', '露天', 4, 'idle'),
(8, 'C2', '露天', 4, 'idle');

-- 分类
INSERT INTO categories (id, name, name_en, sort_order, icon) VALUES
(1, '热菜', 'Hot Dishes', 1, '🔥'),
(2, '凉菜', 'Cold Dishes', 2, '🥗'),
(3, '汤', 'Soups', 3, '🍲'),
(4, '主食', 'Staples', 4, '🍚'),
(5, '饮品', 'Drinks', 5, '🥤');

-- 菜品
INSERT INTO dishes (id, name, name_en, category_id, price, description, description_en, image, status, sold_out, is_spicy, labels, sort_order) VALUES
(1, '红烧肉', 'Braised Pork', 1, 38.00, '经典红烧肉，肥瘦相间', 'Classic braised pork, tender and flavorful', '', 'active', 0, 0, '[]', 1),
(2, '辣子鸡', 'Spicy Chicken', 1, 42.00, '香辣过瘾', 'Spicy and satisfying', '', 'active', 0, 3, '[]', 2),
(3, '鱼香肉丝', 'Yuxiang Pork', 1, 32.00, '酸甜微辣', 'Sweet, sour and mildly spicy', '', 'active', 0, 1, '[]', 3),
(4, '水煮牛肉', 'Boiled Beef in Chili', 1, 58.00, '麻辣鲜香', 'Tender beef in fiery chili broth', '', 'active', 1, 2, '[]', 4),
(5, '凉拌黄瓜', 'Cucumber Salad', 2, 12.00, '清爽开胃', 'Refreshing cucumber salad', '', 'active', 0, 0, '["vegetarian"]', 1),
(6, '皮蛋豆腐', 'Century Egg Tofu', 2, 15.00, '嫩滑爽口', 'Silky tofu with century egg', '', 'active', 0, 0, '[]', 2),
(7, '凉拌木耳', 'Wood Ear Salad', 2, 14.00, '脆爽可口', 'Crunchy wood ear mushroom salad', '', 'active', 0, 1, '["vegetarian"]', 3),
(8, '番茄蛋汤', 'Tomato Egg Soup', 3, 18.00, '家常暖汤', 'Homestyle warm soup', '', 'active', 0, 0, '[]', 1),
(9, '酸菜鱼汤', 'Sour Fish Soup', 3, 48.00, '酸辣鲜美', 'Tangy and spicy fish soup', '', 'active', 0, 2, '[]', 2),
(10, '蛋炒饭', 'Egg Fried Rice', 4, 18.00, '粒粒分明', 'Perfectly separated grains', '', 'active', 0, 0, '[]', 1),
(11, '炒面', 'Fried Noodles', 4, 16.00, '弹牙爽滑', 'Bouncy and smooth noodles', '', 'active', 0, 0, '[]', 2),
(12, '可乐', 'Cola', 5, 5.00, '冰镇可乐', 'Cold cola', '', 'active', 0, 0, '[]', 1),
(13, '酸梅汤', 'Plum Juice', 5, 8.00, '自制酸梅汤', 'Homemade plum juice', '', 'active', 0, 0, '[]', 2);

-- 套餐
INSERT INTO combos (id, name, name_en, price, description, status) VALUES
(1, '4人超值套餐', '4-Person Value Set', 128.00, '4人份量，含热菜2道+凉菜1道+汤1道+主食2份+饮品4杯', 'active'),
(2, '2人情侣套餐', '2-Person Couple Set', 68.00, '2人份量，含热菜1道+汤1道+主食2份+饮品2杯', 'active');

-- 套餐包含菜品
INSERT INTO combo_items (combo_id, dish_id, dish_name, quantity) VALUES
(1, 1, '红烧肉', 1),
(1, 3, '鱼香肉丝', 1),
(1, 5, '凉拌黄瓜', 1),
(1, 8, '番茄蛋汤', 1),
(1, 10, '蛋炒饭', 2),
(1, 12, '可乐', 4),
(2, 1, '红烧肉', 1),
(2, 8, '番茄蛋汤', 1),
(2, 10, '蛋炒饭', 2),
(2, 13, '酸梅汤', 2);

-- 订单
INSERT INTO orders (id, table_id, order_type, status, total_amount, remark, created_at) VALUES
(101, 2, 'scan', 'preparing', 82.00, '', '2026-07-28 18:30:00'),
(102, 5, 'server', 'preparing', 96.00, '不要葱', '2026-07-28 18:45:00');

-- 订单项
INSERT INTO order_items (order_id, dish_id, dish_name, quantity, remark, status, started_at) VALUES
(101, 1, '红烧肉', 1, '', 'preparing', '2026-07-28 18:31:00'),
(101, 5, '凉拌黄瓜', 2, '少放辣', 'completed', '2026-07-28 18:31:00'),
(101, 8, '番茄蛋汤', 1, '', 'pending', NULL),
(102, 2, '辣子鸡', 1, '', 'pending', NULL),
(102, 10, '蛋炒饭', 2, '不要葱', 'pending', NULL),
(102, 9, '酸菜鱼汤', 1, '', 'preparing', '2026-07-28 18:46:00');

UPDATE order_items SET completed_at = '2026-07-28 18:35:00' WHERE order_id = 101 AND dish_id = 5;

-- 服务员呼叫
INSERT INTO waiter_calls (table_id, table_number, created_at, status) VALUES
(2, 'A2', '2026-07-28 18:35:00', 'pending'),
(5, 'B2', '2026-07-28 18:40:00', 'pending');
