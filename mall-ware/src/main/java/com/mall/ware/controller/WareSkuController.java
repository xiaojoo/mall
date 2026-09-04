package com.mall.ware.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mall.common.exception.NoStockException;
import com.mall.common.vo.WareSkuStockVo;
import com.mall.ware.vo.LockStockResultVo;
import com.mall.ware.vo.SkuHasStockVo;
import com.mall.ware.vo.WareSkuLockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.mall.ware.entity.WareSkuEntity;
import com.mall.ware.service.WareSkuService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;

import static com.mall.common.exception.BizCodeEnum.NO_STOCK_EXCEPTION;
import lombok.RequiredArgsConstructor;
import com.mall.ware.dto.WareSkuQueryDto;


/**
 * 商品库存
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:50:20
 */
@RestController
@RequestMapping("ware/waresku")
@RequiredArgsConstructor
public class WareSkuController {

    private final WareSkuService wareSkuService;

    /**
     * 锁定库存
     * 库存解锁的场景
     * 1）、下订单成功，订单过期没有支付被系统自动取消或者被用户手动取消，都要解锁库存
     * 2）、下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚。之前锁定的库存就要自动解锁
     */
    @PostMapping(value = "/lock/order")
    public Result<Object> orderLockStock(@RequestBody WareSkuLockVo vo) {
        try {
            boolean lockStock = wareSkuService.orderLockStock(vo);
            return Result.success().setData(lockStock);
        } catch (NoStockException e) {
            return Result.fail(NO_STOCK_EXCEPTION.getCode(), NO_STOCK_EXCEPTION.getMsg());
        }
    }

    /**
     * order模块，查询sku是否有库存
     */
    @PostMapping("/stock")
    public Result<Object> getSkuStock(@RequestBody List<Long> skuIds) {
        List<SkuHasStockVo> vos = wareSkuService.getSkusHasStock(skuIds);
        return Result.success().setData(vos);
    }

    /**
     * 查询sku是否有库存
     */
    @PostMapping("/hasstock")
    public List<SkuHasStockVo> getSkuHasStock(@RequestBody List<Long> skuIds) {
        return wareSkuService.getSkusHasStock(skuIds);
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(WareSkuQueryDto query) {
        PageUtils page = wareSkuService.queryPage(query.toMap());

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<WareSkuEntity> info(@PathVariable Long id) {
        WareSkuEntity wareSku = wareSkuService.getById(id);

        return Result.success(wareSku);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody WareSkuEntity wareSku) {
        wareSkuService.save(wareSku);

        return Result.success();
    }

    /**
     * 设置 SKU 库存（发布商品/批量设置，SET 语义）
     */
    @PostMapping("/save-stock")
    public Result<Void> saveStock(@RequestBody List<WareSkuStockVo> vos) {
        wareSkuService.saveStock(vos);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody WareSkuEntity wareSku) {
        wareSkuService.updateById(wareSku);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        wareSkuService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
