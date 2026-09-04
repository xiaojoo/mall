-- ============================================================
-- mall_sms 数据库全量脚本（合并版：表结构 mall_sms + 首页轮播/跑马灯/大促种子数据）
-- 执行数据库：mall_sms（mall-coupon 所在库）
-- ============================================================

drop table if exists sms_coupon;

drop table if exists sms_coupon_history;

drop table if exists sms_coupon_spu_category_relation;

drop table if exists sms_coupon_spu_relation;

drop table if exists sms_home_adv;

drop table if exists sms_home_subject;

drop table if exists sms_home_subject_spu;

drop table if exists sms_member_price;

drop table if exists sms_seckill_promotion;

drop table if exists sms_seckill_session;

drop table if exists sms_seckill_sku_notice;

drop table if exists sms_seckill_sku_relation;

drop table if exists sms_sku_full_reduction;

drop table if exists sms_sku_ladder;

drop table if exists sms_spu_bounds;

/*==============================================================*/
/* Table: sms_coupon                                            */
/*==============================================================*/
create table sms_coupon
(
    id                   bigint not null auto_increment comment 'id',
    coupon_type          tinyint(1) comment '优惠卷类型[0->全场赠券；1->会员赠券；2->购物赠券；3->注册赠券]',
    coupon_img           varchar(2000) comment '优惠券图片',
    coupon_name          varchar(100) comment '优惠卷名字',
    num                  int comment '数量',
    amount               decimal(18,4) comment '金额',
    per_limit            int comment '每人限领张数',
    min_point            decimal(18,4) comment '使用门槛',
    start_time           datetime comment '开始时间',
    end_time             datetime comment '结束时间',
    use_type             tinyint(1) comment '使用类型[0->全场通用；1->指定分类；2->指定商品]',
    note                 varchar(200) comment '备注',
    publish_count        int(11) comment '发行数量',
    use_count            int(11) comment '已使用数量',
    receive_count        int(11) comment '领取数量',
    enable_start_time    datetime comment '可以领取的开始日期',
    enable_end_time      datetime comment '可以领取的结束日期',
    code                 varchar(64) comment '优惠码',
    member_level         tinyint(1) comment '可以领取的会员等级[0->不限等级，其他-对应等级]',
    publish              tinyint(1) comment '发布状态[0-未发布，1-已发布]',
    brand_id             bigint comment '店铺/品牌id（null=全场券；非空=该店铺专享券）',
    primary key (id)
);

alter table sms_coupon comment '优惠券信息';

/*==============================================================*/
/* Table: sms_coupon_history                                    */
/*==============================================================*/
create table sms_coupon_history
(
    id                   bigint not null auto_increment comment 'id',
    coupon_id            bigint comment '优惠券id',
    member_id            bigint comment '会员id',
    member_nick_name     varchar(64) comment '会员名字',
    get_type             tinyint(1) comment '获取方式[0->后台赠送；1->主动领取]',
    create_time          datetime comment '创建时间',
    use_type             tinyint(1) comment '使用状态[0->未使用；1->已使用；2->已过期]',
    use_time             datetime comment '使用时间',
    order_id             bigint comment '订单id',
    order_sn             bigint comment '订单号',
    primary key (id)
);

alter table sms_coupon_history comment '优惠券领取历史记录';

/*==============================================================*/
/* Table: sms_coupon_spu_category_relation                      */
/*==============================================================*/
create table sms_coupon_spu_category_relation
(
    id                   bigint not null auto_increment comment 'id',
    coupon_id            bigint comment '优惠券id',
    category_id          bigint comment '产品分类id',
    category_name        varchar(64) comment '产品分类名称',
    primary key (id)
);

alter table sms_coupon_spu_category_relation comment '优惠券分类关联';

/*==============================================================*/
/* Table: sms_coupon_spu_relation                               */
/*==============================================================*/
create table sms_coupon_spu_relation
(
    id                   bigint not null auto_increment comment 'id',
    coupon_id            bigint comment '优惠券id',
    spu_id               bigint comment 'spu_id',
    spu_name             varchar(255) comment 'spu_name',
    primary key (id)
);

alter table sms_coupon_spu_relation comment '优惠券与产品关联';

/*==============================================================*/
/* Table: sms_home_adv                                          */
/*==============================================================*/
create table sms_home_adv
(
    id                   bigint not null auto_increment comment 'id',
    name                 varchar(100) comment '名字',
    pic                  varchar(500) comment '图片地址',
    start_time           datetime comment '开始时间',
    end_time             datetime comment '结束时间',
    status               tinyint(1) comment '状态',
    click_count          int comment '点击数',
    url                  varchar(500) comment '广告详情连接地址',
    note                 varchar(500) comment '备注',
    sort                 int comment '排序',
    publisher_id         bigint comment '发布者',
    auth_id              bigint comment '审核者',
    primary key (id)
);

alter table sms_home_adv comment '首页轮播广告';

/*==============================================================*/
/* Table: sms_home_subject                                      */
/*==============================================================*/
create table sms_home_subject
(
    id                   bigint not null auto_increment comment 'id',
    name                 varchar(200) comment '专题名字',
    title                varchar(255) comment '专题标题',
    sub_title            varchar(255) comment '专题副标题',
    status               tinyint(1) comment '显示状态',
    url                  varchar(500) comment '详情连接',
    sort                 int comment '排序',
    img                  varchar(500) comment '专题图片地址',
    primary key (id)
);

alter table sms_home_subject comment '首页专题表【jd首页下面很多专题，每个专题链接新的页面，展示专题商品信息】';

/*==============================================================*/
/* Table: sms_home_subject_spu                                  */
/*==============================================================*/
create table sms_home_subject_spu
(
    id                   bigint not null auto_increment comment 'id',
    name                 varchar(200) comment '专题名字',
    subject_id           bigint comment '专题id',
    spu_id               bigint comment 'spu_id',
    sort                 int comment '排序',
    primary key (id)
);

alter table sms_home_subject_spu comment '专题商品';

/*==============================================================*/
/* Table: sms_member_price                                      */
/*==============================================================*/
create table sms_member_price
(
    id                   bigint not null auto_increment comment 'id',
    sku_id               bigint comment 'sku_id',
    member_level_id      bigint comment '会员等级id',
    member_level_name    varchar(100) comment '会员等级名',
    member_price         decimal(18,4) comment '会员对应价格',
    add_other            tinyint(1) comment '可否叠加其他优惠[0-不可叠加优惠，1-可叠加]',
    primary key (id)
);

alter table sms_member_price comment '商品会员价格';

/*==============================================================*/
/* Table: sms_seckill_promotion                                 */
/*==============================================================*/
create table sms_seckill_promotion
(
    id                   bigint not null auto_increment comment 'id',
    title                varchar(255) comment '活动标题',
    start_time           datetime comment '开始日期',
    end_time             datetime comment '结束日期',
    status               tinyint comment '上下线状态',
    create_time          datetime comment '创建时间',
    user_id              bigint comment '创建人',
    primary key (id)
);

alter table sms_seckill_promotion comment '秒杀活动';

/*==============================================================*/
/* Table: sms_seckill_session                                   */
/*==============================================================*/
create table sms_seckill_session
(
    id                   bigint not null auto_increment comment 'id',
    name                 varchar(200) comment '场次名称',
    start_time           datetime comment '每日开始时间',
    end_time             datetime comment '每日结束时间',
    status               tinyint(1) comment '启用状态',
    create_time          datetime comment '创建时间',
    primary key (id)
);

alter table sms_seckill_session comment '秒杀活动场次';

/*==============================================================*/
/* Table: sms_seckill_sku_notice                                */
/*==============================================================*/
create table sms_seckill_sku_notice
(
    id                   bigint not null auto_increment comment 'id',
    member_id            bigint comment 'member_id',
    sku_id               bigint comment 'sku_id',
    session_id           bigint comment '活动场次id',
    subcribe_time        datetime comment '订阅时间',
    send_time            datetime comment '发送时间',
    notice_type          tinyint(1) comment '通知方式[0-短信，1-邮件]',
    primary key (id)
);

alter table sms_seckill_sku_notice comment '秒杀商品通知订阅';

/*==============================================================*/
/* Table: sms_seckill_sku_relation                              */
/*==============================================================*/
create table sms_seckill_sku_relation
(
    id                   bigint not null auto_increment comment 'id',
    promotion_id         bigint comment '活动id',
    promotion_session_id bigint comment '活动场次id',
    sku_id               bigint comment '商品id',
    seckill_price        decimal comment '秒杀价格',
    seckill_count        decimal comment '秒杀总量',
    seckill_limit        decimal comment '每人限购数量',
    seckill_sort         int comment '排序',
    shelf_status         tinyint default 1 comment '上架状态：1=上架 0=下架（持久化，Redis 重建后以此为准）',
    off_shelf_time       datetime comment '下架时间（审计）',
    on_shelf_time        datetime comment '上架时间（审计）',
    primary key (id)
);

-- 存量库升级（可重复执行）：上下架状态持久化
-- alter table sms_seckill_sku_relation
--     add column shelf_status   tinyint default 1 comment '上架状态：1=上架 0=下架' after seckill_sort,
--     add column off_shelf_time datetime comment '下架时间（审计）' after shelf_status,
--     add column on_shelf_time  datetime comment '上架时间（审计）' after off_shelf_time;

alter table sms_seckill_sku_relation comment '秒杀活动商品关联';

/*==============================================================*/
/* Table: sms_sku_full_reduction                                */
/*==============================================================*/
create table sms_sku_full_reduction
(
    id                   bigint not null auto_increment comment 'id',
    sku_id               bigint comment 'spu_id',
    full_price           decimal(18,4) comment '满多少',
    reduce_price         decimal(18,4) comment '减多少',
    add_other            tinyint(1) comment '是否参与其他优惠',
    primary key (id)
);

alter table sms_sku_full_reduction comment '商品满减信息';

/*==============================================================*/
/* Table: sms_sku_ladder                                        */
/*==============================================================*/
create table sms_sku_ladder
(
    id                   bigint not null auto_increment comment 'id',
    sku_id               bigint comment 'spu_id',
    full_count           int comment '满几件',
    discount             decimal(4,2) comment '打几折',
    price                decimal(18,4) comment '折后价',
    add_other            tinyint(1) comment '是否叠加其他优惠[0-不可叠加，1-可叠加]',
    primary key (id)
);

alter table sms_sku_ladder comment '商品阶梯价格';

/*==============================================================*/
/* Table: sms_spu_bounds                                        */
/*==============================================================*/
create table sms_spu_bounds
(
    id                   bigint not null auto_increment comment 'id',
    spu_id               bigint,
    grow_bounds          decimal(18,4) comment '成长积分',
    buy_bounds           decimal(18,4) comment '购物积分',
    work                 tinyint(1) comment '优惠生效情况[1111（四个状态位，从右到左）;0 - 无优惠，成长积分是否赠送;1 - 无优惠，购物积分是否赠送;2 - 有优惠，成长积分是否赠送;3 - 有优惠，购物积分是否赠送【状态位0：不赠送，1：赠送】]',
    primary key (id)
);

alter table sms_spu_bounds comment '商品spu积分设置';

-- ============================================================
-- 以下为首页业务种子数据
-- ============================================================
-- ============================================================
-- 首页轮播内容表（mall-ui 首页 HERO 轮播）
-- 执行数据库：mall_sms（mall-coupon 所在库）
--
-- 轮播每屏渲染所需信息分析（来自 mall-ui src/views/home/index.vue 现有 3 屏）：
--   kicker        顶部小字（如 "◢ NEURAL-SHOPPING ONLINE · AI 智选已上线 ◣"）
--   title         主标题第一段（如 "未来已至 · "）
--   highlight1    主标题高亮词1（glow-c 青色）
--   title2        主标题第二段（<br/> 之后）
--   highlight2    主标题高亮词2（glow-m 品红）
--   sub           副标题/描述（可含 <b> 加粗标签）
--   buttons[]     按钮组 { text 文案, type primary|ghost, link 跳转路由 }
--   stats[]       数据条 { num 数字, unit 单位, label 说明 }
--   chips[]       右侧全息面板小字 { pos a|b|c|d 位置, text 文案 }
--   price         右下角价格卡 { label 标签, value 价格, decimals 小数 }
-- ============================================================

DROP TABLE IF EXISTS `sms_home_carousel`;

CREATE TABLE `sms_home_carousel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(100) DEFAULT NULL COMMENT '轮播标识（如：AI智选）',
  `theme` varchar(20) DEFAULT NULL COMMENT '主题（s1/s2/s3，对应前端主题色）',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-停用 1-启用',
  `sort` int DEFAULT 0 COMMENT '排序',
  `content` json DEFAULT NULL COMMENT '轮播内容 JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页轮播内容';

-- 种子数据：mall-ui 现有 3 屏轮播（按原硬编码内容转换）
INSERT INTO `sms_home_carousel` (`name`, `theme`, `status`, `sort`, `content`) VALUES
('AI智选', 's1', 1, 1, '{
  "kicker": "◢ NEURAL-SHOPPING ONLINE · AI 智选已上线 ◣",
  "title": "未来已至 · ",
  "highlight1": "银河级",
  "title2": "智能购物体验",
  "highlight2": "全息启动",
  "sub": "接入<b>量子推荐引擎</b>，3 秒生成专属购物矩阵。全站商品支持<b>全息预览</b>、<b>脑波支付</b>、<b>轨道快递 48h 达</b>。",
  "buttons": [
    { "text": "立即探索 ⟶", "type": "primary", "link": "/list" },
    { "text": "▶ 观看全息演示", "type": "ghost", "link": "" }
  ],
  "stats": [
    { "num": "2.4", "unit": "亿+", "label": "注册星际用户" },
    { "num": "98", "unit": "%", "label": "AI 推荐精准度" },
    { "num": "0.3", "unit": "s", "label": "量子极速加载" }
  ],
  "chips": [
    { "pos": "a", "text": "◈ QUANTUM CORE 量子核心" },
    { "pos": "b", "text": "✓ 已接入星域网 · 信号满格" },
    { "pos": "c", "text": "✦ 全息预览中 · 放大 300%" },
    { "pos": "d", "text": "⟳ AI 同步价格中…" }
  ],
  "price": { "label": "限时首发", "value": "9,999", "decimals": ".00" }
}'),
('量子秒杀', 's2', 1, 2, '{
  "kicker": "⚡ QUANTUM SECKILL · 限时抢购",
  "title": "限时秒杀 · ",
  "highlight1": "低至 1 折",
  "title2": "量子抢购",
  "highlight2": "手慢无",
  "sub": "全场秒杀商品最低<b>1 折起</b>，量子通道极速下单。限时限量，<b>抢完即止</b>，错过再等一光年。",
  "buttons": [
    { "text": "进入时空卖场 ⟶", "type": "primary", "link": "/seckill" },
    { "text": "查看全部商品", "type": "ghost", "link": "/list" }
  ],
  "stats": [
    { "num": "1", "unit": "折起", "label": "全场秒杀价" },
    { "num": "24", "unit": "h", "label": "不间断场次" },
    { "num": "限量", "unit": "⚡", "label": "抢完即止" }
  ],
  "chips": [
    { "pos": "a", "text": "⚡ SECKILL NOW 秒杀进行中" },
    { "pos": "b", "text": "🔥 今日已抢 12,880 件" },
    { "pos": "c", "text": "✦ 限购 2 件 · 手慢无" },
    { "pos": "d", "text": "⟳ 倒计时 04:32:11" }
  ],
  "price": { "label": "秒杀价", "value": "299", "decimals": ".00" }
}'),
('星环会员', 's3', 1, 3, '{
  "kicker": "✦ GALAXY MEMBERSHIP · 星环尊享",
  "title": "星环会员 · ",
  "highlight1": "尊享权益",
  "title2": "全场 95 折",
  "highlight2": "专属通道",
  "sub": "开通星环会员，享<b>全场 95 折</b>、专属量子客服、<b>脑波极速支付</b>、每月 8 号<b>会员日</b>双倍积分。",
  "buttons": [
    { "text": "开通会员 ⟶", "type": "primary", "link": "/profile" },
    { "text": "了解权益", "type": "ghost", "link": "/list" }
  ],
  "stats": [
    { "num": "95", "unit": "折", "label": "全场通用" },
    { "num": "8", "unit": "号", "label": "每月会员日" },
    { "num": "7×24", "unit": "h", "label": "量子专属客服" }
  ],
  "chips": [
    { "pos": "a", "text": "✦ MEMBER ONLY 会员专享" },
    { "pos": "b", "text": "✓ 全年 95 折 · 已生效" },
    { "pos": "c", "text": "◈ 成长值加速 1.5 倍" },
    { "pos": "d", "text": "⟳ 会员日双倍积分中…" }
  ],
  "price": { "label": "会员年费", "value": "888", "decimals": ".00" }
}');

-- ============================================================
-- 首页跑马灯公告表（mall-ui 首页 Ticker）
-- 执行数据库：mall_sms（mall-coupon 所在库）
-- ============================================================

DROP TABLE IF EXISTS `sms_home_ticker`;

CREATE TABLE `sms_home_ticker` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `content` varchar(255) DEFAULT NULL COMMENT '公告文本',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-停用 1-启用',
  `sort` int DEFAULT 0 COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页跑马灯公告';

-- 种子数据：mall-ui 原 TICKER_ITEMS 6 条
INSERT INTO `sms_home_ticker` (`content`, `status`, `sort`) VALUES
('⚡ 618 星际狂欢节 · 全场低至 1 折', 1, 1),
('🚀 量子级配送 · 城市 2 小时达', 1, 2),
('🧠 脑波支付开通 · 下单免触控', 1, 3),
('🎁 新用户注册即送 ¥888 星元礼包', 1, 4),
('🌌 全息设备区 · 满 5000 减 800', 1, 5),
('🛰 火星基地分仓已启用 · 跨星直邮', 1, 6);

-- ============================================================
-- 首页大促横条表（mall-ui AppPromo，多页面共用）
-- 执行数据库：mall_sms（mall-coupon 所在库）
-- ============================================================

DROP TABLE IF EXISTS `sms_home_promo`;

CREATE TABLE `sms_home_promo` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `title1` varchar(100) DEFAULT NULL COMMENT '促销主标题1（如：618 星际狂欢节）',
  `title2` varchar(100) DEFAULT NULL COMMENT '促销主标题2（如：全场低至 1 折）',
  `description` varchar(500) DEFAULT NULL COMMENT '促销描述',
  `code` varchar(50) DEFAULT NULL COMMENT '优惠码（如：NEBULA-618）',
  `btn_text` varchar(50) DEFAULT NULL COMMENT '按钮文案',
  `btn_link` varchar(200) DEFAULT NULL COMMENT '按钮跳转链接',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-停用 1-启用',
  `sort` int DEFAULT 0 COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页大促横条';

-- 种子数据：mall-ui 原 AppPromo 内容
INSERT INTO `sms_home_promo` (`title1`, `title2`, `description`, `code`, `btn_text`, `btn_link`, `status`, `sort`) VALUES
('618 星际狂欢节', '全场低至 1 折', '新用户注册立得 ¥888 星元 · 会员加赠量子加速券 · 满 5000 减 800 上不封顶', 'NEBULA-618', '立即抢购 ⟶', '/list', 1, 1);

-- 首页快捷导航（mall-ui AppNav cat-row：全息设备 … 新品首发）
DROP TABLE IF EXISTS `sms_home_nav`;

CREATE TABLE `sms_home_nav` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(50) DEFAULT NULL COMMENT '名称（如：全息设备 / 新品首发）',
  `icon` varchar(50) DEFAULT NULL COMMENT '图标（字符图标，如：⌬）',
  `link` varchar(500) DEFAULT NULL COMMENT '跳转链接（空=按名称跳列表筛选；站内路径 /xxx 或 https:// 外链）',
  `hot` tinyint DEFAULT 0 COMMENT '是否HOT标[0-否；1-是]（兼容旧数据）',
  `tag` varchar(20) DEFAULT NULL COMMENT '标签文字（如 HOT/新品/爆款；空=不显示）',
  `tag_color` varchar(100) DEFAULT NULL COMMENT '标签颜色（CSS 颜色值或渐变，如 linear-gradient(135deg, #ff2e63, #ff7eb3)；空=默认渐变）',
  `sort` int DEFAULT 0 COMMENT '排序',
  `show_status` tinyint DEFAULT 1 COMMENT '显示状态[0-停用；1-启用]',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页快捷导航';

-- 种子数据：mall-ui 原 AppNav quickCats 静态内容（link 留空=按名称跳列表筛选）
INSERT INTO `sms_home_nav` (`name`, `icon`, `link`, `hot`, `tag`, `tag_color`, `sort`, `show_status`) VALUES
('全息设备', '⌬', NULL, 1, 'HOT', 'linear-gradient(135deg, #ff2e63, #ff7eb3)', 1, 1),
('量子计算', '◈', NULL, 0, NULL, NULL, 2, 1),
('机甲外骨骼', '⬡', NULL, 0, NULL, NULL, 3, 1),
('神经接口', '✦', NULL, 0, NULL, NULL, 4, 1),
('智能义体', '◉', NULL, 0, NULL, NULL, 5, 1),
('太空装备', '✧', NULL, 0, NULL, NULL, 6, 1),
('能量补给', '⚡', NULL, 0, NULL, NULL, 7, 1),
('新品首发', '▸', NULL, 0, '新品', 'linear-gradient(135deg, #3d7bff, #00d4ff)', 8, 1);

-- 页脚链接（mall-ui AppFooter 页脚三列，group_sort 列排序 / sort 组内排序）
DROP TABLE IF EXISTS `sms_footer_link`;

CREATE TABLE `sms_footer_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `group_name` varchar(50) DEFAULT NULL COMMENT '列标题（如：购物指南 / 配送服务 / 关于我们）',
  `group_sort` int DEFAULT 0 COMMENT '列排序',
  `name` varchar(50) DEFAULT NULL COMMENT '链接名称',
  `url` varchar(200) DEFAULT NULL COMMENT '跳转链接（/list 等站内路径或 https:// 外部链接）',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-停用 1-启用',
  `sort` int DEFAULT 0 COMMENT '组内排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='页脚链接';

-- 种子数据：mall-ui 原 AppFooter 静态内容
INSERT INTO `sms_footer_link` (`group_name`, `group_sort`, `name`, `url`, `status`, `sort`) VALUES
('购物指南', 1, '购物流程', '/list', 1, 1),
('购物指南', 1, '会员体系', '/profile', 1, 2),
('购物指南', 1, '星元充值', '/profile', 1, 3),
('购物指南', 1, '脑波支付', '/cart', 1, 4),
('购物指南', 1, '联系客服', '/profile', 1, 5),
('配送服务', 2, '轨道快递', '/list', 1, 1),
('配送服务', 2, '星际速运', '/list', 1, 2),
('配送服务', 2, '火星分仓', '/list', 1, 3),
('配送服务', 2, '月球自提', '/list', 1, 4),
('配送服务', 2, '运费查询', '/list', 1, 5),
('关于我们', 3, '企业介绍', '/', 1, 1),
('关于我们', 3, '加入星环', '/', 1, 2),
('关于我们', 3, '新闻中心', '/', 1, 3),
('关于我们', 3, '隐私协议', '/', 1, 4),
('关于我们', 3, '资质认证', '/', 1, 5);
