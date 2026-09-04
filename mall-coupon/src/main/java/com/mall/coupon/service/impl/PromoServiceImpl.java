package com.mall.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;
import com.mall.coupon.dao.PromoDao;
import com.mall.coupon.entity.PromoEntity;
import com.mall.coupon.service.PromoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 首页大促横条
 */
@Service("promoService")
public class PromoServiceImpl extends ServiceImpl<PromoDao, PromoEntity>
        implements PromoService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        String status = (String) params.get("status");
        IPage<PromoEntity> page = this.page(
                new Query<PromoEntity>().getPage(params),
                new LambdaQueryWrapper<PromoEntity>()
                        .and(StringUtils.isNotBlank(key), w -> w
                                .like(PromoEntity::getTitle1, key)
                                .or().like(PromoEntity::getTitle2, key)
                                .or().like(PromoEntity::getDescription, key))
                        .eq(StringUtils.isNotBlank(status), PromoEntity::getStatus, status)
                        .orderByAsc(PromoEntity::getSort)
        );
        return new PageUtils(page);
    }

    @Override
    public List<PromoEntity> listEnabled() {
        return this.list(new LambdaQueryWrapper<PromoEntity>()
                .eq(PromoEntity::getStatus, 1)
                .orderByAsc(PromoEntity::getSort));
    }

    @Override
    public boolean save(PromoEntity entity) {
        Date now = new Date();
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(now);
        }
        entity.setUpdateTime(now);
        return super.save(entity);
    }

    @Override
    public boolean updateById(PromoEntity entity) {
        entity.setUpdateTime(new Date());
        return super.updateById(entity);
    }
}
