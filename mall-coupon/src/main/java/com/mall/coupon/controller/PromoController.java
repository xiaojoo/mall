package com.mall.coupon.controller;

import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import com.mall.coupon.entity.PromoEntity;
import com.mall.coupon.service.PromoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 首页大促横条
 *
 * - /list          商城端：启用中的大促列表（mall-ui AppPromo 调用）
 * - /admin/list    管理端：分页查询（可按 key/status 过滤）
 * - /info /save /update /delete  管理
 */
@RestController
@RequestMapping("coupon/promo")
@RequiredArgsConstructor
public class PromoController {

    private final PromoService promoService;

    /**
     * 商城端：启用中的大促列表
     */
    @GetMapping("/list")
    public Result<List<PromoEntity>> list() {
        return Result.success(promoService.listEnabled());
    }

    /**
     * 管理端：分页查询
     */
    @GetMapping("/admin/list")
    public Result<PageUtils> adminList(@RequestParam Map<String, Object> params) {
        return Result.success(promoService.queryPage(params));
    }

    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<PromoEntity> info(@PathVariable Long id) {
        return Result.success(promoService.getById(id));
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody PromoEntity promo) {
        promoService.save(promo);
        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody PromoEntity promo) {
        promoService.updateById(promo);
        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        promoService.removeByIds(Arrays.asList(ids));
        return Result.success();
    }
}
