package com.mall.weapp.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 微信小程序创建订单请求实体
 * <p>接收创建订单所需的参数</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@Data
public class WeappOrderCreateEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 购物车ID列表，逗号分隔
     */
    private String cartIds;

    /**
     * 收货地址ID
     */
    private Long addressId;
}
