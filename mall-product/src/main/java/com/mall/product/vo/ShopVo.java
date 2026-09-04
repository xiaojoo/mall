package com.mall.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 店铺详情 VO：mall-ui /shop 页面渲染数据
 * <p>店铺 = mall-web 商家账号发布的品牌（pms_brand，showStatus=1），
 * 商品 = 该品牌已上架 SPU（publishStatus=1）。</p>
 */
@Data
public class ShopVo {

    /** 品牌 id（店铺 id） */
    private Long brandId;

    /** 店铺名称 */
    private String shopName;

    /** 店铺 logo */
    private String logo;

    /** 店铺简介 */
    private String descript;

    /** 在售商品数 */
    private Long productCount;

    /** 覆盖分类数 */
    private Long categoryCount;

    /** 累计销量（在售 SKU 销量合计） */
    private Long totalSales;

    /** 店铺评分（首评星级均值，无评论为 null） */
    private Double rating;

    /** 在售商品列表（按 SKU id 升序取首个 SKU） */
    private List<ShopProductVo> products;
}
