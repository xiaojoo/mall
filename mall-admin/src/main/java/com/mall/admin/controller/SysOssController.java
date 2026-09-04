package com.mall.admin.controller;

import com.mall.admin.entity.SysOssEntity;
import com.mall.admin.service.SysOssService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import com.mall.common.dto.PageQueryDTO;

@RestController
@RequestMapping("/sys/oss")
@RequiredArgsConstructor
public class SysOssController {

    private final SysOssService sysOssService;

    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params) {
        PageUtils page = sysOssService.queryPage(params);
        return Result.success(page);
    }

    @GetMapping("/info/{id}")
    public Result<SysOssEntity> info(@PathVariable Long id) {
        SysOssEntity oss = sysOssService.getById(id);
        return Result.success(oss);
    }

    @PostMapping("/save")
    public Result<Void> save(@RequestBody SysOssEntity oss) {
        sysOssService.save(oss);
        return Result.success();
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        sysOssService.removeByIds(java.util.Arrays.asList(ids));
        return Result.success();
    }
}
