package com.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.coupon.entity.SeckillSkuRelationEntity;

import java.util.Map;

/**
 * 秒杀活动商品关联
 *
 * @author sunxiaojie
 * @date 2024-08-01 13:52:57
 */
public interface SeckillSkuRelationService extends IService<SeckillSkuRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 下架/上架秒杀商品（只操作 Redis，不执行 MySQL 变更）
     *
     * @param id      关联 id
     * @param onShelf true=上架（按 DB 最新配置重置库存） false=下架（库存置 0）
     */
    void updateShelfStatus(Long id, boolean onShelf);
}

