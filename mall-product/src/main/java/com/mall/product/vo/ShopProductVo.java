package com.mall.product.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 店铺商品 VO：SPU 基本信息 + 首个 SKU（价格/主图/销量）+ 分类名 + 评分
 * <p>供 mall-ui 店铺页渲染，数据来源于 mall-web 商家账号发布的品牌/商品。</p>
 */
@Data
public class ShopProductVo {

    /** SPU id */
    private Long spuId;

    /** 首个 SKU id（跳转详情用） */
    private Long skuId;

    /** 商品名称 */
    private String spuName;

    /** 商品主图（取该 SPU 首个 SKU 的默认图） */
    private String img;

    /** 首个 SKU 价格 */
    private BigDecimal price;

    /** 首个 SKU 销量 */
    private Long sales;

    /** 所属三级分类 id */
    private Long catalogId;

    /** 分类名称 */
    private String catalogName;

    /** 商品评分（首评星级均值，无评论为 null） */
    private Double rating;
}
