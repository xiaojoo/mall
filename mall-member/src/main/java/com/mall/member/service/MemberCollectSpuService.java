package com.mall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.member.entity.MemberCollectSpuEntity;
import com.mall.member.vo.CollectSpuVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 会员收藏的商品
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:14:49
 */
public interface MemberCollectSpuService extends IService<MemberCollectSpuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 查询当前会员的收藏列表（聚合商品名称/主图/价格/分类）
     */
    List<CollectSpuVo> listCollectByMember(Long memberId);

    /**
     * 添加收藏（幂等：已收藏直接返回原记录 id）
     *
     * @return 收藏记录 id
     */
    Long saveCollect(Long memberId, Long spuId, String spuName, String spuImg, String skuParams);

    /**
     * 批量删除收藏（仅当前会员的记录）
     */
    void removeCollect(Long memberId, Collection<Long> ids);

    /**
     * 按 spuId 删除收藏（仅当前会员的记录）
     */
    void removeCollectBySpu(Long memberId, Long spuId);

    /**
     * 当前会员是否已收藏该 SPU
     */
    boolean isCollected(Long memberId, Long spuId);

    /**
     * 当前会员收藏数量（Header 角标）
     */
    long countByMember(Long memberId);
}
