package com.mall.coupon.controller;

import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import com.mall.coupon.entity.HomeNavEntity;
import com.mall.coupon.service.HomeNavService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 首页快捷导航（cat-row：全息设备 … 新品首发）
 *
 * - /list          商城端：启用中的导航（mall-ui AppNav 调用）
 * - /admin/list    管理端：分页查询（可按 name/status 过滤）
 * - /info /save /update /delete  管理
 */
@RestController
@RequestMapping("coupon/homenav")
@RequiredArgsConstructor
public class HomeNavController {

    private final HomeNavService homeNavService;

    /**
     * 商城端：启用中的导航列表
     */
    @GetMapping("/list")
    public Result<List<HomeNavEntity>> list() {
        return Result.success(homeNavService.listEnabled());
    }

    /**
     * 管理端：分页查询
     */
    @GetMapping("/admin/list")
    public Result<PageUtils> adminList(@RequestParam Map<String, Object> params) {
        return Result.success(homeNavService.queryPage(params));
    }

    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<HomeNavEntity> info(@PathVariable Long id) {
        return Result.success(homeNavService.getById(id));
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody HomeNavEntity homeNav) {
        homeNavService.save(homeNav);
        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody HomeNavEntity homeNav) {
        homeNavService.updateById(homeNav);
        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        homeNavService.removeByIds(Arrays.asList(ids));
        return Result.success();
    }
}
