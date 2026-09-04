package com.mall.product.service;

import com.mall.product.vo.ShopSummaryVo;
import com.mall.product.vo.ShopVo;

import java.util.List;

/**
 * 店铺渲染服务（C 端）
 * <p>店铺 = mall-web 商家账号发布的品牌（showStatus=1），
 * 商品 = 该品牌已上架 SPU（publishStatus=1）。</p>
 */
public interface ShopService {

    /**
     * 店铺列表（展示中品牌，按 sort 升序），用于店铺切换/默认店铺
     */
    List<ShopSummaryVo> listShops();

    /**
     * 店铺详情：品牌信息 + 在售商品（首个 SKU 价格/主图/销量 + 评分）
     *
     * @param brandId 品牌 id
     * @return 店铺不存在或未展示返回 null
     */
    ShopVo getShopDetail(Long brandId);
}
