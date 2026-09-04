package com.mall.common.to;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券使用校验请求（order → coupon 服务间调用）
 */
@Data
public class CouponUseCheckTo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 会员 id
     */
    private Long memberId;

    /**
     * 优惠券 id
     */
    private Long couponId;

    /**
     * 订单商品总额（不含运费，用于门槛判断）
     */
    private BigDecimal amount;

    /**
     * 订单商品 skuId 列表（适用范围匹配）
     */
    private List<Long> skuIds;

    /**
     * 订单号（核销时回写 sms_coupon_history.order_sn）
     */
    private String orderSn;
}
