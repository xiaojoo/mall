package com.mall.coupon.service.impl;
import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.coupon.dao.HomeAdvDao;
import com.mall.coupon.entity.HomeAdvEntity;
import com.mall.coupon.service.HomeAdvService;


@Service("homeAdvService")
public class HomeAdvServiceImpl extends ServiceImpl<HomeAdvDao, HomeAdvEntity> implements HomeAdvService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        IPage<HomeAdvEntity> page = this.page(
                new Query<HomeAdvEntity>().getPage(params),
                new LambdaQueryWrapper<HomeAdvEntity>()
        .like(StringUtils.isNotBlank(key), HomeAdvEntity::getName, key)
        );

        return new PageUtils(page);
    }

}