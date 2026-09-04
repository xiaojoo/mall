package com.mall.coupon.service.impl;
import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.coupon.dao.SeckillSkuRelationDao;
import com.mall.coupon.entity.SeckillSkuRelationEntity;
import com.mall.common.exception.RRException;
import lombok.extern.slf4j.Slf4j;
import com.mall.coupon.service.SeckillCacheCleaner;
import com.mall.coupon.service.SeckillSkuRelationService;


@Slf4j
@Service("seckillSkuRelationService")
public class SeckillSkuRelationServiceImpl extends ServiceImpl<SeckillSkuRelationDao, SeckillSkuRelationEntity> implements SeckillSkuRelationService {

    @Autowired
    private SeckillCacheCleaner seckillCacheCleaner;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        String sessionId = (String) params.get("promotionSessionId");
        IPage<SeckillSkuRelationEntity> page = this.page(
                new Query<SeckillSkuRelationEntity>().getPage(params),
                new LambdaQueryWrapper<SeckillSkuRelationEntity>()
        .eq(StringUtils.isNotBlank(sessionId) && !"0".equals(sessionId),
                SeckillSkuRelationEntity::getPromotionSessionId, sessionId)
        .and(StringUtils.isNotBlank(key), w -> w.like(SeckillSkuRelationEntity::getSkuId, key))
        );

        // 实时剩余库存（Redis）供管理端区分 上架中/已售罄/已下架；shelf_status 为 DB 持久化权威值
        List<SeckillSkuRelationEntity> records = page.getRecords();
        if (records != null) {
            records.forEach(r -> r.setStock(
                    seckillCacheCleaner.getStock(r.getPromotionSessionId(), r.getSkuId())));
        }

        return new PageUtils(page);
    }

    /**
     * 下架/上架：<b>DB 先行（权威），Redis 同步（失败记日志由上架任务兜底收敛）</b>
     *
     * @param id      关联 id
     * @param onShelf true=上架（shelf_status=1 + 按 DB 最新配置重置库存） false=下架（shelf_status=0 + 库存置 0）
     */
    @Override
    @Transactional
    public void updateShelfStatus(Long id, boolean onShelf) {
        SeckillSkuRelationEntity relation = this.getById(id);
        if (relation == null) {
            throw new RRException("秒杀商品关联不存在");
        }
        Integer current = relation.getShelfStatus() == null ? 1 : relation.getShelfStatus();
        int target = onShelf ? 1 : 0;
        if (current != target) {
            // 1、先落库（含审计时间）
            SeckillSkuRelationEntity update = new SeckillSkuRelationEntity();
            update.setId(id);
            update.setShelfStatus(target);
            update.setOnShelfTime(onShelf ? new Date() : null);
            update.setOffShelfTime(onShelf ? null : new Date());
            this.updateById(update);
            relation.setShelfStatus(target);
        }
        // 2、再同步 Redis；失败只记日志，状态以 DB 为准，由上架任务重建时按 shelf_status 收敛
        try {
            if (onShelf) {
                seckillCacheCleaner.onShelf(relation);
            } else {
                seckillCacheCleaner.offShelf(relation.getPromotionSessionId(), relation.getSkuId());
            }
        } catch (Exception e) {
            log.error("上下架同步 Redis 失败: id={}, onShelf={}, err={}", id, onShelf, e.getMessage());
        }
    }

}