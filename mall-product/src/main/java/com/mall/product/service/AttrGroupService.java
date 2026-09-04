package com.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.product.entity.AttrGroupEntity;
import com.mall.product.vo.AttrGroupWithAttrsVo;
import com.mall.product.vo.SpuItemAttrGroupVo;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author sunxiaojie
 * @date 2024-08-01 11:32:29
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPage(Map<String, Object> params, Long catelogId);

    List<AttrGroupWithAttrsVo> getAttrGroupWithAttrByCatelogId(Long catelogId);

    List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId);

    /**
     * 批量添加属性分组（单条失败不影响其他条，返回每条结果）
     */
    List<Map<String, Object>> batchSave(List<AttrGroupEntity> groups);
}

