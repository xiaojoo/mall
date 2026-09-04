package com.mall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.to.mq.OrderTo;
import com.mall.common.to.mq.StockLockedTo;
import com.mall.common.utils.PageUtils;
import com.mall.common.vo.WareSkuStockVo;
import com.mall.ware.entity.WareSkuEntity;
import com.mall.ware.vo.LockStockResultVo;
import com.mall.ware.vo.SkuHasStockVo;
import com.mall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:50:20
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    /**
     * 设置 SKU 库存（SET 语义：覆盖库存数/新增记录）
     */
    void saveStock(List<WareSkuStockVo> vos);

    List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds);

    Boolean orderLockStock(WareSkuLockVo vo);

    void unlockStock(StockLockedTo to);

    void unlockStock(OrderTo orderTo);
}
