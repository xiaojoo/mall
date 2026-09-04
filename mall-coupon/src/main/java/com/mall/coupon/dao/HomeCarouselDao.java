package com.mall.coupon.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.coupon.entity.HomeCarouselEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 首页轮播内容
 */
@Mapper
public interface HomeCarouselDao extends BaseMapper<HomeCarouselEntity> {
}
