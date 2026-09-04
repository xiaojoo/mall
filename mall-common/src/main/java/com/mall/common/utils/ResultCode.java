package com.mall.common.utils;

import lombok.Getter;

/**
 * 统一返回码枚举
 * 错误码规范：
 * - 200: 成功
 * - 400: 参数错误
 * - 401: 未授权
 * - 403: 拒绝访问
 * - 404: 资源不存在
 * - 500: 服务器内部错误
 * - 1xxx: 会员模块
 * - 2xxx: 商品模块
 * - 3xxx: 订单模块
 * - 4xxx: 优惠券模块
 * - 5xxx: 仓库模块
 */
@Getter
public enum ResultCode {
    // 通用状态码
    SUCCESS(200, "成功"),
    FAIL(500, "操作失败"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "拒绝访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 会员模块 1xxx
    MEMBER_NOT_FOUND(1001, "会员不存在"),
    MEMBER_LEVEL_NOT_FOUND(1002, "会员等级不存在"),
    MEMBER_ADDRESS_NOT_FOUND(1003, "收货地址不存在"),
    MEMBER_LOGIN_LOG_NOT_FOUND(1004, "登录日志不存在"),

    // 商品模块 2xxx
    PRODUCT_NOT_FOUND(2001, "商品不存在"),
    SPU_NOT_FOUND(2002, "SPU不存在"),
    SKU_NOT_FOUND(2003, "SKU不存在"),
    CATEGORY_NOT_FOUND(2004, "分类不存在"),
    BRAND_NOT_FOUND(2005, "品牌不存在"),
    ATTR_NOT_FOUND(2006, "属性不存在"),
    ATTR_GROUP_NOT_FOUND(2007, "属性编组不存在"),

    // 订单模块 3xxx
    ORDER_NOT_FOUND(3001, "订单不存在"),
    ORDER_ITEM_NOT_FOUND(3002, "订单项不存在"),
    ORDER_RETURN_NOT_FOUND(3003, "退货单不存在"),
    PAYMENT_NOT_FOUND(3004, "支付信息不存在"),
    REFUND_NOT_FOUND(3005, "退款信息不存在"),

    // 优惠券模块 4xxx
    COUPON_NOT_FOUND(4001, "优惠券不存在"),
    COUPON_HISTORY_NOT_FOUND(4002, "优惠券领取记录不存在"),
    COUPON_SPU_RELATION_NOT_FOUND(4003, "优惠券与 SPU 关联不存在"),
    COUPON_CATEGORY_RELATION_NOT_FOUND(4004, "优惠券与分类关联不存在"),
    MEMBER_PRICE_NOT_FOUND(4005, "会员价格不存在"),
    SECKILL_PROMOTION_NOT_FOUND(4006, "秒杀活动不存在"),
    SECKILL_SESSION_NOT_FOUND(4007, "秒杀场次不存在"),
    SECKILL_SKU_NOT_FOUND(4008, "秒杀商品不存在"),
    SECKILL_SKU_NOTICE_NOT_FOUND(4009, "秒杀提醒不存在"),
    SKU_FULL_REDUCTION_NOT_FOUND(4010, "满减优惠不存在"),
    SKU_LADDER_NOT_FOUND(4011, "阶梯价格不存在"),

    // 仓库模块 5xxx
    WARE_NOT_FOUND(5001, "仓库信息不存在"),
    WARE_SKU_NOT_FOUND(5002, "仓库 SKU 不存在"),
    PURCHASE_NOT_FOUND(5003, "采购单不存在"),
    PURCHASE_DETAIL_NOT_FOUND(5004, "采购单详情不存在"),
    WARE_ORDER_TASK_NOT_FOUND(5005, "入库单不存在"),
    WARE_ORDER_TASK_DETAIL_NOT_FOUND(5006, "入库单详情不存在"),

    // 通用业务错误
    DATA_NOT_FOUND(1000, "数据不存在"),
    DATA_ALREADY_EXISTS(1001, "数据已存在"),
    PARAM_ERROR(1002, "参数错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
