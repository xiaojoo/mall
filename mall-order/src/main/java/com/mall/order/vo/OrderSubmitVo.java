package com.mall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSubmitVo {

    /**
     * 收获地址的id
     **/
    private Long addrId;

    /**
     * 支付方式
     **/
    private Integer payType;
    // 无需提交要购买的商品，去购物车再获取一遍
    // 优惠、发票
    // 用户相关的信息，直接去session中取出即可

    /**
     * 防重令牌
     **/
    private String orderToken;

    /**
     * 应付价格
     **/
    private BigDecimal payPrice;

    /**
     * 优惠券 id（未使用优惠券时为 null）
     **/
    private Long couponId;

    /**
     * 订单备注
     **/
    private String remarks;

    /**
     * 立即购买直购模式的订单项（由前端传入，不读购物车）；为空则走购物车勾选项结算
     */
    private List<OrderItemVo> items;
}
