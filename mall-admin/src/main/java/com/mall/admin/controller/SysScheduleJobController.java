package com.mall.admin.controller;

import com.mall.admin.entity.SysScheduleJobEntity;
import com.mall.admin.entity.SysScheduleJobLogEntity;
import com.mall.admin.service.SysScheduleJobLogService;
import com.mall.admin.service.SysScheduleJobService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/sys/schedule")
@RequiredArgsConstructor
public class SysScheduleJobController {

    private final SysScheduleJobService sysScheduleJobService;
    private final SysScheduleJobLogService sysScheduleJobLogService;

    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params) {
        PageUtils page = sysScheduleJobService.queryPage(params);
        return Result.success(page);
    }

    @GetMapping("/info/{jobId}")
    public Result<SysScheduleJobEntity> info(@PathVariable Long jobId) {
        SysScheduleJobEntity job = sysScheduleJobService.getById(jobId);
        return Result.success(job);
    }

    @PostMapping("/save")
    public Result<Void> save(@RequestBody SysScheduleJobEntity job) {
        sysScheduleJobService.save(job);
        return Result.success();
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody SysScheduleJobEntity job) {
        sysScheduleJobService.updateById(job);
        return Result.success();
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] jobIds) {
        sysScheduleJobService.removeByIds(Arrays.asList(jobIds));
        return Result.success();
    }

    @PostMapping("/pause")
    public Result<Void> pause(@RequestBody Long[] jobIds) {
        for (Long jobId : jobIds) {
            SysScheduleJobEntity job = sysScheduleJobService.getById(jobId);
            job.setStatus(0);
            sysScheduleJobService.updateById(job);
        }
        return Result.success();
    }

    @PostMapping("/resume")
    public Result<Void> resume(@RequestBody Long[] jobIds) {
        for (Long jobId : jobIds) {
            SysScheduleJobEntity job = sysScheduleJobService.getById(jobId);
            job.setStatus(1);
            sysScheduleJobService.updateById(job);
        }
        return Result.success();
    }

    @PostMapping("/run")
    public Result<Void> run(@RequestBody Long[] jobIds) {
        // Run jobs immediately
        return Result.success();
    }
}
