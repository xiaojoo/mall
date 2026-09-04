package com.mall.ware.dao;

import com.mall.ware.entity.PurchaseEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 采购信息
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:50:20
 */
@Mapper
public interface PurchaseDao extends BaseMapper<PurchaseEntity> {

    void updateBatchById(@Param("PurchaseEntity") List<PurchaseEntity> collect);
}
