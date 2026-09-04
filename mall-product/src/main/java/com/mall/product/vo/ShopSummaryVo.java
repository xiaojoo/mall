package com.mall.product.vo;

import lombok.Data;

/**
 * 店铺列表项 VO：mall-ui /shop 店铺切换/默认店铺使用
 */
@Data
public class ShopSummaryVo {

    /** 品牌 id（店铺 id） */
    private Long brandId;

    /** 店铺名称 */
    private String shopName;

    /** 店铺 logo */
    private String logo;

    /** 在售商品数 */
    private Long productCount;
}
