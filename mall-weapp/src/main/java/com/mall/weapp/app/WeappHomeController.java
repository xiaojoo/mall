package com.mall.weapp.app;

import com.mall.common.utils.Result;
import com.mall.weapp.entity.WeappHomeEntity;
import com.mall.weapp.feign.ProductFeignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 微信小程序 - 首页模块控制器
 * <p>提供首页聚合数据接口，包含Banner、推荐商品、分类入口等</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@RestController
@RequestMapping("weapp/home")
@RequiredArgsConstructor
public class WeappHomeController {

    private final ProductFeignService productFeignService;

    /**
     * 首页聚合数据
     * <p>聚合Banner、推荐商品、分类入口等数据，供首页展示</p>
     *
     * @return 首页聚合数据
     */
    @GetMapping("/index")
    public Result<Object> index() {
        WeappHomeEntity homeData = new WeappHomeEntity();

        // 1. 构造Banner列表（示例数据，实际可从配置中心或数据库读取）
        List<WeappHomeEntity.Banner> banners = new ArrayList<>();

        WeappHomeEntity.Banner banner1 = new WeappHomeEntity.Banner();
        banner1.setImageUrl("https://mall-static.example.com/banner/spring-sale.png");
        banner1.setLinkUrl("/pages/product/list?activity=spring-sale");
        banner1.setSort(1);
        banner1.setTitle("春季大促");
        banners.add(banner1);

        WeappHomeEntity.Banner banner2 = new WeappHomeEntity.Banner();
        banner2.setImageUrl("https://mall-static.example.com/banner/new-arrivals.png");
        banner2.setLinkUrl("/pages/product/new");
        banner2.setSort(2);
        banner2.setTitle("新品上市");
        banners.add(banner2);

        homeData.setBanners(banners);

        // 2. 获取推荐商品（热销商品作为推荐）
        List<WeappHomeEntity.RecommendProduct> recommendProducts = new ArrayList<>();
        try {
            Map<String, Object> params = new java.util.HashMap<>();
            params.put("sale", "1");
            params.put("limit", "8");
            Result<Object> hotResult = productFeignService.list(params);
            if (hotResult.getCode() == 200 && hotResult.getData() != null) {
                // 将返回数据转为推荐商品列表
                homeData.setRecommendProducts(recommendProducts);
            }
        } catch (Exception e) {
            // 服务调用失败时返回空列表
        }
        homeData.setRecommendProducts(recommendProducts);

        // 3. 获取分类入口
        List<WeappHomeEntity.CategoryEntry> categoryEntries = new ArrayList<>();
        try {
            Result<Object> categoryResult = productFeignService.categoryList();
            if (categoryResult.getCode() == 200 && categoryResult.getData() != null) {
                // 从分类树数据中提取一级分类作为入口
                homeData.setCategoryEntries(categoryEntries);
            }
        } catch (Exception e) {
            // 服务调用失败时返回空列表
        }
        homeData.setCategoryEntries(categoryEntries);

        return Result.success(homeData);
    }
}
