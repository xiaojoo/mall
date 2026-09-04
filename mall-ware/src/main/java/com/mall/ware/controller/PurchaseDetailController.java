package com.mall.ware.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mall.ware.entity.PurchaseDetailEntity;
import com.mall.ware.service.PurchaseDetailService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import com.mall.ware.dto.PurchaseDetailQueryDto;



/**
 * 
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:50:20
 */
@RestController
@RequestMapping("ware/purchasedetail")
@RequiredArgsConstructor
public class PurchaseDetailController {

    private final PurchaseDetailService purchaseDetailService;

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(PurchaseDetailQueryDto query){
        PageUtils page = purchaseDetailService.queryPage(query.toMap());

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<PurchaseDetailEntity> info(@PathVariable Long id){
		PurchaseDetailEntity purchaseDetail = purchaseDetailService.getById(id);

        return Result.success(purchaseDetail);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody PurchaseDetailEntity purchaseDetail){
		purchaseDetailService.save(purchaseDetail);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody PurchaseDetailEntity purchaseDetail){
		purchaseDetailService.updateById(purchaseDetail);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids){
		purchaseDetailService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
