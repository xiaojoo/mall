package com.mall.ware.controller;

import java.util.Arrays;
import java.util.Map;

import com.mall.ware.vo.FareVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.mall.ware.entity.WareInfoEntity;
import com.mall.ware.service.WareInfoService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import com.mall.ware.dto.WareInfoQueryDto;


/**
 * 仓库信息
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:50:20
 */
@RestController
@RequestMapping("ware/wareinfo")
@RequiredArgsConstructor
public class WareInfoController {

    private final WareInfoService wareInfoService;

    /**
     * order模块，获取运费信息
     */
    @GetMapping(value = "/fare")
    public Result<Object> getFare(@RequestParam("addrId") Long addrId) {
        FareVo fare = wareInfoService.getFare(addrId);
        return Result.success().setData(fare);
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(WareInfoQueryDto query) {
        PageUtils page = wareInfoService.queryPage(query.toMap());

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<WareInfoEntity> info(@PathVariable Long id) {
        WareInfoEntity wareInfo = wareInfoService.getById(id);

        return Result.success(wareInfo);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody WareInfoEntity wareInfo) {
        wareInfoService.save(wareInfo);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody WareInfoEntity wareInfo) {
        wareInfoService.updateById(wareInfo);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        wareInfoService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
