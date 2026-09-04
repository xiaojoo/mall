package com.mall.product.vo;

import com.mall.product.entity.SkuImagesEntity;
import com.mall.product.entity.SkuInfoEntity;
import com.mall.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@ToString
@Data
public class SkuItemVo {

    // 1、sku基本信息的获取：pms_sku_info
    private SkuInfoEntity info;

    private boolean hasStock = true;

    // 2、sku的图片信息：pms_sku_images
    private List<SkuImagesEntity> images;

    // 3、获取spu的销售属性组合
    private List<SkuItemSaleAttrVo> saleAttr;

    // 4、获取spu的介绍
    private SpuInfoDescEntity desc;

    // 5、获取spu的规格参数信息
    private List<SpuItemAttrGroupVo> groupAttrs;

    // 6、秒杀商品的优惠信息
    private SeckillSkuVo seckillSkuVo;

    /**
     * 品牌名（店铺名，详情页展示/跳转用；品牌缺失为空）
     */
    private String brandName;

    /**
     * 品牌 logo（店铺图标，详情页展示用；品牌缺失为空）
     */
    private String brandLogo;

}
