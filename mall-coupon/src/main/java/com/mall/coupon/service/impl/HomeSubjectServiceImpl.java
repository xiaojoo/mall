package com.mall.coupon.service.impl;
import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.coupon.dao.HomeSubjectDao;
import com.mall.coupon.entity.HomeSubjectEntity;
import com.mall.coupon.service.HomeSubjectService;


@Service("homeSubjectService")
public class HomeSubjectServiceImpl extends ServiceImpl<HomeSubjectDao, HomeSubjectEntity> implements HomeSubjectService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        IPage<HomeSubjectEntity> page = this.page(
                new Query<HomeSubjectEntity>().getPage(params),
                new LambdaQueryWrapper<HomeSubjectEntity>()
        .like(StringUtils.isNotBlank(key), HomeSubjectEntity::getTitle, key)
        );

        return new PageUtils(page);
    }

}