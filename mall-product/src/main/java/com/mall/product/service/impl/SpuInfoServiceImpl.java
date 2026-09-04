package com.mall.product.service.impl;


import com.mall.common.constant.ProductConstant;
import com.mall.common.exception.BizCodeEnum;
import com.mall.common.to.SkuReductionTo;
import com.mall.common.to.SpuBoundTo;
import com.mall.common.to.es.SkuEsModel;
import com.mall.common.utils.Result;
import com.mall.common.vo.SkuHasStockVo;
import com.mall.product.entity.*;
import com.mall.product.feign.CouponFeignService;
import com.mall.product.feign.SearchFeignService;
import com.mall.product.feign.WareFeignService;
import com.mall.product.service.*;
import com.mall.product.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;


@Service("spuInfoService")
@RequiredArgsConstructor
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    final SpuInfoDescService spuInfoDescService;


    final SpuImagesService spuImagesService;


    final AttrService attrService;


    final ProductAttrValueService attrValueService;


    final SkuInfoService skuInfoService;


    final SkuImagesService skuImagesService;


    final SkuSaleAttrValueService skuSaleAttrValueService;


    final CouponFeignService couponFeignService;


    final BrandService brandService;


    final CategoryService categoryService;


    final WareFeignService wareFeignService;


    final SearchFeignService searchFeignService;

    /**
     * 删除 SPU 并级联删除其下所有 SKU 及关联数据（图片/销售属性/描述/图集/规格参数）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(Collection<?> list) {
        List<Long> spuIds = list.stream()
                .map(id -> Long.valueOf(id.toString()))
                .collect(Collectors.toList());
        if (spuIds.isEmpty()) {
            return false;
        }
        // 1、查出这些 SPU 下的所有 SKU
        List<SkuInfoEntity> skus = skuInfoService.list(
                new LambdaQueryWrapper<SkuInfoEntity>().in(SkuInfoEntity::getSpuId, spuIds));
        List<Long> skuIds = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
        if (!skuIds.isEmpty()) {
            // 1.1、删 SKU 关联数据
            skuImagesService.remove(new LambdaQueryWrapper<SkuImagesEntity>().in(SkuImagesEntity::getSkuId, skuIds));
            skuSaleAttrValueService.remove(new LambdaQueryWrapper<SkuSaleAttrValueEntity>().in(SkuSaleAttrValueEntity::getSkuId, skuIds));
            // 1.2、删 SKU 基本信息（SPU管理界面记录随之减少）
            skuInfoService.removeByIds(skuIds);
        }
        // 2、删 SPU 关联数据
        spuInfoDescService.remove(new LambdaQueryWrapper<SpuInfoDescEntity>().in(SpuInfoDescEntity::getSpuId, spuIds));
        spuImagesService.remove(new LambdaQueryWrapper<SpuImagesEntity>().in(SpuImagesEntity::getSpuId, spuIds));
        attrValueService.remove(new LambdaQueryWrapper<ProductAttrValueEntity>().in(ProductAttrValueEntity::getSpuId, spuIds));
        // 3、删 SPU
        return super.removeByIds(list);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new LambdaQueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public Long saveSpuInfo(SpuSaveVo vo) {
        // 1、保存spu基本信息 pms_spu_info
        SpuInfoEntity infoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo, infoEntity);
        infoEntity.setCreateTime(new Date());
        infoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(infoEntity);
        // 2、保存Spu的描述图片
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
        descEntity.setSpuId(infoEntity.getId());
        descEntity.setDecript(String.join(",", decript));
        spuInfoDescService.saveSpuInfoDesc(descEntity);
        // 3、保存spu的图片集 pms_spu_images
        List<String> images = vo.getImages();
        spuImagesService.saveImages(infoEntity.getId(), images);
        // 4、保存spu的规格参数 pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> productAttrValueEntity = baseAttrs.stream()
                .filter(attr -> attr.getAttrId() != null)
                .map(attr -> {
                    AttrEntity id = attrService.getById(attr.getAttrId());
                    if (id == null) {
                        // 属性已被删除，跳过该条，避免 NPE 导致整个商品保存失败
                        return null;
                    }
                    ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();
                    // 必须保存 attr_id，否则 pms_product_attr_value.attr_id 为 NULL，
                    // 上架时无法按属性类型过滤（listByIds 查出 null 导致 attrs 为空）
                    valueEntity.setAttrId(attr.getAttrId());
                    valueEntity.setAttrName(id.getAttrName());
                    valueEntity.setAttrValue(attr.getAttrValues());
                    valueEntity.setQuickShow(attr.getShowDesc());
                    valueEntity.setSpuId(infoEntity.getId());
                    return valueEntity;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        attrValueService.saveProductAttr(productAttrValueEntity);
        // 5、保存spu的积分信息 sms_spu_bounds（失败则整个发布回滚）
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds, spuBoundTo);
        spuBoundTo.setSpuId(infoEntity.getId());
        Result<Object> r = couponFeignService.saveSpuBounds(spuBoundTo);
        if (r.getCode() != 200) {
            throw new RuntimeException("远程保存spu积分信息失败：" + r.getMessage());
        }
        // 6 保存当前spu对应的所有sku信息
        List<Skus> skus = vo.getSkus();
        if (skus != null && !skus.isEmpty()) {
            skus.forEach(item -> {
                // 获取默认图片地址
                String defaultImg = "";
                for (Images image : item.getImages()) {
                    if (image.getDefaultImg() == 1) {
                        defaultImg = image.getImgUrl();
                    }
                }
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item, skuInfoEntity);
                skuInfoEntity.setBrandId(infoEntity.getBrandId());
                skuInfoEntity.setCatalogId(infoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(infoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                // 6.1）、sku的基本信息；pms_sku_info
                skuInfoService.saveSkuInfo(skuInfoEntity);
                Long skuId = skuInfoEntity.getSkuId();
                List<SkuImagesEntity> skuImagesEntity = item.getImages().stream().map(img -> {
                    SkuImagesEntity imagesEntity = new SkuImagesEntity();
                    imagesEntity.setSkuId(skuId);
                    imagesEntity.setImgUrl(img.getImgUrl());
                    imagesEntity.setDefaultImg(img.getDefaultImg());
                    return imagesEntity;
                }).filter(entity -> {
                    // 返回true就是需要，false就是剔除
                    return !StringUtils.isEmpty(entity.getImgUrl());
                }).collect(Collectors.toList());
                // 6.2）、sku的图片信息；pms_sku_image
                skuImagesService.saveBatch(skuImagesEntity);
                // 6.3）、sku的销售属性信息：pms_sku_sale_attr_value
                List<Attr> attr = item.getAttr();
                List<SkuSaleAttrValueEntity> saleAttrValueEntity = attr.stream().map(a -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(a, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(saleAttrValueEntity);
                // 6.4）、sku的优惠、满减等信息；mall_sms->sms_sku_ladder\sms_sku_full_reduction\sms_member_price
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(item, skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                // 会员价单独判断：只要存在有效会员价（价格>0）就必须走优惠保存，
                // 否则被 fullCount/fullPrice 条件挡住，会员价永远落不了库
                List<com.mall.common.to.MemberPrice> memberPrice = item.getMemberPrice();
                boolean hasMemberPrice = memberPrice != null && memberPrice.stream()
                        .anyMatch(mp -> mp.getPrice() != null && mp.getPrice().compareTo(new BigDecimal("0")) > 0);
                if (skuReductionTo.getFullCount() > 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) > 0 || hasMemberPrice) {
                    Result<Object> r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if (r1.getCode() != 200) {
                        throw new RuntimeException("远程保存sku优惠信息失败：" + r1.getMessage());
                    }
                }
            });
        }
        // 7、保存成功后返回 spuId（批量发布用）
        return infoEntity.getId();
    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }

    @Override
    public List<Map<String, Object>> batchPublish(List<SpuSaveVo> vos) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (vos == null || vos.isEmpty()) {
            return results;
        }
        for (SpuSaveVo vo : vos) {
            Map<String, Object> item = new HashMap<>();
            item.put("spuName", vo.getSpuName());
            try {
                // 批量发布默认上架状态
                vo.setPublishStatus(1);
                // 1. 保存商品（含 SPU/描述图/图片集/规格参数/SKU，图片直接传 URL）
                Long spuId = saveSpuInfo(vo);
                // 2. 上架：写入 ES 商品索引
                Result<Object> upRes = up(spuId);
                boolean ok = upRes != null && upRes.getCode() == 200;
                item.put("success", ok);
                item.put("spuId", spuId);
                if (!ok) {
                    item.put("error", upRes == null ? "上架失败" : upRes.getMessage());
                }
            } catch (Exception e) {
                // 单条失败不影响其他条（本条事务已回滚）
                item.put("success", false);
                item.put("error", e.getMessage());
                log.error("批量发布商品失败: " + vo.getSpuName(), e);
            }
            results.add(item);
        }
        return results;
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        LambdaQueryWrapper<SpuInfoEntity> queryWrapper = new LambdaQueryWrapper<SpuInfoEntity>();
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and((w) -> {
                w.eq(SpuInfoEntity::getId, key).or().like(SpuInfoEntity::getSpuName, key);
            });
        }
        String status = (String) params.get("status");
        if (!StringUtils.isEmpty(status)) {
            queryWrapper.eq(SpuInfoEntity::getPublishStatus, status);
        }
        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId)) {
            queryWrapper.eq(SpuInfoEntity::getBrandId, brandId);
        }
        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catelogId)) {
            queryWrapper.eq(SpuInfoEntity::getCatalogId, catelogId);
        }
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public Result<Object> up(Long spuId) {
        // 查出当前spuId对应的所有sku信息，品牌名字
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);
        List<Long> skuIdList = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());

        // 查询该 SPU 下所有 SKU 的销售属性值（pms_sku_sale_attr_value，每个 SKU 只有自己的值），
        // 按 skuId 分组后写入对应 SKU 的 ES 文档，保证按单个值筛选时只命中对应 SKU
        List<SkuSaleAttrValueEntity> skuSaleAttrs = skuSaleAttrValueService.list(
                new LambdaQueryWrapper<SkuSaleAttrValueEntity>()
                        .in(SkuSaleAttrValueEntity::getSkuId, skuIdList));
        Map<Long, List<SkuEsModel.Attrs>> skuAttrsMap = skuSaleAttrs.stream()
                // 过滤历史脏数据：空属性名/无效属性 id（错位修复前保存的占位行）
                .filter(v -> v.getAttrName() != null && !v.getAttrName().isEmpty()
                        && v.getAttrId() != null && v.getAttrId() > 0)
                .collect(Collectors.groupingBy(SkuSaleAttrValueEntity::getSkuId,
                        Collectors.mapping(v -> {
                            SkuEsModel.Attrs attr = new SkuEsModel.Attrs();
                            attr.setAttrId(v.getAttrId());
                            attr.setAttrName(v.getAttrName());
                            attr.setAttrValue(Collections.singletonList(v.getAttrValue()));
                            return attr;
                        }, Collectors.toList())));

        // 发送远程调用，库存系统查询是否有库存
        Map<Long, Boolean> stockMap = null;
        try {
            stockMap = wareFeignService.getSkuHasStock(skuIdList).stream()
                    .collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));
        } catch (Exception e) {
            log.error("库存服务查询异常：原因{}", e);
        }

        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> upProducts = skus.stream().map(sku -> {
            // 组装需要的数据
            SkuEsModel skuEsModel = new SkuEsModel();
            BeanUtils.copyProperties(sku, skuEsModel);
            skuEsModel.setSkuPrice(sku.getPrice());
            skuEsModel.setSkuImg(sku.getSkuDefaultImg());
            // 1、是否有库存
            if (finalStockMap == null) {
                skuEsModel.setHasStock(true);
            } else {
                skuEsModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }
            // 2、热度评分
            skuEsModel.setHotScore(0L);
            System.out.println(skuEsModel.getCatalogId());
            // 3、查询品牌和分类的名字信息
            BrandEntity brand = brandService.getById(skuEsModel.getBrandId());
            skuEsModel.setBrandName(brand.getName());
            skuEsModel.setBrandImg(brand.getLogo());
            CategoryEntity category = categoryService.getById(skuEsModel.getCatalogId());
            skuEsModel.setCatalogName(category.getName());

            // 设置检索属性
            // 设置检索属性（该 SKU 自己的销售属性值）
            skuEsModel.setAttrs(skuAttrsMap.getOrDefault(sku.getSkuId(), Collections.emptyList()));
            return skuEsModel;
        }).collect(Collectors.toList());

        // 将数据发送给es进行保存：mall-search
        Result<Object> r;
        try {
            r = searchFeignService.productStatusUp(upProducts);
        } catch (Exception e) {
            log.error("商品上架远程调用异常，spuId=" + spuId, e);
            return Result.fail(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), "商品上架失败：" + e.getMessage());
        }
        // Result 成功码为 200（非 0），失败时透传错误给调用方
        if (r.getCode() == 200) {
            // 远程调用成功，修改当前spu的状态
            baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
            return Result.success();
        }
        // 上架失败（幂等/重试由调用方决定），返回失败结果
        return r;
    }

    /**
     * 下架：删除 ES 中该 SPU 所有 SKU 的商品文档，并更新发布状态
     */
    @Override
    public Result<Object> down(Long spuId) {
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);
        List<Long> skuIds = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
        if (skuIds.isEmpty()) {
            // 没有 SKU 也算下架成功（无数据可删）
            baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_DOWN.getCode());
            return Result.success();
        }
        Result<Object> r;
        try {
            r = searchFeignService.productDown(skuIds);
        } catch (Exception e) {
            log.error("商品下架远程调用异常，spuId=" + spuId, e);
            return Result.fail(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), "商品下架失败：" + e.getMessage());
        }
        if (r.getCode() == 200) {
            baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_DOWN.getCode());
            return Result.success();
        }
        return r;
    }

    /**
     * 根据skuId查询spu的信息
     */
    @Override
    public SpuInfoEntity getSpuInfoBySkuId(Long skuId) {
        // 先查询sku表里的数据
        SkuInfoEntity skuInfoEntity = skuInfoService.getById(skuId);
        if (skuInfoEntity == null || skuInfoEntity.getSpuId() == null) {
            // sku 不存在或未关联 spu：返回 null（调用方降级处理），避免 NPE 抛 500
            log.warn("getSpuInfoBySkuId：sku " + skuId + " 不存在或未关联 spu");
            return null;
        }
        // 获得spuId
        Long spuId = skuInfoEntity.getSpuId();
        // 再通过spuId查询spuInfo信息表里的数据
        SpuInfoEntity spuInfoEntity = this.baseMapper.selectById(spuId);
        if (spuInfoEntity == null) {
            log.warn("getSpuInfoBySkuId：sku " + skuId + " 关联的 spu " + spuId + " 不存在");
            return null;
        }
        // 查询品牌表的数据获取品牌名（品牌缺失不阻塞返回，brandName/brandLogo 留空）
        try {
            BrandEntity brandEntity = brandService.getById(spuInfoEntity.getBrandId());
            if (brandEntity != null) {
                spuInfoEntity.setBrandName(brandEntity.getName());
                spuInfoEntity.setBrandLogo(brandEntity.getLogo());
            } else {
                log.warn("getSpuInfoBySkuId：spu " + spuId + " 关联的品牌 " + spuInfoEntity.getBrandId() + " 不存在，brandName 留空");
            }
        } catch (Exception e) {
            log.warn("getSpuInfoBySkuId：查询 spu " + spuId + " 品牌信息异常: " + e.getMessage());
        }

        return spuInfoEntity;
    }

    /**
     * 批量按 skuId 查询 spu 信息（品牌补全用，替代逐个 feign 调用）
     * <p>sku → spu 一次查询，spu 一次查询，品牌一次查询（品牌名回填，缺失不阻塞）。</p>
     */
    @Override
    public Map<Long, SpuInfoEntity> getSpuInfoMapBySkuIds(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = skuIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        // 1. sku → spuId（只需 skuId/spuId 两列）
        List<SkuInfoEntity> skus = skuInfoService.list(
                new LambdaQueryWrapper<SkuInfoEntity>()
                        .select(SkuInfoEntity::getSkuId, SkuInfoEntity::getSpuId)
                        .in(SkuInfoEntity::getSkuId, ids));
        Map<Long, Long> spuIdBySku = skus.stream()
                .filter(s -> s.getSpuId() != null)
                .collect(Collectors.toMap(SkuInfoEntity::getSkuId, SkuInfoEntity::getSpuId, (a, b) -> a));
        if (spuIdBySku.isEmpty()) {
            return Collections.emptyMap();
        }
        // 2. spu 信息（一次批量查）
        Map<Long, SpuInfoEntity> spuMap = this.listByIds(spuIdBySku.values()).stream()
                .collect(Collectors.toMap(SpuInfoEntity::getId, s -> s, (a, b) -> a));
        if (spuMap.isEmpty()) {
            return Collections.emptyMap();
        }
        // 3. 品牌名/logo 回填（一次批量查；品牌缺失不阻塞）
        Set<Long> brandIds = spuMap.values().stream()
                .map(SpuInfoEntity::getBrandId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> brandNames = Collections.emptyMap();
        Map<Long, String> brandLogos = Collections.emptyMap();
        if (!brandIds.isEmpty()) {
            List<BrandEntity> brands = brandService.listByIds(brandIds);
            brandNames = brands.stream()
                    .collect(Collectors.toMap(BrandEntity::getBrandId, BrandEntity::getName, (a, b) -> a));
            brandLogos = brands.stream()
                    .collect(Collectors.toMap(BrandEntity::getBrandId, BrandEntity::getLogo, (a, b) -> a));
        }
        for (SpuInfoEntity spu : spuMap.values()) {
            if (spu.getBrandId() != null) {
                spu.setBrandName(brandNames.get(spu.getBrandId()));
                spu.setBrandLogo(brandLogos.get(spu.getBrandId()));
            }
        }
        // 4. 组装 skuId -> spu
        Map<Long, SpuInfoEntity> result = new HashMap<>();
        spuIdBySku.forEach((skuId, spuId) -> {
            SpuInfoEntity spu = spuMap.get(spuId);
            if (spu != null) {
                result.put(skuId, spu);
            }
        });
        return result;
    }

    /**
     * 批量查询收藏展示信息：SPU 基本信息 + 首个 SKU（价格/跳转）+ 分类名
     */
    @Override
    public Map<Long, SpuFavVo> favInfo(List<Long> spuIds) {
        if (spuIds == null || spuIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = spuIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. SPU 基本信息
        Map<Long, SpuInfoEntity> spuMap = this.listByIds(ids).stream()
                .collect(Collectors.toMap(SpuInfoEntity::getId, s -> s, (a, b) -> a));
        if (spuMap.isEmpty()) {
            return Collections.emptyMap();
        }

        // 2. 每个 SPU 取 skuId 最小的 SKU（价格 + 主图 + 跳转详情）
        Map<Long, SkuInfoEntity> firstSkuMap = skuInfoService.list(
                        new LambdaQueryWrapper<SkuInfoEntity>()
                                .in(SkuInfoEntity::getSpuId, spuMap.keySet())
                                .orderByAsc(SkuInfoEntity::getSkuId))
                .stream()
                .collect(Collectors.toMap(SkuInfoEntity::getSpuId, s -> s, (a, b) -> a));

        // 3. 分类名称（批量查一次）
        Set<Long> catalogIds = spuMap.values().stream()
                .map(SpuInfoEntity::getCatalogId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> catNames = catalogIds.isEmpty() ? Collections.emptyMap()
                : categoryService.listByIds(catalogIds).stream()
                .collect(Collectors.toMap(CategoryEntity::getCatId, CategoryEntity::getName, (a, b) -> a));

        // 4. 组装返回
        Map<Long, SpuFavVo> result = new HashMap<>();
        for (SpuInfoEntity spu : spuMap.values()) {
            SpuFavVo vo = new SpuFavVo();
            vo.setSpuId(spu.getId());
            vo.setSpuName(spu.getSpuName());
            vo.setCatalogId(spu.getCatalogId());
            vo.setCategoryName(catNames.get(spu.getCatalogId()));
            SkuInfoEntity firstSku = firstSkuMap.get(spu.getId());
            if (firstSku != null) {
                vo.setSkuId(firstSku.getSkuId());
                vo.setPrice(firstSku.getPrice());
                vo.setSpuImg(firstSku.getSkuDefaultImg());
            }
            result.put(spu.getId(), vo);
        }
        return result;
    }
}