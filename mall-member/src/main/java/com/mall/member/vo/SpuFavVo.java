package com.mall.member.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 收藏列表聚合 VO（member 侧副本，用于 Feign 反序列化 product 服务返回）
 */
@Data
public class SpuFavVo {

    /** spu_id */
    private Long spuId;

    /** 商品名称 */
    private String spuName;

    /** 商品主图 */
    private String spuImg;

    /** 首个 SKU id */
    private Long skuId;

    /** 首个 SKU 价格 */
    private BigDecimal price;

    /** 所属分类 id */
    private Long catalogId;

    /** 分类名称 */
    private String categoryName;
}
