package com.mall.coupon.service.impl;
import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.coupon.dao.SeckillSkuNoticeDao;
import com.mall.coupon.entity.SeckillSkuNoticeEntity;
import com.mall.coupon.service.SeckillSkuNoticeService;


@Service("seckillSkuNoticeService")
public class SeckillSkuNoticeServiceImpl extends ServiceImpl<SeckillSkuNoticeDao, SeckillSkuNoticeEntity> implements SeckillSkuNoticeService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        IPage<SeckillSkuNoticeEntity> page = this.page(
                new Query<SeckillSkuNoticeEntity>().getPage(params),
                new LambdaQueryWrapper<SeckillSkuNoticeEntity>()
        .like(StringUtils.isNotBlank(key), SeckillSkuNoticeEntity::getSkuId, key)
        );

        return new PageUtils(page);
    }

}