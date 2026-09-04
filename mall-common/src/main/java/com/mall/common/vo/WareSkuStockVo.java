package com.mall.common.vo;

import lombok.Data;

/**
 * SKU 库存设置 VO（商品发布/库存管理用）
 */
@Data
public class WareSkuStockVo {

    /**
     * sku_id
     */
    private Long skuId;

    /**
     * 仓库id
     */
    private Long wareId;

    /**
     * 库存数
     */
    private Integer stock;
}
