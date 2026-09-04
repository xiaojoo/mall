package com.mall.cart.dto;

import lombok.Data;

/**
 * 购物车请求DTO
 */
@Data
public class CartRequestDto {

    private String skuId;

    private String num;

    private String checked;

    /**
     * 规格（如 "颜色:黑色;版本:256GB"，详情页加购传入，缺省由后端默认）
     */
    private String skuAttrValues;
}
