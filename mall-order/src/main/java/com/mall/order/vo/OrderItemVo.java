package com.mall.order.vo;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVo {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long skuId;

    private Boolean check;

    private String title;

    private String image;

    /**
     * 商品套餐属性
     */
    private List<String> skuAttrValues;

    private BigDecimal price;

    private Integer count;

    private BigDecimal totalPrice;
    /**
     * 商品重量
     */
    private BigDecimal weight = new BigDecimal("0.085");
    /**
     * 商家/品牌（结算页按商家分组展示用）
     */
    private String spuBrand;

    /**
     * 商家/品牌 id（店铺跳转用）
     */
    private Long brandId;

    /**
     * 商家/品牌 logo（店铺图标）
     */
    private String brandLogo;
}
