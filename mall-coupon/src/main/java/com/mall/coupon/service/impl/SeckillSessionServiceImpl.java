package com.mall.coupon.service.impl;


import com.mall.coupon.entity.SeckillSkuRelationEntity;
import com.mall.coupon.service.SeckillCacheCleaner;
import com.mall.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.coupon.dao.SeckillSessionDao;
import com.mall.coupon.entity.SeckillSessionEntity;
import com.mall.common.exception.RRException;
import com.mall.coupon.service.SeckillSessionService;
import org.springframework.util.ObjectUtils;
import lombok.RequiredArgsConstructor;

@Service("seckillSessionService")
@RequiredArgsConstructor
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    private final SeckillSkuRelationService seckillSkuRelationService;
    private final SeckillCacheCleaner seckillCacheCleaner;

    /**
     * 场次时间重叠校验：新场次 [start,end] 与任一已有场次 [start,end] 相交即拒绝
     */
    @Override
    public void validateNoOverlap(SeckillSessionEntity session, Long excludeId) {
        if (session.getStartTime() == null || session.getEndTime() == null) {
            throw new RRException("秒杀场次开始/结束时间不能为空");
        }
        if (!session.getEndTime().after(session.getStartTime())) {
            throw new RRException("秒杀场次结束时间必须晚于开始时间");
        }
        Long overlap = this.count(new LambdaQueryWrapper<SeckillSessionEntity>()
                .lt(SeckillSessionEntity::getStartTime, session.getEndTime())
                .gt(SeckillSessionEntity::getEndTime, session.getStartTime())
                .ne(excludeId != null, SeckillSessionEntity::getId, excludeId));
        if (overlap != null && overlap > 0) {
            throw new RRException("秒杀场次时间重叠，请重新输入");
        }
    }

    @Override
    public boolean removeByIds(Collection<?> idList) {
        @SuppressWarnings("unchecked")
        Collection<? extends Serializable> ids = (Collection<? extends Serializable>) idList;
        List<SeckillSessionEntity> sessions = this.listByIds(ids);
        boolean ok = super.removeByIds(idList);
        if (ok) {
            sessions.forEach(session -> {
                // 该场次全部关联商品的 killId
                List<SeckillSkuRelationEntity> relations = seckillSkuRelationService.list(
                        new LambdaQueryWrapper<SeckillSkuRelationEntity>()
                                .eq(SeckillSkuRelationEntity::getPromotionSessionId, session.getId()));
                Set<String> killIds = relations.stream()
                        .map(r -> r.getPromotionSessionId() + "-" + r.getSkuId())
                        .collect(Collectors.toSet());
                // 删除该场次的关联商品（DB），避免残留脏数据
                seckillSkuRelationService.remove(new LambdaQueryWrapper<SeckillSkuRelationEntity>()
                        .eq(SeckillSkuRelationEntity::getPromotionSessionId, session.getId()));
                // 立即清理 Redis：场次列表 + 关联商品缓存/信号量
                seckillCacheCleaner.removeSessionCache(session.getId(),
                        session.getStartTime() == null ? null : session.getStartTime().getTime(),
                        session.getEndTime() == null ? null : session.getEndTime().getTime(),
                        killIds);
            });
        }
        return ok;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<SeckillSessionEntity> queryWrapper = new LambdaQueryWrapper<>();
        String key = (String) params.get("key");
        if (!ObjectUtils.isEmpty(key)) {
            queryWrapper.eq(SeckillSessionEntity::getId, key);
        }
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                queryWrapper
        );
        return new PageUtils(page);
    }

    @Override
    public List<SeckillSessionEntity> getLates3DaySession() {
        // 计算最近三天
        // 查出这三天参与秒杀活动的商品
        List<SeckillSessionEntity> list = this.baseMapper.selectList(new LambdaQueryWrapper<SeckillSessionEntity>()
                .between(SeckillSessionEntity::getStartTime, startTime(), endTime()));

        if (list != null && !list.isEmpty()) {
            // 查出sms_seckill_sku_relation表中关联的skuId
            return list.stream().map(session -> {
                Long id = session.getId();
                // 查出sms_seckill_sku_relation表中关联的skuId
                List<SeckillSkuRelationEntity> relationSkus = seckillSkuRelationService
                        .list(new LambdaQueryWrapper<SeckillSkuRelationEntity>()
                                .eq(SeckillSkuRelationEntity::getPromotionSessionId, id));
                session.setRelationSkus(relationSkus);
                return session;
            }).collect(Collectors.toList());
        }

        return null;
    }

    private String startTime() {
        LocalDate now = LocalDate.now();
        LocalTime min = LocalTime.MIN;
        LocalDateTime start = LocalDateTime.of(now, min);

        // 格式化时间
        String startFormat = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return startFormat;
    }

    /**
     * 结束时间
     */
    private String endTime() {
        LocalDate now = LocalDate.now();
        LocalDate plus = now.plusDays(2);
        LocalTime max = LocalTime.MAX;
        LocalDateTime end = LocalDateTime.of(plus, max);

        // 格式化时间
        String endFormat = end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return endFormat;
    }

}