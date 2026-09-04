package com.mall.seckill.service;

import com.mall.seckill.to.SeckillSkuRedisTo;

import java.util.List;
import java.util.Map;

public interface SeckillService {
    void uploadSeckillSkuLatest3Days();

    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    /**
     * 分场次秒杀商品：live=正在秒杀 / upcoming=预约秒杀 / history=历史秒杀（含售罄、下架商品）
     */
    Map<String, List<SeckillSkuRedisTo>> getSeckillSessions();

    SeckillSkuRedisTo getSkuSeckilInfo(Long skuId);

    String kill(String killId, String key, Integer num);
}
