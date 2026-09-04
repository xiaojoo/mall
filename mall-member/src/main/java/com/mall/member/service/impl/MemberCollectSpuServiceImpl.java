package com.mall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;
import com.mall.common.utils.Result;
import com.mall.member.dao.MemberCollectSpuDao;
import com.mall.member.entity.MemberCollectSpuEntity;
import com.mall.member.feign.ProductFeignService;
import com.mall.member.service.MemberCollectSpuService;
import com.mall.member.vo.CollectSpuVo;
import com.mall.member.vo.SpuFavVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("memberCollectSpuService")
@RequiredArgsConstructor
public class MemberCollectSpuServiceImpl extends ServiceImpl<MemberCollectSpuDao, MemberCollectSpuEntity> implements MemberCollectSpuService {

    private final ProductFeignService productFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberCollectSpuEntity> page = this.page(
                new Query<MemberCollectSpuEntity>().getPage(params),
                new LambdaQueryWrapper<MemberCollectSpuEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CollectSpuVo> listCollectByMember(Long memberId) {
        List<MemberCollectSpuEntity> collects = this.list(new LambdaQueryWrapper<MemberCollectSpuEntity>()
                .eq(MemberCollectSpuEntity::getMemberId, memberId)
                .orderByDesc(MemberCollectSpuEntity::getCreateTime));
        if (collects.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量聚合商品展示信息（product 服务），失败时降级为收藏时快照的名称/主图
        List<Long> spuIds = collects.stream()
                .map(MemberCollectSpuEntity::getSpuId).distinct().collect(Collectors.toList());
        Map<Long, SpuFavVo> favMap = Collections.emptyMap();
        try {
            Result<Map<Long, SpuFavVo>> res = productFeignService.favInfo(spuIds);
            if (res != null && res.getCode() == 200 && res.getData() != null) {
                favMap = res.getData();
            }
        } catch (Exception e) {
            log.warn("收藏列表聚合商品信息失败，降级使用收藏快照: {}", e.getMessage());
        }

        Map<Long, SpuFavVo> finalFavMap = favMap;
        return collects.stream().map(c -> {
            CollectSpuVo vo = new CollectSpuVo();
            BeanUtils.copyProperties(c, vo);
            SpuFavVo fav = finalFavMap.get(c.getSpuId());
            if (fav != null) {
                vo.setSkuId(fav.getSkuId());
                vo.setPrice(fav.getPrice());
                vo.setCatalogId(fav.getCatalogId());
                vo.setCategoryName(fav.getCategoryName());
                if (StringUtils.isBlank(vo.getSpuName())) {
                    vo.setSpuName(fav.getSpuName());
                }
                if (StringUtils.isBlank(vo.getSpuImg())) {
                    vo.setSpuImg(fav.getSpuImg());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Long saveCollect(Long memberId, Long spuId, String spuName, String spuImg, String skuParams) {
        // 幂等：已收藏直接返回原记录
        MemberCollectSpuEntity exist = this.getOne(new LambdaQueryWrapper<MemberCollectSpuEntity>()
                .eq(MemberCollectSpuEntity::getMemberId, memberId)
                .eq(MemberCollectSpuEntity::getSpuId, spuId), false);
        if (exist != null) {
            return exist.getId();
        }

        // 商品名称/主图优先取 product 服务权威数据，失败降级用前端快照
        try {
            Result<Map<Long, SpuFavVo>> res = productFeignService.favInfo(Collections.singletonList(spuId));
            if (res != null && res.getCode() == 200 && res.getData() != null) {
                SpuFavVo fav = res.getData().get(spuId);
                if (fav != null) {
                    if (StringUtils.isNotBlank(fav.getSpuName())) {
                        spuName = fav.getSpuName();
                    }
                    if (StringUtils.isNotBlank(fav.getSpuImg())) {
                        spuImg = fav.getSpuImg();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("收藏时聚合商品信息失败，使用前端快照: {}", e.getMessage());
        }

        MemberCollectSpuEntity entity = new MemberCollectSpuEntity();
        entity.setMemberId(memberId);
        entity.setSpuId(spuId);
        entity.setSpuName(spuName);
        entity.setSpuImg(spuImg);
        entity.setSkuParams(skuParams);
        entity.setCreateTime(new Date());
        this.save(entity);
        return entity.getId();
    }

    @Override
    public void removeCollect(Long memberId, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        this.remove(new LambdaQueryWrapper<MemberCollectSpuEntity>()
                .eq(MemberCollectSpuEntity::getMemberId, memberId)
                .in(MemberCollectSpuEntity::getId, ids));
    }

    @Override
    public void removeCollectBySpu(Long memberId, Long spuId) {
        this.remove(new LambdaQueryWrapper<MemberCollectSpuEntity>()
                .eq(MemberCollectSpuEntity::getMemberId, memberId)
                .eq(MemberCollectSpuEntity::getSpuId, spuId));
    }

    @Override
    public boolean isCollected(Long memberId, Long spuId) {
        return this.count(new LambdaQueryWrapper<MemberCollectSpuEntity>()
                .eq(MemberCollectSpuEntity::getMemberId, memberId)
                .eq(MemberCollectSpuEntity::getSpuId, spuId)) > 0;
    }

    @Override
    public long countByMember(Long memberId) {
        return this.count(new LambdaQueryWrapper<MemberCollectSpuEntity>()
                .eq(MemberCollectSpuEntity::getMemberId, memberId));
    }

}
