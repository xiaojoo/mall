package com.mall.order.vo;

import lombok.Data;

import java.util.List;

@Data
public class WareSkuLockVo {

    private String orderSn;

    /** 收货人 */
    private String consignee;

    /** 收货人电话 */
    private String consigneeTel;

    /** 配送地址（省市区+详细地址） */
    private String deliveryAddress;

    /** 订单备注 */
    private String orderComment;

    /** 付款方式 */
    private Integer paymentWay;

    /**
     * 需要锁住的所有库存信息
     **/
    private List<OrderItemVo> locks;
}
