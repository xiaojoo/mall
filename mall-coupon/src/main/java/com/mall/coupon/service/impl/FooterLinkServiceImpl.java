package com.mall.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;
import com.mall.coupon.dao.FooterLinkDao;
import com.mall.coupon.entity.FooterLinkEntity;
import com.mall.coupon.service.FooterLinkService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 页脚链接
 */
@Service("footerLinkService")
public class FooterLinkServiceImpl extends ServiceImpl<FooterLinkDao, FooterLinkEntity>
        implements FooterLinkService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        String status = (String) params.get("status");
        IPage<FooterLinkEntity> page = this.page(
                new Query<FooterLinkEntity>().getPage(params),
                new LambdaQueryWrapper<FooterLinkEntity>()
                        .and(StringUtils.isNotBlank(key), w -> w
                                .like(FooterLinkEntity::getGroupName, key)
                                .or().like(FooterLinkEntity::getName, key))
                        .eq(StringUtils.isNotBlank(status), FooterLinkEntity::getStatus, status)
                        .orderByAsc(FooterLinkEntity::getGroupSort)
                        .orderByAsc(FooterLinkEntity::getSort)
        );
        return new PageUtils(page);
    }

    @Override
    public List<FooterLinkEntity> listEnabled() {
        return this.list(new LambdaQueryWrapper<FooterLinkEntity>()
                .eq(FooterLinkEntity::getStatus, 1)
                .orderByAsc(FooterLinkEntity::getGroupSort)
                .orderByAsc(FooterLinkEntity::getSort));
    }

    @Override
    public boolean save(FooterLinkEntity entity) {
        Date now = new Date();
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(now);
        }
        entity.setUpdateTime(now);
        return super.save(entity);
    }

    @Override
    public boolean updateById(FooterLinkEntity entity) {
        entity.setUpdateTime(new Date());
        return super.updateById(entity);
    }
}
