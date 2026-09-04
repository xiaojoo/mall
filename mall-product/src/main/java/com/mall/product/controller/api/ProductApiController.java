package com.mall.product.controller.api;

import com.mall.common.jwt.MemberJwtUtils;
import com.mall.common.utils.Result;
import com.mall.product.entity.CategoryEntity;
import com.mall.product.feign.CouponFeignService;
import com.mall.product.feign.OrderFeignService;
import com.mall.product.service.CategoryService;
import com.mall.product.service.ShopService;
import com.mall.product.service.SkuInfoService;
import com.mall.product.vo.Catalog2Vo;
import com.mall.product.vo.ShopSummaryVo;
import com.mall.product.vo.ShopVo;
import com.mall.product.vo.SkuItemVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 商品 API 接口（前后端分离）
 * <p>
 * 替代原模板引擎页面接口：商城首页（index.html）、商品详情页（item.html）。
 * 数据契约与原模板保持一致，供 mall-ui 前端消费。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductApiController {

    private final CategoryService categoryService;

    private final SkuInfoService skuInfoService;

    private final ShopService shopService;

    private final CouponFeignService couponFeignService;

    private final OrderFeignService orderFeignService;

    private final MemberJwtUtils memberJwtUtils;

    /**
     * 店铺列表（mall-web 商家账号发布的品牌，showStatus=1），供店铺页默认店铺/切换使用
     */
    @GetMapping("/shop/list")
    public Result<List<ShopSummaryVo>> shopList() {
        return Result.success(shopService.listShops());
    }

    /**
     * 店铺详情（mall-ui /shop 渲染）：店铺信息 + 在售商品（已上架 SPU）
     *
     * @param brandId 品牌 id（店铺 id）
     */
    @GetMapping("/shop/{brandId}")
    public Result<ShopVo> shopDetail(@PathVariable Long brandId) {
        ShopVo shop = shopService.getShopDetail(brandId);
        if (shop == null) {
            return Result.fail("店铺不存在或已关闭");
        }
        return Result.success(shop);
    }

    /**
     * 当前登录会员是否可评价该商品（已购买且支付成功）；
     * 未登录返回 false；订单服务不可用时降级放行返回 true。
     */
    @GetMapping("/comment/paidCheck")
    public Result<Boolean> commentPaidCheck(@RequestParam("skuId") Long skuId,
                                            HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        if (memberId == null) {
            return Result.success(false);
        }
        try {
            Result<Boolean> r = orderFeignService.paidCheck(memberId, skuId, null);
            return Result.success(r != null && Boolean.TRUE.equals(r.getData()));
        } catch (Exception e) {
            log.error("评论购买校验异常，默认不可评价: skuId={}, err={}", skuId, e.getMessage(), e);
            return Result.success(false);
        }
    }

    /**
     * 商城首页数据（对应原 index.html 的数据契约）
     * <ul>
     *     <li>{@code categories}      - 一级分类导航，已接入 CategoryService</li>
     *     <li>{@code banners}         - 轮播图，TODO: 待 mall-coupon 提供首页轮播接口后接入</li>
     *     <li>{@code hotProducts}     - 热门商品，TODO: 待补充推荐商品接口后接入</li>
     *     <li>{@code seckillProducts} - 秒杀商品，TODO: 待 mall-seckill 提供当前秒杀接口后接入</li>
     * </ul>
     */
    @GetMapping("/home")
    public Result<Map<String, Object>> home() {
        List<CategoryEntity> categories = categoryService.getLevel1Categorys();

        // 首页轮播：mall-coupon 提供；失败/为空时给空列表，不影响首页其他数据
        List<Map<String, Object>> banners = Collections.emptyList();
        try {
            Result<List<Map<String, Object>>> carouselRes = couponFeignService.listCarousel();
            if (carouselRes != null && carouselRes.getCode() == 200 && carouselRes.getData() != null) {
                banners = carouselRes.getData();
            }
        } catch (Exception e) {
            log.warn("获取首页轮播失败，使用空列表: {}", e.getMessage());
        }

        // 首页跑马灯公告：mall-coupon 提供；失败/为空时给空列表
        List<String> ticker = Collections.emptyList();
        try {
            Result<List<String>> tickerRes = couponFeignService.listTicker();
            if (tickerRes != null && tickerRes.getCode() == 200 && tickerRes.getData() != null) {
                ticker = tickerRes.getData();
            }
        } catch (Exception e) {
            log.warn("获取跑马灯公告失败，使用空列表: {}", e.getMessage());
        }

        // 首页大促横条：mall-coupon 提供；失败/为空时给空列表
        List<Map<String, Object>> promo = Collections.emptyList();
        try {
            Result<List<Map<String, Object>>> promoRes = couponFeignService.listPromo();
            if (promoRes != null && promoRes.getCode() == 200 && promoRes.getData() != null) {
                promo = promoRes.getData();
            }
        } catch (Exception e) {
            log.warn("获取大促横条失败，使用空列表: {}", e.getMessage());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("categories", categories);
        data.put("banners", banners);
        data.put("ticker", ticker);
        data.put("promo", promo);
        data.put("hotProducts", Collections.emptyList());
        data.put("seckillProducts", Collections.emptyList());
        return Result.success(data);
    }

    /**
     * 页脚链接（mall-ui AppFooter 全站调用；mall-coupon 提供，失败/为空给空列表，前端保留内置兑底）
     */
    @GetMapping("/footer")
    public Result<Map<String, Object>> footer() {
        List<Map<String, Object>> footerLinks = Collections.emptyList();
        try {
            Result<List<Map<String, Object>>> footerRes = couponFeignService.listFooterLink();
            if (footerRes != null && footerRes.getCode() == 200 && footerRes.getData() != null) {
                footerLinks = footerRes.getData();
            }
        } catch (Exception e) {
            log.warn("获取页脚链接失败，使用空列表: {}", e.getMessage());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("footerLinks", footerLinks);
        return Result.success(data);
    }

    /**
     * 首页三级分类树（对应原 /index/catalog.json）
     */
    @GetMapping("/catalog")
    public Result<Map<String, List<Catalog2Vo>>> catalog() {
        return Result.success(categoryService.getCatalogJson());
    }

    /**
     * 分类树（含子分类，供前端菜单/分类导航使用）
     */
    @GetMapping("/category/tree")
    public Result<List<CategoryEntity>> categoryTree() {
        return Result.success(categoryService.listWithTree());
    }

    /**
     * 商品详情（对应原 item.html 页面数据）
     *
     * @param skuId 商品 SKU ID
     */
    @GetMapping("/detail/{skuId}")
    public Result<SkuItemVo> detail(@PathVariable Long skuId) {
        try {
            return Result.success(skuInfoService.item(skuId));
        } catch (ExecutionException | InterruptedException e) {
            log.error("查询商品详情失败, skuId={}", skuId, e);
            Thread.currentThread().interrupt();
            return Result.fail("查询商品详情失败：" + e.getMessage());
        }
    }
}
