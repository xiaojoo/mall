package com.mall.coupon.service.impl;
import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.coupon.dao.HomeSubjectSpuDao;
import com.mall.coupon.entity.HomeSubjectSpuEntity;
import com.mall.coupon.service.HomeSubjectSpuService;


@Service("homeSubjectSpuService")
public class HomeSubjectSpuServiceImpl extends ServiceImpl<HomeSubjectSpuDao, HomeSubjectSpuEntity> implements HomeSubjectSpuService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        IPage<HomeSubjectSpuEntity> page = this.page(
                new Query<HomeSubjectSpuEntity>().getPage(params),
                new LambdaQueryWrapper<HomeSubjectSpuEntity>()
        .like(StringUtils.isNotBlank(key), HomeSubjectSpuEntity::getName, key)
        );

        return new PageUtils(page);
    }

}