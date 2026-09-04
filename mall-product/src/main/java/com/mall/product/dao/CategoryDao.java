package com.mall.product.dao;

import com.mall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author sunxiaojie
 * @date 2024-08-01 11:32:29
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
