package com.mall.coupon.service;

import com.mall.coupon.entity.SeckillSkuRelationEntity;

import java.util.Set;

/**
 * 秒杀 Redis 缓存处理接口（管理端上下架/移除关联/删除场次时调用，mall-seckill 同库同 db 直改）。
 * <p>实现见 {@link SeckillCacheCleanerImpl}。</p>
 */
public interface SeckillCacheCleaner {

    /**
     * 下架：DB 已置 shelf_status=0（调用方先落库）；同步 Redis——hash 标记下架 + 库存置 0
     */
    void offShelf(Long promotionSessionId, Long skuId);

    /**
     * 上架：DB 已置 shelf_status=1（调用方先落库）；同步 Redis——hash 标记上架、按 DB 最新配置刷新并重置库存
     */
    void onShelf(SeckillSkuRelationEntity relation);

    /**
     * 查询商品当前库存：hash 不存在（尚未推送上架）返回 -1；库存 &lt;=0 表示已售罄/已下架
     */
    int getStock(Long promotionSessionId, Long skuId);

    /**
     * 移除单个关联商品：不删数据，把对应库存信号量置 0（列表不再展示、无法再抢购）
     */
    void removeRelationCache(Long promotionSessionId, Long skuId);

    /**
     * 移除整个场次的缓存（场次列表 key + 其全部关联商品库存置 0）
     */
    void removeSessionCache(Long sessionId, Long startMillis, Long endMillis, Set<String> killIds);
}
