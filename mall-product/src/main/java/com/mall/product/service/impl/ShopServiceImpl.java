package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.product.entity.BrandEntity;
import com.mall.product.entity.CategoryEntity;
import com.mall.product.entity.SkuInfoEntity;
import com.mall.product.entity.SpuCommentEntity;
import com.mall.product.entity.SpuInfoEntity;
import com.mall.product.service.BrandService;
import com.mall.product.service.CategoryService;
import com.mall.product.service.ShopService;
import com.mall.product.service.SkuInfoService;
import com.mall.product.service.SpuCommentService;
import com.mall.product.service.SpuInfoService;
import com.mall.product.vo.ShopProductVo;
import com.mall.product.vo.ShopSummaryVo;
import com.mall.product.vo.ShopVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 店铺渲染服务实现（C 端聚合）
 * <p>数据全部来源于 mall-web 商家账号发布的品牌/商品/评论。</p>
 */
@Slf4j
@Service("shopService")
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final BrandService brandService;

    private final SpuInfoService spuInfoService;

    private final SkuInfoService skuInfoService;

    private final CategoryService categoryService;

    private final SpuCommentService spuCommentService;

    @Override
    public List<ShopSummaryVo> listShops() {
        // 展示中的品牌（商家）
        List<BrandEntity> brands = brandService.list(
                new LambdaQueryWrapper<BrandEntity>()
                        .eq(BrandEntity::getShowStatus, 1)
                        .orderByAsc(BrandEntity::getSort));
        if (brands.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> brandIds = brands.stream().map(BrandEntity::getBrandId)
                .filter(Objects::nonNull).collect(Collectors.toList());

        // 每个品牌已上架商品数
        Map<Long, Long> productCountMap = new HashMap<>();
        if (!brandIds.isEmpty()) {
            List<SpuInfoEntity> spus = spuInfoService.list(
                    new LambdaQueryWrapper<SpuInfoEntity>()
                            .eq(SpuInfoEntity::getPublishStatus, 1)
                            .in(SpuInfoEntity::getBrandId, brandIds));
            productCountMap = spus.stream()
                    .filter(s -> s.getBrandId() != null)
                    .collect(Collectors.groupingBy(SpuInfoEntity::getBrandId, Collectors.counting()));
        }

        Map<Long, Long> finalCountMap = productCountMap;
        // 只返回有在售商品的店铺（无商品的品牌不展示）
        return brands.stream()
                .filter(b -> finalCountMap.getOrDefault(b.getBrandId(), 0L) > 0)
                .map(b -> {
                    ShopSummaryVo vo = new ShopSummaryVo();
                    vo.setBrandId(b.getBrandId());
                    vo.setShopName(b.getName());
                    vo.setLogo(b.getLogo());
                    vo.setProductCount(finalCountMap.getOrDefault(b.getBrandId(), 0L));
                    return vo;
                }).collect(Collectors.toList());
    }

    @Override
    public ShopVo getShopDetail(Long brandId) {
        if (brandId == null) {
            return null;
        }
        BrandEntity brand = brandService.getById(brandId);
        // 不存在或未展示（商家未发布/已下架店铺）视为无此店铺
        if (brand == null || !Integer.valueOf(1).equals(brand.getShowStatus())) {
            return null;
        }

        ShopVo vo = new ShopVo();
        vo.setBrandId(brand.getBrandId());
        vo.setShopName(brand.getName());
        vo.setLogo(brand.getLogo());
        vo.setDescript(brand.getDescript());

        // 在售商品（商家已上架 SPU）
        List<SpuInfoEntity> spus = spuInfoService.list(
                new LambdaQueryWrapper<SpuInfoEntity>()
                        .eq(SpuInfoEntity::getBrandId, brandId)
                        .eq(SpuInfoEntity::getPublishStatus, 1));
        if (spus.isEmpty()) {
            vo.setProductCount(0L);
            vo.setCategoryCount(0L);
            vo.setTotalSales(0L);
            vo.setProducts(Collections.emptyList());
            return vo;
        }

        List<Long> spuIds = spus.stream().map(SpuInfoEntity::getId)
                .filter(Objects::nonNull).collect(Collectors.toList());

        // 每个 SPU 取 skuId 最小的 SKU（价格 + 主图 + 跳转详情）
        List<SkuInfoEntity> allSkus = skuInfoService.list(
                new LambdaQueryWrapper<SkuInfoEntity>()
                        .in(SkuInfoEntity::getSpuId, spuIds)
                        .orderByAsc(SkuInfoEntity::getSkuId));
        Map<Long, SkuInfoEntity> firstSkuMap = allSkus.stream()
                .collect(Collectors.toMap(SkuInfoEntity::getSpuId, Function.identity(), (a, b) -> a));

        // 分类名称（批量查一次）
        Set<Long> catalogIds = spus.stream().map(SpuInfoEntity::getCatalogId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> catNames = catalogIds.isEmpty() ? Collections.emptyMap()
                : categoryService.listByIds(catalogIds).stream()
                .collect(Collectors.toMap(CategoryEntity::getCatId, CategoryEntity::getName, (a, b) -> a));

        // 商品评分：仅首评（commentType=0）且展示中，按 SPU 分组取星级均值
        Map<Long, Double> spuRatingMap;
        List<SpuCommentEntity> comments = spuCommentService.list(
                new LambdaQueryWrapper<SpuCommentEntity>()
                        .in(SpuCommentEntity::getSpuId, spuIds)
                        .eq(SpuCommentEntity::getCommentType, 0)
                        .eq(SpuCommentEntity::getShowStatus, 1)
                        .isNotNull(SpuCommentEntity::getStar));
        if (comments.isEmpty()) {
            spuRatingMap = Collections.emptyMap();
        } else {
            spuRatingMap = comments.stream()
                    .collect(Collectors.groupingBy(SpuCommentEntity::getSpuId,
                            Collectors.averagingInt(SpuCommentEntity::getStar)));
        }
        Map<Long, Double> finalRatingMap = spuRatingMap;

        // 累计销量：在售 SKU 销量合计（null 安全）
        long totalSales = allSkus.stream()
                .mapToLong(s -> s.getSaleCount() == null ? 0L : s.getSaleCount())
                .sum();

        List<ShopProductVo> products = spus.stream().map(spu -> {
            ShopProductVo p = new ShopProductVo();
            p.setSpuId(spu.getId());
            p.setSpuName(spu.getSpuName());
            p.setCatalogId(spu.getCatalogId());
            p.setCatalogName(catNames.get(spu.getCatalogId()));
            p.setRating(finalRatingMap.get(spu.getId()));
            SkuInfoEntity firstSku = firstSkuMap.get(spu.getId());
            if (firstSku != null) {
                p.setSkuId(firstSku.getSkuId());
                p.setPrice(firstSku.getPrice());
                p.setImg(firstSku.getSkuDefaultImg());
                p.setSales(firstSku.getSaleCount());
            }
            return p;
        }).collect(Collectors.toList());

        vo.setProductCount((long) products.size());
        vo.setCategoryCount((long) catalogIds.size());
        vo.setTotalSales(totalSales);
        vo.setProducts(products);

        // 店铺评分：各商品首评均值的平均（无评分商品不参与）
        List<Double> ratings = products.stream()
                .map(ShopProductVo::getRating).filter(Objects::nonNull).collect(Collectors.toList());
        if (!ratings.isEmpty()) {
            vo.setRating(ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        }
        return vo;
    }
}
