package com.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import com.mall.product.entity.SpuInfoDescEntity;
import com.mall.product.entity.SpuInfoEntity;
import com.mall.product.vo.SpuFavVo;
import com.mall.product.vo.SpuSaveVo;

import java.util.List;
import java.util.Map;

/**
 * spu信息
 *
 * @author sunxiaojie
 * @date 2024-08-01 11:32:29
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 保存 SPU 及全部关联数据；任一远程保存失败抛出异常，整个事务回滚
     */
    Long saveSpuInfo(SpuSaveVo vo);

    /**
     * 批量发布商品：逐个保存（图片直接传 URL）并自动上架，单条失败不影响其他条
     *
     * @return 每条结果：[{spuName, success, spuId?, error?}]
     */
    List<Map<String, Object>> batchPublish(List<SpuSaveVo> vos);

    void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity);

    PageUtils queryPageByCondition(Map<String, Object> params);

    Result<Object> up(Long spuId);

    /**
     * 批量查询收藏展示信息：SPU 基本信息 + 首个 SKU（价格/跳转）+ 分类名
     *
     * @param spuIds SPU id 集合（去空后查询）
     * @return spuId -> 收藏展示信息
     */
    Map<Long, SpuFavVo> favInfo(List<Long> spuIds);

    /**
     * 批量按 skuId 查询 spu 信息（品牌补全用，替代逐个 feign 调用）
     *
     * @param skuIds SKU id 列表
     * @return skuId -> spu 信息（含品牌名；无关联 spu 的 sku 不在结果中）
     */
    Map<Long, SpuInfoEntity> getSpuInfoMapBySkuIds(List<Long> skuIds);

    /**
     * 下架：删除 ES 中该 SPU 所有 SKU 的商品文档，并更新发布状态
     */
    Result<Object> down(Long spuId);

    SpuInfoEntity getSpuInfoBySkuId(Long skuId);
}

