package com.mall.coupon.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.coupon.entity.HomeNavEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 首页快捷导航
 */
@Mapper
public interface HomeNavDao extends BaseMapper<HomeNavEntity> {
}
