package com.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.product.entity.BrandEntity;

import java.util.List;
import java.util.Map;

/**
 * 品牌
 *
 * @author sunxiaojie
 * @date 2024-08-01 11:32:29
 */
public interface BrandService extends IService<BrandEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void updateDetail(BrandEntity brand);

    List<BrandEntity> getBrandsByIds(List<Long> brandIds);

    /**
     * 批量添加品牌（单条失败不影响其他条，返回每条结果）
     */
    List<Map<String, Object>> batchSave(List<BrandEntity> brands);
}

