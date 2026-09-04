package com.mall.coupon.service.impl;
import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.coupon.dao.CouponHistoryDao;
import com.mall.coupon.entity.CouponHistoryEntity;
import com.mall.coupon.service.CouponHistoryService;


@Service("couponHistoryService")
public class CouponHistoryServiceImpl extends ServiceImpl<CouponHistoryDao, CouponHistoryEntity> implements CouponHistoryService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        IPage<CouponHistoryEntity> page = this.page(
                new Query<CouponHistoryEntity>().getPage(params),
                new LambdaQueryWrapper<CouponHistoryEntity>()
        .like(StringUtils.isNotBlank(key), CouponHistoryEntity::getMemberNickName, key)
        );

        return new PageUtils(page);
    }

}