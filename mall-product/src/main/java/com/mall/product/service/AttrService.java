package com.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.product.entity.AttrEntity;
import com.mall.product.vo.AttrGroupRelationVo;
import com.mall.product.vo.AttrRespVo;
import com.mall.product.vo.AttrVo;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author sunxiaojie
 * @date 2024-08-01 11:32:29
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 保存基本属性/销售属性，返回生成的 attrId
     */
    Long saveAttr(AttrVo attr);

    /**
     * 批量添加（单条失败不影响其他条，返回每条结果）
     */
    List<Map<String, Object>> batchSave(List<AttrVo> attrs);

    PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String attrType);

    AttrRespVo getAttrInfo(Long attrId);

    void updateAttr(AttrVo attr);

    List<AttrEntity> getReLationAttr(Long attrgroupId);

    void deleteRelation(AttrGroupRelationVo[] vos);

    PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId);

    List<Long> selectSearchAttrIds(List<Long> attrIds);
}

