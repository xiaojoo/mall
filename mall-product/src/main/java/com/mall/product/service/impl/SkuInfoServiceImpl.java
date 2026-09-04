package com.mall.product.service.impl;

import com.alibaba.fastjson2.TypeReference;
import com.mall.common.utils.Result;
import com.mall.product.entity.SkuImagesEntity;
import com.mall.product.entity.SpuInfoDescEntity;
import com.mall.product.entity.BrandEntity;
import com.mall.product.feign.SeckillFeignService;
import com.mall.product.service.*;
import com.mall.product.vo.SeckillSkuVo;
import com.mall.product.vo.SkuItemSaleAttrVo;
import com.mall.product.vo.SkuItemVo;
import com.mall.product.vo.SpuItemAttrGroupVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.product.dao.SkuInfoDao;
import com.mall.product.entity.SkuInfoEntity;
import lombok.RequiredArgsConstructor;


@Service("skuInfoService")
@RequiredArgsConstructor
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    private final SkuImagesService skuImagesService;


    private final SpuInfoDescService spuInfoDescService;


    private final AttrGroupService attrGroupService;


    private final SkuSaleAttrValueService skuSaleAttrValueService;


    private final SeckillFeignService seckillFeignService;


    private final BrandService brandService;


    private final ThreadPoolExecutor threadPoolExecutor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new LambdaQueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.baseMapper.insert(skuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        LambdaQueryWrapper<SkuInfoEntity> queryWrapper = new LambdaQueryWrapper<SkuInfoEntity>();
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and((wrapper) -> {
                wrapper.eq(SkuInfoEntity::getSkuId, key).or().like(SkuInfoEntity::getSkuName, key);
            });
        }
        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)) {

            queryWrapper.eq(SkuInfoEntity::getCatalogId, catelogId);
        }
        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(catelogId)) {
            queryWrapper.eq(SkuInfoEntity::getBrandId, brandId);
        }
        String min = (String) params.get("min");
        if (!StringUtils.isEmpty(min)) {
            queryWrapper.ge(SkuInfoEntity::getPrice, min);
        }
        String max = (String) params.get("max");
        if (!StringUtils.isEmpty(max)) {
            try {
                BigDecimal bigDecimal = new BigDecimal(max);
                if (bigDecimal.compareTo(new BigDecimal("0")) > 0) {
                    queryWrapper.le(SkuInfoEntity::getPrice, max);
                }
            } catch (Exception e) {

            }
        }
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                queryWrapper
        );
        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        return this.list(new LambdaQueryWrapper<SkuInfoEntity>().eq(SkuInfoEntity::getSpuId, spuId));
    }

    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVo skuItemVo = new SkuItemVo();
        // 1、sku基本信息的获取：pms_sku_info
        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfoEntity info = this.getById(skuId);
            skuItemVo.setInfo(info);
            // Long catalogId = info.getCatalogId();
            // Long spuId = info.getSpuId();
            return info;
        }, threadPoolExecutor);

        // 3、获取spu的销售属性组合
        CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            List<SkuItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService.getSaleAttrBySpuId(res.getSpuId());
            skuItemVo.setSaleAttr(saleAttrVos);
        }, threadPoolExecutor);

        // 4、获取spu的介绍：pms_spu_info_desc
        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync((res) -> {
            SpuInfoDescEntity spuInfoDescEntity = spuInfoDescService.getById(res.getSpuId());
            skuItemVo.setDesc(spuInfoDescEntity);
        }, threadPoolExecutor);

        // 5、获取spu的规格参数信息
        CompletableFuture<Void> baseAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            List<SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
            skuItemVo.setGroupAttrs(attrGroupVos);
        }, threadPoolExecutor);

        // 注意必须放在最后
        // 2、sku的图片信息：pms_sku_images
        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            List<SkuImagesEntity> imagesEntities = skuImagesService.getImagesBySkuId(skuId);
            skuItemVo.setImages(imagesEntities);
        }, threadPoolExecutor);

        CompletableFuture<Void> seckillFuture = CompletableFuture.runAsync(() -> {
            // 3、远程调用查询当前sku是否参与秒杀优惠活动
            Result<Object> skuSeckilInfo = seckillFeignService.getSkuSeckilInfo(skuId);
            if (skuSeckilInfo.getCode() == 0) {
                //查询成功
SeckillSkuVo seckilInfoData = (SeckillSkuVo) skuSeckilInfo.getData();
                skuItemVo.setSeckillSkuVo(seckilInfoData);

                if (seckilInfoData != null) {
                    long currentTime = new Date().getTime();
                    if (currentTime > seckilInfoData.getEndTime()) {
                        skuItemVo.setSeckillSkuVo(null);
                    }
                }
            }
        }, threadPoolExecutor);

        // 等到所有任务都完成
        CompletableFuture.allOf(saleAttrFuture, descFuture, baseAttrFuture, imageFuture, seckillFuture).get();

        // 品牌名（详情页店铺名展示/跳转用）：品牌缺失不阻塞详情
        SkuInfoEntity skuInfo = skuItemVo.getInfo();
        if (skuInfo != null && skuInfo.getBrandId() != null) {
            try {
                BrandEntity brand = brandService.getById(skuInfo.getBrandId());
                if (brand != null) {
                    skuItemVo.setBrandName(brand.getName());
                    skuItemVo.setBrandLogo(brand.getLogo());
                }
            } catch (Exception e) {
                log.warn("item：查询品牌 " + skuInfo.getBrandId() + " 异常: " + e.getMessage());
            }
        }

        return skuItemVo;
    }

    @Override
    public Map<String, Object> getSkuDetail(Long skuId) {
        SkuInfoEntity sku = this.getById(skuId);
        List<SkuImagesEntity> images = skuImagesService.list(
                new LambdaQueryWrapper<SkuImagesEntity>().eq(SkuImagesEntity::getSkuId, skuId));
        Map<String, Object> map = new HashMap<>();
        map.put("sku", sku);
        map.put("images", images);
        // SPU 商品细节图（pms_spu_info_desc，decript 逗号分隔，按 spuId 查）
        if (sku != null && sku.getSpuId() != null) {
            SpuInfoDescEntity desc = spuInfoDescService.getById(sku.getSpuId());
            if (desc != null && desc.getDecript() != null && !desc.getDecript().isEmpty()) {
                map.put("decript", Arrays.asList(desc.getDecript().split(",")));
            }
        }
        return map;
    }
}