package com.mall.coupon.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mall.coupon.entity.SeckillSkuRelationEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 秒杀 Redis 缓存即时处理（mall-seckill 同库同 db 直改）。
 *
 * <p>管理端移除秒杀商品关联/场次时同步调用，避免等上架任务（分钟级）才生效。
 * <b>移除商品不删数据，只把库存信号量置 0</b>——记录保留，但列表不再展示、无法再抢购。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillCacheCleanerImpl implements SeckillCacheCleaner {

    private static final String SESSION_CACHE_PREFIX = "seckill:sessions:";
    private static final String SKU_HASH_KEY = "seckill:skus";
    private static final String STOCK_PREFIX = "seckill:stock:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 下架：DB 已置 shelf_status=0（调用方先落库）；这里同步 Redis——hash 标记下架 + 库存置 0
     */
    public void offShelf(Long promotionSessionId, Long skuId) {
        if (promotionSessionId == null || skuId == null) {
            return;
        }
        String killId = promotionSessionId + "-" + skuId;
        String value = (String) redisTemplate.opsForHash().get(SKU_HASH_KEY, killId);
        if (StringUtils.isEmpty(value)) {
            log.warn("秒杀商品下架: killId={} 不在秒杀缓存（尚未上架到 Redis），跳过", killId);
            return;
        }
        try {
            JSONObject obj = JSON.parseObject(value);
            obj.put("shelfStatus", 0);
            redisTemplate.opsForHash().put(SKU_HASH_KEY, killId, obj.toJSONString());
            String randomCode = obj.getString("randomCode");
            if (StringUtils.isNotEmpty(randomCode)) {
                redisTemplate.opsForValue().set(STOCK_PREFIX + randomCode, "0");
            }
            log.info("秒杀商品下架: killId={} hash 标记下架、库存置 0（MySQL 已落库）", killId);
        } catch (Exception e) {
            log.error("秒杀商品下架异常: killId={}, err={}", killId, e.getMessage());
        }
    }

    /**
     * 上架：DB 已置 shelf_status=1（调用方先落库）；这里同步 Redis——hash 标记上架、按 DB 最新配置刷新并重置库存
     */
    public void onShelf(SeckillSkuRelationEntity relation) {
        Long promotionSessionId = relation.getPromotionSessionId();
        Long skuId = relation.getSkuId();
        if (promotionSessionId == null || skuId == null) {
            return;
        }
        String killId = promotionSessionId + "-" + skuId;
        String value = (String) redisTemplate.opsForHash().get(SKU_HASH_KEY, killId);
        if (StringUtils.isEmpty(value)) {
            log.warn("秒杀商品上架: killId={} 不在秒杀缓存（等待上架任务推送后生效）", killId);
            return;
        }
        try {
            JSONObject obj = JSON.parseObject(value);
            String randomCode = obj.getString("randomCode");
            if (StringUtils.isEmpty(randomCode)) {
                log.warn("秒杀商品上架: killId={} 无 randomCode，跳过", killId);
                return;
            }
            // 用 DB 最新配置刷新 hash（保留 randomCode/时间/skuInfo），标记上架
            obj.put("shelfStatus", 1);
            obj.put("seckillPrice", relation.getSeckillPrice());
            obj.put("seckillCount", relation.getSeckillCount());
            obj.put("seckillLimit", relation.getSeckillLimit());
            obj.put("seckillSort", relation.getSeckillSort());
            redisTemplate.opsForHash().put(SKU_HASH_KEY, killId, obj.toJSONString());
            // 库存重置为当前秒杀总量
            redisTemplate.opsForValue().set(STOCK_PREFIX + randomCode, String.valueOf(relation.getSeckillCount()));
            log.info("秒杀商品上架: killId={} 库存重置为 {}", killId, relation.getSeckillCount());
        } catch (Exception e) {
            log.error("秒杀商品上架异常: killId={}, err={}", killId, e.getMessage());
        }
    }

    /**
     * 查询商品当前库存（用于管理端列表展示上/下架状态）：不在缓存或库存 &lt;=0 视为已下架
     */
    public int getStock(Long promotionSessionId, Long skuId) {
        if (promotionSessionId == null || skuId == null) {
            return -1;
        }
        String killId = promotionSessionId + "-" + skuId;
        String value = (String) redisTemplate.opsForHash().get(SKU_HASH_KEY, killId);
        if (StringUtils.isEmpty(value)) {
            // hash 不存在：商品尚未推送到秒杀缓存（等待分钟级上架任务），返回 -1 供前端展示「待上架」
            return -1;
        }
        try {
            String randomCode = JSON.parseObject(value).getString("randomCode");
            if (StringUtils.isEmpty(randomCode)) {
                return -1;
            }
            String stock = redisTemplate.opsForValue().get(STOCK_PREFIX + randomCode);
            return stock == null ? 0 : Integer.parseInt(stock);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 移除单个关联商品的缓存：信号量 + hash 字段 + 场次列表中的 killId
     */
    public void removeRelationCache(Long promotionSessionId, Long skuId) {
        if (skuId == null) {
            return;
        }
        Set<String> killIds = new HashSet<>();
        if (promotionSessionId != null) {
            killIds.add(promotionSessionId + "-" + skuId);
        } else {
            // 历史脏数据 promotionSessionId 为空：按 skuId 后缀匹配 hash 中所有 killId 兜底
            Set<Object> allKeys = redisTemplate.opsForHash().keys(SKU_HASH_KEY);
            if (allKeys != null) {
                String suffix = "-" + skuId;
                for (Object k : allKeys) {
                    if (String.valueOf(k).endsWith(suffix)) {
                        killIds.add(String.valueOf(k));
                    }
                }
            }
        }
        for (String killId : killIds) {
            setStockZero(killId);
        }
    }

    /**
     * 单个 killId 库存置 0：从 hash 取 randomCode，SET 信号量=0（hash 与场次列表数据保留）
     */
    private void setStockZero(String killId) {
        String value = (String) redisTemplate.opsForHash().get(SKU_HASH_KEY, killId);
        if (StringUtils.isEmpty(value)) {
            log.warn("秒杀缓存清理: killId={} 不在秒杀 hash，跳过库存置0", killId);
            return;
        }
        try {
            JSONObject obj = JSON.parseObject(value);
            String randomCode = obj.getString("randomCode");
            if (StringUtils.isNotEmpty(randomCode)) {
                redisTemplate.opsForValue().set(STOCK_PREFIX + randomCode, "0");
                log.info("秒杀缓存清理: killId={} 库存置 0（商品已移除关联），数据保留", killId);
            }
        } catch (Exception e) {
            log.error("秒杀缓存清理: 解析 {} 异常: {}", killId, e.getMessage());
        }
    }

    /**
     * 移除整个场次的缓存（场次列表 key + 其全部关联商品）
     */
    public void removeSessionCache(Long sessionId, Long startMillis, Long endMillis, Set<String> killIds) {
        if (startMillis != null && endMillis != null) {
            String key = SESSION_CACHE_PREFIX + startMillis + "_" + endMillis;
            Boolean deleted = redisTemplate.delete(key);
            log.info("秒杀缓存清理: 场次 {} 已删除={}, sessionId={}", key, deleted, sessionId);
        }
        if (killIds != null) {
            for (String killId : killIds) {
                String[] parts = killId.split("-");
                if (parts.length == 2) {
                    try {
                        removeRelationCache(Long.valueOf(parts[0]), Long.valueOf(parts[1]));
                    } catch (NumberFormatException ignore) {
                        // 忽略异常格式
                    }
                }
            }
        }
    }
}
