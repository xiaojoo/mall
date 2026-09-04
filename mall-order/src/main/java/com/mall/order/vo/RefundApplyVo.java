package com.mall.order.vo;

import lombok.Data;

/**
 * 申请退款请求（C 端）
 */
@Data
public class RefundApplyVo {

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 退款原因
     */
    private String reason;

    /**
     * 问题描述
     */
    private String description;
}
