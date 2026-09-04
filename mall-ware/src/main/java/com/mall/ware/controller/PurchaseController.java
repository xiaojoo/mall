package com.mall.ware.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mall.ware.vo.MergeVo;
import com.mall.ware.vo.PurchaseDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.mall.ware.entity.PurchaseEntity;
import com.mall.ware.service.PurchaseService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import java.util.Date;
import com.mall.ware.dto.PurchaseQueryDto;


/**
 * 采购信息
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:50:20
 */
@RestController
@RequestMapping("ware/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping("/done")
    public Result<Void> finish(@RequestBody PurchaseDoneVo doneVo){

        purchaseService.done(doneVo);
        return Result.success();
    }

    /**
     * 领取采购单
     *
     * @return
     */
    @PostMapping("/received")
    public Result<Void> received(@RequestBody List<Long> ids) {

        purchaseService.received(ids);
        return Result.success();
    }

    @PostMapping("/merge")
    public Result<Void> merge(@RequestBody MergeVo mergeVo) {

        purchaseService.mergePurchase(mergeVo);
        return Result.success();
    }

    @GetMapping("/unreceive/list")
    public Result<Object> unreceivelist(PurchaseQueryDto query) {
        PageUtils page = purchaseService.queryPageUnreceivePurchase(query.toMap());

        return Result.success(page);
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(PurchaseQueryDto query) {
        PageUtils page = purchaseService.queryPage(query.toMap());

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<PurchaseEntity> info(@PathVariable Long id) {
        PurchaseEntity purchase = purchaseService.getById(id);

        return Result.success(purchase);
    }

    /**
     * 保存（创建/更新时间为空时后端补齐，避免前端空值落库）
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody PurchaseEntity purchase) {
        if (purchase.getCreateTime() == null) {
            purchase.setCreateTime(new Date());
        }
        purchase.setUpdateTime(new Date());
        purchaseService.save(purchase);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody PurchaseEntity purchase) {
        purchase.setUpdateTime(new Date());
        purchaseService.updateById(purchase);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        purchaseService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
