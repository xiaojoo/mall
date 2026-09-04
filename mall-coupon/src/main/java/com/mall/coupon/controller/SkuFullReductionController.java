package com.mall.coupon.controller;

import java.util.Arrays;
import java.util.Map;

import com.mall.common.to.SkuReductionTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.mall.coupon.entity.SkuFullReductionEntity;
import com.mall.coupon.service.SkuFullReductionService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import com.mall.common.dto.PageQueryDTO;


/**
 * 商品满减信息
 *
 * @author sunxiaojie
 * @date 2024-08-01 13:52:57
 */
@RestController
@RequestMapping("coupon/skufullreduction")
@RequiredArgsConstructor
public class SkuFullReductionController {

    private final SkuFullReductionService skuFullReductionService;

    /**
     * 发布商品远程保存优惠信息
     */
    @PostMapping("/savainfo")
    public Result<Object> savaInfo(@RequestBody SkuReductionTo skuReductionTo) {
        skuFullReductionService.saveSkuReduction(skuReductionTo);
        return Result.success();
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params) {
        PageUtils page = skuFullReductionService.queryPage(params);

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<SkuFullReductionEntity> info(@PathVariable Long id) {
        SkuFullReductionEntity skuFullReduction = skuFullReductionService.getById(id);

        return Result.success(skuFullReduction);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody SkuFullReductionEntity skuFullReduction) {
        skuFullReductionService.save(skuFullReduction);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody SkuFullReductionEntity skuFullReduction) {
        skuFullReductionService.updateById(skuFullReduction);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        skuFullReductionService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
