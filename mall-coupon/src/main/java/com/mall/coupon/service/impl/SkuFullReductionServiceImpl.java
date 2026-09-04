package com.mall.coupon.service.impl;
import org.apache.commons.lang3.StringUtils;

import com.mall.common.to.MemberPrice;
import com.mall.common.to.SkuReductionTo;
import com.mall.coupon.entity.MemberPriceEntity;
import com.mall.coupon.entity.SkuLadderEntity;
import com.mall.coupon.service.MemberPriceService;
import com.mall.coupon.service.SkuLadderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.coupon.dao.SkuFullReductionDao;
import com.mall.coupon.entity.SkuFullReductionEntity;
import com.mall.coupon.service.SkuFullReductionService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;


@Service("skuFullReductionService")
@RequiredArgsConstructor
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {

    final SkuLadderService skuLadderService;


    final MemberPriceService memberPriceService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new LambdaQueryWrapper<SkuFullReductionEntity>()
        .like(StringUtils.isNotBlank(key), SkuFullReductionEntity::getSkuId, key)
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveSkuReduction(SkuReductionTo skuReductionTo) {
        // sku的优惠、满减等信息；mall_sms->sms_sku_ladder
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        skuLadderEntity.setSkuId(skuReductionTo.getSkuId());
        skuLadderEntity.setFullCount(skuReductionTo.getFullCount());
        skuLadderEntity.setDiscount(skuReductionTo.getDiscount());
        skuLadderEntity.setAddOther(skuReductionTo.getCountStatus());
        if (skuReductionTo.getFullCount() > 0) {
            skuLadderService.save(skuLadderEntity);
        }
        // sms_sku_full_reduction
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(skuReductionTo, skuFullReductionEntity);
        if (skuFullReductionEntity.getFullPrice().compareTo(new BigDecimal("0")) > 0) {
            this.save(skuFullReductionEntity);
        }
        // sms_member_price
        List<MemberPrice> memberPrice = skuReductionTo.getMemberPrice();
        // 判空防御：memberPrice 可能为 null（前端未填写会员价时），避免 stream() NPE
        if (memberPrice != null && !memberPrice.isEmpty()) {
            List<MemberPriceEntity> collect = memberPrice.stream().map(item -> {
                MemberPriceEntity memberPriceEntity = new MemberPriceEntity();
                memberPriceEntity.setSkuId(skuReductionTo.getSkuId());
                memberPriceEntity.setMemberLevelId(item.getId());
                memberPriceEntity.setMemberLevelName(item.getName());
                memberPriceEntity.setMemberPrice(item.getPrice());
                memberPriceEntity.setAddOther(1);
                return memberPriceEntity;
            }).filter(item -> item.getMemberPrice() != null && item.getMemberPrice().compareTo(new BigDecimal("0")) > 0).collect(Collectors.toList());
            memberPriceService.saveBatch(collect);
        }
    }
}