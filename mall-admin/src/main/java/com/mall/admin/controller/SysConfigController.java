package com.mall.admin.controller;

import com.mall.admin.entity.SysConfigEntity;
import com.mall.admin.service.SysConfigService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import com.mall.admin.dto.SysConfigQueryDto;

@RestController
@RequestMapping("/sys/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @GetMapping("/list")
    public Result<PageUtils> list(SysConfigQueryDto query) {
        PageUtils page = sysConfigService.queryPage(query.toMap());
        return Result.success(page);
    }

    @GetMapping("/info/{id}")
    public Result<SysConfigEntity> info(@PathVariable Long id) {
        SysConfigEntity config = sysConfigService.getById(id);
        return Result.success(config);
    }

    @PostMapping("/save")
    public Result<Void> save(@RequestBody SysConfigEntity config) {
        sysConfigService.saveConfig(config);
        return Result.success();
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody SysConfigEntity config) {
        sysConfigService.updateConfig(config);
        return Result.success();
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        sysConfigService.deleteBatch(ids);
        return Result.success();
    }
}
