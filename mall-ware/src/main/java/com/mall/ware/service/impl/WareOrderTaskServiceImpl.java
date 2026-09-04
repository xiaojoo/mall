package com.mall.ware.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.ware.dao.WareOrderTaskDao;
import com.mall.ware.entity.WareOrderTaskEntity;
import com.mall.ware.service.WareOrderTaskService;


@Service("wareOrderTaskService")
public class WareOrderTaskServiceImpl extends ServiceImpl<WareOrderTaskDao, WareOrderTaskEntity> implements WareOrderTaskService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        IPage<WareOrderTaskEntity> page = this.page(
                new Query<WareOrderTaskEntity>().getPage(params),
                // 搜索 key：模糊匹配订单号或收货人
                new LambdaQueryWrapper<WareOrderTaskEntity>()
                        .and(StringUtils.isNotBlank(key), w -> w
                                .like(WareOrderTaskEntity::getOrderSn, key)
                                .or().like(WareOrderTaskEntity::getConsignee, key))
                        .orderByDesc(WareOrderTaskEntity::getCreateTime)
        );

        return new PageUtils(page);
    }

    @Override
    public WareOrderTaskEntity getOrderTaskByOrderSn(String orderSn) {
        WareOrderTaskEntity orderTaskEntity = this.baseMapper.selectOne(
                new LambdaQueryWrapper<WareOrderTaskEntity>().eq(WareOrderTaskEntity::getOrderSn, orderSn));

        return orderTaskEntity;
    }

}