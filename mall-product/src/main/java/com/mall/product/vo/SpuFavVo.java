package com.mall.product.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 收藏列表聚合 VO：SPU 基本信息 + 首个 SKU（价格/跳转）+ 分类名
 * <p>供 mall-member 收藏接口远程聚合，会员端收藏页渲染使用。</p>
 */
@Data
public class SpuFavVo {

    /** spu_id */
    private Long spuId;

    /** 商品名称 */
    private String spuName;

    /** 商品主图（取该 SPU 首个 SKU 的默认图） */
    private String spuImg;

    /** 首个 SKU id（收藏页跳转详情用） */
    private Long skuId;

    /** 首个 SKU 价格 */
    private BigDecimal price;

    /** 所属三级分类 id */
    private Long catalogId;

    /** 分类名称 */
    private String categoryName;
}
