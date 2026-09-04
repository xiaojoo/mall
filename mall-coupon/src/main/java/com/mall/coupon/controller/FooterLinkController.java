package com.mall.coupon.controller;

import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import com.mall.coupon.entity.FooterLinkEntity;
import com.mall.coupon.service.FooterLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 页脚链接
 *
 * - /list          商城端：启用中的页脚链接（mall-ui AppFooter 调用）
 * - /admin/list    管理端：分页查询（可按 key/status 过滤）
 * - /info /save /update /delete  管理
 */
@RestController
@RequestMapping("coupon/footerlink")
@RequiredArgsConstructor
public class FooterLinkController {

    private final FooterLinkService footerLinkService;

    /**
     * 商城端：启用中的页脚链接列表
     */
    @GetMapping("/list")
    public Result<List<FooterLinkEntity>> list() {
        return Result.success(footerLinkService.listEnabled());
    }

    /**
     * 管理端：分页查询
     */
    @GetMapping("/admin/list")
    public Result<PageUtils> adminList(@RequestParam Map<String, Object> params) {
        return Result.success(footerLinkService.queryPage(params));
    }

    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<FooterLinkEntity> info(@PathVariable Long id) {
        return Result.success(footerLinkService.getById(id));
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody FooterLinkEntity footerLink) {
        footerLinkService.save(footerLink);
        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody FooterLinkEntity footerLink) {
        footerLinkService.updateById(footerLink);
        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        footerLinkService.removeByIds(Arrays.asList(ids));
        return Result.success();
    }
}
