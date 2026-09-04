package com.mall.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;
import com.mall.coupon.dao.TickerDao;
import com.mall.coupon.entity.TickerEntity;
import com.mall.coupon.service.TickerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页跑马灯公告
 */
@Service("tickerService")
public class TickerServiceImpl extends ServiceImpl<TickerDao, TickerEntity>
        implements TickerService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        String status = (String) params.get("status");
        IPage<TickerEntity> page = this.page(
                new Query<TickerEntity>().getPage(params),
                new LambdaQueryWrapper<TickerEntity>()
                        .like(StringUtils.isNotBlank(key), TickerEntity::getContent, key)
                        .eq(StringUtils.isNotBlank(status), TickerEntity::getStatus, status)
                        .orderByAsc(TickerEntity::getSort)
        );
        return new PageUtils(page);
    }

    @Override
    public List<String> listEnabledTexts() {
        List<TickerEntity> list = this.list(new LambdaQueryWrapper<TickerEntity>()
                .eq(TickerEntity::getStatus, 1)
                .orderByAsc(TickerEntity::getSort));
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(TickerEntity::getContent)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    @Override
    public boolean save(TickerEntity entity) {
        Date now = new Date();
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(now);
        }
        entity.setUpdateTime(now);
        return super.save(entity);
    }

    @Override
    public boolean updateById(TickerEntity entity) {
        entity.setUpdateTime(new Date());
        return super.updateById(entity);
    }
}
