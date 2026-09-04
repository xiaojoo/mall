package com.mall.coupon.dao;

import com.mall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author sunxiaojie
 * @date 2024-08-01 13:52:57
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
