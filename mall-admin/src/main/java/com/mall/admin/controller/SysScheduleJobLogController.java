package com.mall.admin.controller;

import com.mall.admin.entity.SysScheduleJobLogEntity;
import com.mall.admin.service.SysScheduleJobLogService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/sys/scheduleLog")
@RequiredArgsConstructor
public class SysScheduleJobLogController {

    private final SysScheduleJobLogService sysScheduleJobLogService;

    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params) {
        PageUtils page = sysScheduleJobLogService.queryPage(params);
        return Result.success(page);
    }

    @GetMapping("/info/{logId}")
    public Result<SysScheduleJobLogEntity> info(@PathVariable Long logId) {
        SysScheduleJobLogEntity log = sysScheduleJobLogService.getById(logId);
        return Result.success(log);
    }
}
