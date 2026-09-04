-- ============================================================
-- mall_auth 数据库全量脚本（删库重建版 · 菜单数据权威版）
-- 执行数据库：mall_auth（先执行 db/database.sql 建库）
--
-- 菜单数据基准：2026-08-22 线上 sys_menu 实际数据整理
--   · 含 800 数据分析 / 801 首页广告 / 960 关联秒杀商品 / 961 批量发布商品
--     / 964 审核通过 / 966 页脚链接 / 967 快捷导航
--   · 每个业务管理菜单补齐「修改 / 删除」按钮权限（前端 v-perms 与后端
--     @RequirePermission 均已引用），并补前端引用的 新增 权限：
--       - 订单列表(30)：新增 order:order:update / order:order:delete
--       - 销售属性(205)：新增 product:saleattr:update / product:saleattr:delete
--       - SPU管理(206)：新增 product:spu:update
--       - 优惠券管理(601)：新增 coupon:coupon:save（前端 v-perms 已引用）
--       - 发放记录(602)：新增 coupon:history:update
--       - 页脚链接(966)：新增 content:footerlink:save / update / delete
--       - 快捷导航(967)：新增 content:homenav:save / update / delete
--   · 修复线上 966 权限串笔误 cotent:footerlink:list → content:footerlink:list
--
-- 本脚本是 sys_menu / sys_role_menu 的唯一权威：重跑会清空两表后重建，
-- 请勿再叠加执行历史分片脚本（sys_menus.sql / sys_permission_buttons.sql 等）。
-- 其余表（sys_user / sys_role / 关联表 / sys_log）幂等，可重复执行。
-- ============================================================

-- ============================================================
-- 1. 表结构
-- ============================================================

-- 系统用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `salt` varchar(20) DEFAULT NULL COMMENT '盐',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `mobile` varchar(20) DEFAULT NULL COMMENT '手机号',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `avatar` varchar(200) DEFAULT NULL COMMENT '头像URL',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 系统角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) DEFAULT NULL COMMENT '角色编码',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 系统菜单/权限表
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `parent_id` bigint DEFAULT 0 COMMENT '父菜单ID',
  `name` varchar(50) NOT NULL COMMENT '菜单名称',
  `url` varchar(200) DEFAULT NULL COMMENT '菜单URL',
  `perms` varchar(200) DEFAULT NULL COMMENT '授权标识（如 sys:user:list）',
  `type` tinyint DEFAULT 0 COMMENT '类型：0-目录 1-菜单 2-按钮',
  `icon` varchar(50) DEFAULT NULL COMMENT '菜单图标',
  `order_num` int DEFAULT 0 COMMENT '排序',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`menu_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单表';

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色-菜单关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `sys_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '操作用户名',
  `operation` varchar(100) DEFAULT NULL COMMENT '操作描述',
  `method` varchar(200) DEFAULT NULL COMMENT '请求方法',
  `params` text DEFAULT NULL COMMENT '请求参数',
  `ip` varchar(64) DEFAULT NULL COMMENT 'IP地址',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-失败 1-成功',
  `error_msg` text DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ============================================================
-- 2. 基础数据：超级管理员 / 角色
-- ============================================================

-- 超级管理员（密码: admin123, BCrypt加密）
INSERT IGNORE INTO `sys_user` (`user_id`, `username`, `password`, `real_name`, `status`) VALUES
(1, 'admin', '$2a$10$DnXZ3KjCFNMs.Iipfu/4reg6Z.mGwySTkcPy6vFW5q2W42RZjFsJy', '超级管理员', 1);

-- 初始化角色
INSERT IGNORE INTO `sys_role` (`role_id`, `role_name`, `role_code`, `remark`, `status`) VALUES
(1, '超级管理员', 'SUPER_ADMIN', '拥有全部权限', 1),
(2, '普通管理员', 'ADMIN', '普通管理员权限', 1),
(3, '运营人员', 'OPERATOR', '运营相关权限', 1);

-- 超级管理员拥有全部角色（角色绑定在菜单重建后统一执行）
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- ============================================================
-- 3. 菜单数据（权威重建）
--    type：0-目录 1-菜单 2-按钮
--    排序：顶级 数据分析(800)/系统(1)/商品(2)/订单(3)/库存(4)/会员(5)/优惠(6)/任务(7)/首页广告(801)/秒杀(613)
-- ============================================================

-- 3.0 清理旧菜单与角色绑定（本脚本为唯一权威）
DELETE FROM `sys_role_menu`;
DELETE FROM `sys_menu`;

-- 3.1 顶级目录/菜单
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(800, 0, '数据分析', 'home', NULL, 1, 'home', 0, 1),
(1, 0, '系统管理', NULL, NULL, 0, 'setting', 1, 1),
(2, 0, '商品管理', NULL, NULL, 0, 'goods', 2, 1),
(3, 0, '订单管理', NULL, NULL, 0, 'order', 3, 1),
(4, 0, '库存系统', NULL, NULL, 0, 'warehouse', 4, 1),
(5, 0, '会员系统', NULL, NULL, 0, 'member', 5, 1),
(6, 0, '优惠营销', NULL, NULL, 0, 'coupon', 6, 1),
(7, 0, '任务调度', NULL, NULL, 0, 'schedule', 7, 1),
(801, 0, '首页广告', NULL, NULL, 0, 'content', 8, 1),
(613, 0, '秒杀管理', NULL, NULL, 0, 'seckill', 9, 1);

-- 3.2 系统管理(1)
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(10, 1, '用户管理', '/sys/user', 'sys:user:list', 1, 'user', 0, 1),
(11, 1, '角色管理', '/sys/role', 'sys:role:list', 1, 'role', 1, 1),
(12, 1, '菜单管理', '/sys/menu', 'sys:menu:list', 1, 'menu', 2, 1),
(13, 1, '操作日志', '/sys/log', 'sys:log:list', 1, 'log', 3, 1);

-- 用户管理(10) 按钮
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(100, 10, '新增用户', NULL, 'sys:user:save', 2, NULL, 0, 1),
(101, 10, '修改用户', NULL, 'sys:user:update', 2, NULL, 1, 1),
(102, 10, '删除用户', NULL, 'sys:user:delete', 2, NULL, 2, 1),
(103, 10, '重置密码', NULL, 'sys:user:resetpwd', 2, NULL, 3, 1);

-- 角色管理(11) 按钮
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(110, 11, '新增角色', NULL, 'sys:role:save', 2, NULL, 0, 1),
(111, 11, '修改角色', NULL, 'sys:role:update', 2, NULL, 1, 1),
(112, 11, '删除角色', NULL, 'sys:role:delete', 2, NULL, 2, 1);

-- 菜单管理(12) 按钮
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(120, 12, '新增菜单', NULL, 'sys:menu:save', 2, NULL, 0, 1),
(121, 12, '修改菜单', NULL, 'sys:menu:update', 2, NULL, 1, 1),
(122, 12, '删除菜单', NULL, 'sys:menu:delete', 2, NULL, 2, 1);

-- 3.3 商品管理(2)
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(201, 2, '分类管理', '/category', 'product:category:list', 1, 'folder', 0, 1),
(202, 2, '品牌管理', '/trademark', 'product:brand:list', 1, 'brand', 1, 1),
(209, 2, '平台属性', NULL, NULL, 0, 'attr', 2, 1),
(210, 2, '商品维护', NULL, NULL, 0, 'spu', 3, 1);

-- 平台属性(209) 子菜单
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(203, 209, '属性分组', '/attrgroup', 'product:attrgroup:list', 1, 'group', 0, 1),
(204, 209, '规则参数', '/baseattr', 'product:baseattr:list', 1, 'tools', 1, 1),
(205, 209, '销售属性', '/saleattr', 'product:saleattr:list', 1, 'sell', 2, 1);

-- 商品维护(210) 子菜单
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(206, 210, 'SPU管理', '/spu/index', 'product:spu:list', 1, 'edit', 0, 1),
(207, 210, '发布商品', '/spu/add', 'product:spu:add', 1, 'add', 1, 1),
(208, 210, '商品列表', '/manager', 'product:manager:list', 1, 'view', 2, 1),
(961, 210, '批量发布商品', '/batchPublish', 'product:batchPublish:list', 1, 'Search', 4, 1);

-- 商品 按钮：删除(901-906) + 修改(936-940) + 补充(927-929)
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(901, 201, '删除', NULL, 'product:category:delete', 2, NULL, 0, 1),
(902, 202, '删除', NULL, 'product:brand:delete', 2, NULL, 0, 1),
(903, 203, '删除', NULL, 'product:attrgroup:delete', 2, NULL, 0, 1),
(904, 204, '删除', NULL, 'product:baseattr:delete', 2, NULL, 0, 1),
(905, 206, '删除', NULL, 'product:spu:delete', 2, NULL, 0, 1),
(906, 208, '删除', NULL, 'product:manager:delete', 2, NULL, 0, 1),
(936, 201, '修改', NULL, 'product:category:update', 2, NULL, 0, 1),
(937, 202, '修改', NULL, 'product:brand:update', 2, NULL, 0, 1),
(938, 203, '修改', NULL, 'product:attrgroup:update', 2, NULL, 0, 1),
(939, 204, '修改', NULL, 'product:baseattr:update', 2, NULL, 0, 1),
(940, 208, '修改', NULL, 'product:manager:update', 2, NULL, 0, 1),
(927, 205, '修改', NULL, 'product:saleattr:update', 2, NULL, 0, 1),
(928, 205, '删除', NULL, 'product:saleattr:delete', 2, NULL, 1, 1),
(929, 206, '修改', NULL, 'product:spu:update', 2, NULL, 0, 1);

-- 3.4 订单管理(3)
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(30, 3, '订单列表', '/order/list', 'order:list', 1, 'wallet', 0, 1),
(31, 3, '退款管理', '/orderreturnapply', 'order:orderreturnapply:list', 1, 'refund', 1, 1);

-- 订单 按钮：补充(932-933) + 退款审核(964)
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(932, 30, '修改订单', NULL, 'order:order:update', 2, NULL, 0, 1),
(933, 30, '删除订单', NULL, 'order:order:delete', 2, NULL, 1, 1),
(964, 31, '审核通过', NULL, 'order:orderreturnapply:approve', 2, NULL, 0, 1);

-- 3.5 库存系统(4)
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(401, 4, '仓库维护', '/wareinfo', 'ware:wareinfo:list', 1, 'home', 0, 1),
(402, 4, '商品库存', '/sku', 'ware:sku:list', 1, 'box', 1, 1),
(403, 4, '库存工作单', '/task', 'ware:task:list', 1, 'task', 2, 1),
(409, 4, '采购单维护', NULL, NULL, 0, 'purchase', 3, 1);

-- 采购单维护(409) 子菜单
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(404, 409, '采购需求', '/purchaseitem', 'ware:purchaseitem:list', 1, 'files', 0, 1),
(405, 409, '采购单', '/purchase', 'ware:purchase:list', 1, 'price', 1, 1);

-- 库存 按钮：删除(907-911) + 修改(941-945)
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(907, 401, '删除', NULL, 'ware:wareinfo:delete', 2, NULL, 0, 1),
(908, 402, '删除', NULL, 'ware:sku:delete', 2, NULL, 0, 1),
(909, 403, '删除', NULL, 'ware:task:delete', 2, NULL, 0, 1),
(910, 404, '删除', NULL, 'ware:purchaseitem:delete', 2, NULL, 0, 1),
(911, 405, '删除', NULL, 'ware:purchase:delete', 2, NULL, 0, 1),
(941, 401, '修改', NULL, 'ware:wareinfo:update', 2, NULL, 0, 1),
(942, 402, '修改', NULL, 'ware:sku:update', 2, NULL, 0, 1),
(943, 403, '修改', NULL, 'ware:task:update', 2, NULL, 0, 1),
(944, 404, '修改', NULL, 'ware:purchaseitem:update', 2, NULL, 0, 1),
(945, 405, '修改', NULL, 'ware:purchase:update', 2, NULL, 0, 1);

-- 3.6 会员系统(5)
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(501, 5, '会员列表', '/member', 'member:member:list', 1, 'trend', 0, 1),
(502, 5, '会员等级', '/level', 'member:level:list', 1, 'trophy', 1, 1);

-- 会员 按钮：删除(912-913) + 修改(946-947)
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(912, 501, '删除', NULL, 'member:member:delete', 2, NULL, 0, 1),
(913, 502, '删除', NULL, 'member:level:delete', 2, NULL, 0, 1),
(946, 501, '修改', NULL, 'member:member:update', 2, NULL, 0, 1),
(947, 502, '修改', NULL, 'member:level:update', 2, NULL, 0, 1);

-- 3.7 优惠营销(6)
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(601, 6, '优惠券管理', '/coupon', 'coupon:coupon:list', 1, 'discount', 0, 1),
(602, 6, '发放记录', '/history', 'coupon:history:list', 1, 'history', 1, 1),
(603, 6, '专题活动', '/subject', 'coupon:subject:list', 1, 'flag', 2, 1),
(605, 6, '积分维护', '/bounds', 'coupon:bounds:list', 1, 'bounds', 4, 1),
(606, 6, '满减折扣', '/full', 'coupon:full:list', 1, 'tickets', 5, 1),
(607, 6, '会员价格', '/memberprice', 'coupon:memberprice:list', 1, 'money', 6, 1),
(609, 6, '首页广告', '/homeadv', 'coupon:homeadv:list', 1, 'monitor', 8, 1),
(610, 6, 'SPU专题', '/homesubjectspu', 'coupon:homesubjectspu:list', 1, 'collection-tag', 9, 1),
(612, 6, '打折优惠', '/skuladder', 'coupon:skuladder:list', 1, 'shopping-cart-full', 11, 1);

-- 优惠 按钮：删除(914-925) + 修改(948-958) + 补充(930-931)
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(914, 601, '删除', NULL, 'coupon:coupon:delete', 2, NULL, 0, 1),
(915, 602, '删除', NULL, 'coupon:history:delete', 2, NULL, 0, 1),
(916, 603, '删除', NULL, 'coupon:subject:delete', 2, NULL, 0, 1),
(917, 604, '删除', NULL, 'coupon:seckill:delete', 2, NULL, 0, 1),
(918, 605, '删除', NULL, 'coupon:bounds:delete', 2, NULL, 0, 1),
(919, 606, '删除', NULL, 'coupon:full:delete', 2, NULL, 0, 1),
(920, 607, '删除', NULL, 'coupon:memberprice:delete', 2, NULL, 0, 1),
(921, 608, '删除', NULL, 'coupon:seckillsession:delete', 2, NULL, 0, 1),
(922, 609, '删除', NULL, 'coupon:homeadv:delete', 2, NULL, 0, 1),
(923, 610, '删除', NULL, 'coupon:homesubjectspu:delete', 2, NULL, 0, 1),
(924, 611, '删除', NULL, 'coupon:seckillskunotice:delete', 2, NULL, 0, 1),
(925, 612, '删除', NULL, 'coupon:skuladder:delete', 2, NULL, 0, 1),
(948, 601, '修改', NULL, 'coupon:coupon:update', 2, NULL, 0, 1),
(949, 603, '修改', NULL, 'coupon:subject:update', 2, NULL, 0, 1),
(950, 604, '修改', NULL, 'coupon:seckill:update', 2, NULL, 0, 1),
(951, 605, '修改', NULL, 'coupon:bounds:update', 2, NULL, 0, 1),
(952, 606, '修改', NULL, 'coupon:full:update', 2, NULL, 0, 1),
(953, 607, '修改', NULL, 'coupon:memberprice:update', 2, NULL, 0, 1),
(954, 608, '修改', NULL, 'coupon:seckillsession:update', 2, NULL, 0, 1),
(955, 609, '修改', NULL, 'coupon:homeadv:update', 2, NULL, 0, 1),
(956, 610, '修改', NULL, 'coupon:homesubjectspu:update', 2, NULL, 0, 1),
(957, 611, '修改', NULL, 'coupon:seckillskunotice:update', 2, NULL, 0, 1),
(958, 612, '修改', NULL, 'coupon:skuladder:update', 2, NULL, 0, 1),
(930, 601, '新增优惠券', NULL, 'coupon:coupon:save', 2, NULL, 0, 1),
(931, 602, '修改发放记录', NULL, 'coupon:history:update', 2, NULL, 0, 1);

-- 3.8 秒杀管理(613)
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(604, 613, '秒杀活动', '/seckill', 'coupon:seckill:list', 1, 'seckill', 0, 1),
(608, 613, '每日秒杀', '/seckillsession', 'coupon:seckillsession:list', 1, 'alarm-clock', 1, 1),
(611, 613, '秒杀配置', '/seckillskunotice', 'coupon:seckillskunotice:list', 1, 'switch-button', 2, 1),
(960, 613, '关联秒杀商品', '/seckillskurelation', 'coupon:seckill:list', 1, 'Connection', 0, 1);

-- 3.9 任务调度(7)
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(701, 7, '任务调度', '/schedule', 'sys:schedule:list', 1, 'stopwatch', 0, 1);

-- 任务调度(701) 按钮：删除(926) + 修改(959)
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(926, 701, '删除', NULL, 'sys:schedule:delete', 2, NULL, 0, 1),
(959, 701, '修改', NULL, 'sys:schedule:update', 2, NULL, 0, 1);

-- 3.10 首页广告(801)
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(802, 801, '轮播图管理', '/carousel', 'content:carousel:list', 1, 'carousel', 0, 1),
(806, 801, '跑马灯管理', '/ticker', 'content:ticker:list', 1, 'ticker', 1, 1),
(810, 801, '大促管理', '/promo', 'content:promo:list', 1, 'promo', 2, 1),
(966, 801, '页脚链接', '/footerlink', 'content:footerlink:list', 1, 'Cloudy', 0, 1),
(967, 801, '快捷导航', '/homenav', 'content:homenav:list', 1, 'MostlyCloudy', 0, 1);

-- 内容 按钮：轮播(803-805) / 跑马灯(807-809) / 大促(811-813) / 页脚链接补充(934-935,962) / 快捷导航补充(963,965,968)
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`, `status`) VALUES
(803, 802, '新增轮播', NULL, 'content:carousel:save', 2, NULL, 0, 1),
(804, 802, '修改轮播', NULL, 'content:carousel:update', 2, NULL, 1, 1),
(805, 802, '删除轮播', NULL, 'content:carousel:delete', 2, NULL, 2, 1),
(807, 806, '新增公告', NULL, 'content:ticker:save', 2, NULL, 0, 1),
(808, 806, '修改公告', NULL, 'content:ticker:update', 2, NULL, 1, 1),
(809, 806, '删除公告', NULL, 'content:ticker:delete', 2, NULL, 2, 1),
(811, 810, '新增大促', NULL, 'content:promo:save', 2, NULL, 0, 1),
(812, 810, '修改大促', NULL, 'content:promo:update', 2, NULL, 1, 1),
(813, 810, '删除大促', NULL, 'content:promo:delete', 2, NULL, 2, 1),
(934, 966, '新增页脚链接', NULL, 'content:footerlink:save', 2, NULL, 0, 1),
(935, 966, '修改页脚链接', NULL, 'content:footerlink:update', 2, NULL, 1, 1),
(962, 966, '删除页脚链接', NULL, 'content:footerlink:delete', 2, NULL, 2, 1),
(963, 967, '新增快捷导航', NULL, 'content:homenav:save', 2, NULL, 0, 1),
(965, 967, '修改快捷导航', NULL, 'content:homenav:update', 2, NULL, 1, 1),
(968, 967, '删除快捷导航', NULL, 'content:homenav:delete', 2, NULL, 2, 1);

-- ============================================================
-- 4. 角色-菜单绑定
--    超级管理员(role_id=1) 绑定全部菜单（目录+菜单+按钮）
-- ============================================================
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 800), (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 801), (1, 613),
-- 系统管理(1)
(1, 10), (1, 11), (1, 12), (1, 13),
(1, 100), (1, 101), (1, 102), (1, 103),
(1, 110), (1, 111), (1, 112),
(1, 120), (1, 121), (1, 122),
-- 商品管理(2)
(1, 201), (1, 202), (1, 209), (1, 210),
(1, 203), (1, 204), (1, 205),
(1, 206), (1, 207), (1, 208), (1, 961),
(1, 901), (1, 902), (1, 903), (1, 904), (1, 905), (1, 906),
(1, 936), (1, 937), (1, 938), (1, 939), (1, 940),
(1, 927), (1, 928), (1, 929),
-- 订单管理(3)
(1, 30), (1, 31), (1, 932), (1, 933), (1, 964),
-- 库存系统(4)
(1, 401), (1, 402), (1, 403), (1, 409),
(1, 404), (1, 405),
(1, 907), (1, 908), (1, 909), (1, 910), (1, 911),
(1, 941), (1, 942), (1, 943), (1, 944), (1, 945),
-- 会员系统(5)
(1, 501), (1, 502),
(1, 912), (1, 913), (1, 946), (1, 947),
-- 优惠营销(6)
(1, 601), (1, 602), (1, 603), (1, 605), (1, 606), (1, 607), (1, 609), (1, 610), (1, 612),
(1, 914), (1, 915), (1, 916), (1, 917), (1, 918), (1, 919), (1, 920), (1, 921), (1, 922), (1, 923), (1, 924), (1, 925),
(1, 948), (1, 949), (1, 950), (1, 951), (1, 952), (1, 953), (1, 954), (1, 955), (1, 956), (1, 957), (1, 958),
(1, 930), (1, 931),
-- 秒杀管理(613)
(1, 604), (1, 608), (1, 611), (1, 960),
-- 任务调度(7)
(1, 701), (1, 926), (1, 959),
-- 首页广告(801)
(1, 802), (1, 806), (1, 810), (1, 966), (1, 967),
(1, 803), (1, 804), (1, 805),
(1, 807), (1, 808), (1, 809),
(1, 811), (1, 812), (1, 813),
(1, 934), (1, 935), (1, 962),
(1, 963), (1, 965), (1, 968);
