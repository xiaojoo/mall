package com.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.coupon.entity.SeckillSessionEntity;

import java.util.List;
import java.util.Map;

/**
 * 秒杀活动场次
 *
 * @author sunxiaojie
 * @date 2024-08-01 13:52:57
 */
public interface SeckillSessionService extends IService<SeckillSessionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<SeckillSessionEntity> getLates3DaySession();

    /**
     * 校验场次时间是否与已有场次重叠（创建/修改时调用）
     *
     * @param session   待保存/修改的场次
     * @param excludeId 修改时排除自身 id，创建传 null
     */
    void validateNoOverlap(SeckillSessionEntity session, Long excludeId);
}
