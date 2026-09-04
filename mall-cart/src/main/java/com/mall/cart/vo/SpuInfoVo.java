package com.mall.cart.vo;

import lombok.Data;

/**
 * SPU 信息（购物车远程调用 mall-product 获取品牌名用）
 */
@Data
public class SpuInfoVo {

    private Long id;

    private String spuName;

    private Long catalogId;

    private Long brandId;

    private String brandName;
}
