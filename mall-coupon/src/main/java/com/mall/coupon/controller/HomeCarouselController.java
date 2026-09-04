package com.mall.coupon.controller;

import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import com.mall.coupon.entity.HomeCarouselEntity;
import com.mall.coupon.service.HomeCarouselService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 首页轮播内容（mall-ui 首页 HERO 轮播）
 *
 * - /list          商城端：启用中的轮播，按 sort 升序（mall-ui 首页调用）
 * - /admin/list    管理端：分页查询（含停用，可按 name/status 过滤）
 * - /info /save /update /delete  管理
 */
@RestController
@RequestMapping("coupon/carousel")
@RequiredArgsConstructor
public class HomeCarouselController {

    private final HomeCarouselService homeCarouselService;

    /**
     * 商城端：启用中的轮播列表
     */
    @GetMapping("/list")
    public Result<List<HomeCarouselEntity>> list() {
        return Result.success(homeCarouselService.listEnabled());
    }

    /**
     * 管理端：分页查询
     */
    @GetMapping("/admin/list")
    public Result<PageUtils> adminList(@RequestParam Map<String, Object> params) {
        return Result.success(homeCarouselService.queryPage(params));
    }

    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<HomeCarouselEntity> info(@PathVariable Long id) {
        return Result.success(homeCarouselService.getById(id));
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody HomeCarouselEntity carousel) {
        homeCarouselService.save(carousel);
        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody HomeCarouselEntity carousel) {
        homeCarouselService.updateById(carousel);
        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        homeCarouselService.removeByIds(Arrays.asList(ids));
        return Result.success();
    }
}
