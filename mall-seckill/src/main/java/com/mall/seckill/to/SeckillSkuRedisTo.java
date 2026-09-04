package com.mall.seckill.to;

import com.mall.seckill.vo.SkuInfoVo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillSkuRedisTo {
    /**
     * 活动id
     */
    private Long promotionId;
    /**
     * 活动场次id
     */
    private Long promotionSessionId;
    /**
     * 商品id
     */
    private Long skuId;
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    /**
     * 秒杀总量
     */
    private Integer seckillCount;
    /**
     * 每人限购数量
     */
    private Integer seckillLimit;
    /**
     * 排序
     */
    private Integer seckillSort;
    /**
     * sku的详细信息
     */
    private SkuInfoVo skuInfo;
    /**
     * 当前商品秒杀的开始时间
     */
    private Long startTime;
    /**
     * 当前商品秒杀的结束时间
     */
    private Long endTime;
    /**
     * 当前商品秒杀的随机码
     */
    private String randomCode;

    /**
     * 剩余库存（列表接口实时填充；&lt;=0 表示已售罄/已下架，前端展示「已抢完」并禁用秒杀按钮）
     */
    private Integer stock;

    /**
     * 上架状态（1=上架 0=下架，随上架任务写入缓存；前端据此区分「已下架」与「已抢完」）
     */
    private Integer shelfStatus;
}
