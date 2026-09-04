package com.mall.coupon.controller;

import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import com.mall.coupon.entity.TickerEntity;
import com.mall.coupon.service.TickerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 首页跑马灯公告
 *
 * - /list          商城端：启用中的公告文本列表（mall-ui 首页 Ticker 调用）
 * - /admin/list    管理端：分页查询（可按 content/status 过滤）
 * - /info /save /update /delete  管理
 */
@RestController
@RequestMapping("coupon/ticker")
@RequiredArgsConstructor
public class TickerController {

    private final TickerService tickerService;

    /**
     * 商城端：启用中的公告文本列表
     */
    @GetMapping("/list")
    public Result<List<String>> list() {
        return Result.success(tickerService.listEnabledTexts());
    }

    /**
     * 管理端：分页查询
     */
    @GetMapping("/admin/list")
    public Result<PageUtils> adminList(@RequestParam Map<String, Object> params) {
        return Result.success(tickerService.queryPage(params));
    }

    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<TickerEntity> info(@PathVariable Long id) {
        return Result.success(tickerService.getById(id));
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody TickerEntity ticker) {
        tickerService.save(ticker);
        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody TickerEntity ticker) {
        tickerService.updateById(ticker);
        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        tickerService.removeByIds(Arrays.asList(ids));
        return Result.success();
    }
}
