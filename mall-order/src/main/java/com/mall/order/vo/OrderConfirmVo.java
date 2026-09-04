package com.mall.order.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OrderConfirmVo {
    @Getter
    @Setter
    /**
     * 会员收获地址列表
     */
    private List<MemberAddressVo> memberAddressVos;

    @Getter
    @Setter
    /**
     * 所有选中的购物项
     */
    private List<OrderItemVo> items;

    /**
     * 发票记录
     */
    @Getter
    @Setter
    /**
     * 优惠券（会员积分）
     */
    private Integer integration;

    /**
     * 防止重复提交的令牌
     */
    @Getter
    @Setter
    private String orderToken;

    @Getter
    @Setter
    /**
     * 运费（默认地址估算，前端切换地址时重算）
     */
    private BigDecimal freightAmount;

    @Getter
    @Setter
    private Map<Long, Boolean> stocks;

    public Integer getCount() {
        Integer count = 0;
        if (items != null && !items.isEmpty()) {
            for (OrderItemVo item : items) {
                count += item.getCount();
            }
        }
        return count;
    }

    /**
     * 订单总额
     */
    //BigDecimal total;
    //计算订单总额
    public BigDecimal getTotal() {
        BigDecimal totalNum = BigDecimal.ZERO;
        if (items != null && !items.isEmpty()) {
            for (OrderItemVo item : items) {
                //计算当前商品的总价格
                BigDecimal itemPrice = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
                //再计算全部商品的总价格
                totalNum = totalNum.add(itemPrice);
            }
        }
        return totalNum;
    }

    /**
     * 应付价格（商品总额 + 运费，与下单验价口径一致）
     */
    public BigDecimal getPayPrice() {
        BigDecimal freight = freightAmount == null ? BigDecimal.ZERO : freightAmount;
        return getTotal().add(freight);
    }
}
