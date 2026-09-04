package com.mall.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;
import com.mall.coupon.dao.HomeCarouselDao;
import com.mall.coupon.entity.HomeCarouselEntity;
import com.mall.coupon.service.HomeCarouselService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 首页轮播内容
 */
@Service("homeCarouselService")
public class HomeCarouselServiceImpl extends ServiceImpl<HomeCarouselDao, HomeCarouselEntity>
        implements HomeCarouselService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        String status = (String) params.get("status");
        IPage<HomeCarouselEntity> page = this.page(
                new Query<HomeCarouselEntity>().getPage(params),
                new LambdaQueryWrapper<HomeCarouselEntity>()
                        .like(StringUtils.isNotBlank(key), HomeCarouselEntity::getName, key)
                        .eq(StringUtils.isNotBlank(status), HomeCarouselEntity::getStatus, status)
                        .orderByAsc(HomeCarouselEntity::getSort)
        );
        return new PageUtils(page);
    }

    @Override
    public List<HomeCarouselEntity> listEnabled() {
        return this.list(new LambdaQueryWrapper<HomeCarouselEntity>()
                .eq(HomeCarouselEntity::getStatus, 1)
                .orderByAsc(HomeCarouselEntity::getSort));
    }

    @Override
    public boolean save(HomeCarouselEntity entity) {
        Date now = new Date();
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(now);
        }
        entity.setUpdateTime(now);
        return super.save(entity);
    }

    @Override
    public boolean updateById(HomeCarouselEntity entity) {
        entity.setUpdateTime(new Date());
        return super.updateById(entity);
    }
}
