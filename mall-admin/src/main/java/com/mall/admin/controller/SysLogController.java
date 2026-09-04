package com.mall.admin.controller;

import com.mall.admin.service.SysLogService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import com.mall.admin.dto.SysLogQueryDto;

@RestController
@RequestMapping("/sys/log")
@RequiredArgsConstructor
public class SysLogController {

    private final SysLogService sysLogService;

    @GetMapping("/list")
    public Result<PageUtils> list(SysLogQueryDto query) {
        PageUtils page = sysLogService.queryPage(query.toMap());
        return Result.success(page);
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        sysLogService.removeByIds(java.util.Arrays.asList(ids));
        return Result.success();
    }
}
