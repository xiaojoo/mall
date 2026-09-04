package com.mall.coupon.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.coupon.entity.TickerEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 首页跑马灯公告
 */
@Mapper
public interface TickerDao extends BaseMapper<TickerEntity> {
}
