package com.mall.auth.perm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.auth.perm.dao.SysLogDao;
import com.mall.auth.perm.entity.SysLogEntity;
import com.mall.auth.perm.service.SysLogService;
import com.mall.common.utils.PageUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志管理
 */
@RestController
@RequestMapping("/sys/log")
@RequiredArgsConstructor
@Tag(name = "操作日志", description = "操作日志查询接口")
public class SysLogController {

    private final SysLogService sysLogService;
    private final SysLogDao sysLogDao;

    @GetMapping("/list")
    @Operation(summary = "日志分页列表")
    public Map<String, Object> list(@RequestParam Map<String, Object> params) {
        String username = (String) params.get("username");
        IPage<SysLogEntity> page = sysLogService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        Long.parseLong(params.getOrDefault("page", "1").toString()),
                        Long.parseLong(params.getOrDefault("limit", "10").toString())
                ),
                // 按操作时间降序，最近的操作排在第一页；支持操作用户名模糊搜索
                new LambdaQueryWrapper<SysLogEntity>()
                        .like(StringUtils.isNotBlank(username), SysLogEntity::getUsername, username)
                        .orderByDesc(SysLogEntity::getCreateTime)
                        .orderByDesc(SysLogEntity::getId)
        );
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("page", new PageUtils(page));
        return result;
    }

    /**
     * 日志统计（首页报表）：登录用户数 / 请求方法数 / 失败情况 / 成功率 / IP 数等
     * 首页对所有登录用户可见，不做 @RequirePermission 限制
     */
    @GetMapping("/stats")
    @Operation(summary = "日志统计（首页报表）")
    public Map<String, Object> stats() {
        long total = sysLogDao.countAll();
        long success = sysLogDao.countSuccess();
        long fail = sysLogDao.countFail();

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("success", success);
        data.put("fail", fail);
        // 成功率（保留 1 位小数）
        data.put("successRate", total == 0 ? 0 : Math.round(success * 1000.0 / total) / 10.0);
        data.put("userCount", sysLogDao.countDistinctUsers());
        data.put("methodCount", sysLogDao.countDistinctMethods());
        data.put("ipCount", sysLogDao.countDistinctIps());
        data.put("todayTotal", sysLogDao.countToday());
        data.put("todayFail", sysLogDao.countTodayFail());
        data.put("topFailMethods", sysLogDao.topFailMethods(10));
        data.put("topUsers", sysLogDao.topUsers(10));
        data.put("trend", buildTrend());
        data.put("methodDist", sysLogDao.methodDist(10));
        data.put("hourDist", buildHourDist());
        // 最近 10 条日志
        data.put("recent", sysLogService.list(
                new LambdaQueryWrapper<SysLogEntity>()
                        .orderByDesc(SysLogEntity::getCreateTime)
                        .last("LIMIT 10")
        ));

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    /** 近 7 天趋势（缺失日期补 0） */
    private List<Map<String, Object>> buildTrend() {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        String from = today.minusDays(6).toString();
        Map<String, Map<String, Object>> byDay = new HashMap<>();
        for (Map<String, Object> row : sysLogDao.countByDay(from)) {
            byDay.put(String.valueOf(row.get("day")), row);
        }
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate d = today.minusDays(i);
            Map<String, Object> row = byDay.get(d.toString());
            Map<String, Object> item = new HashMap<>();
            item.put("day", d.toString());
            item.put("total", row == null ? 0 : Long.parseLong(row.get("total").toString()));
            item.put("success", row == null ? 0 : Long.parseLong(row.get("success").toString()));
            result.add(item);
        }
        return result;
    }

    /** 今日 24 小时分布（缺小时补 0） */
    private List<Map<String, Object>> buildHourDist() {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        Map<Integer, Long> byHour = new HashMap<>();
        for (Map<String, Object> row : sysLogDao.countByHourToday()) {
            byHour.put(Integer.parseInt(row.get("hour").toString()),
                    Long.parseLong(row.get("cnt").toString()));
        }
        for (int h = 0; h < 24; h++) {
            Map<String, Object> item = new HashMap<>();
            item.put("hour", h);
            item.put("cnt", byHour.getOrDefault(h, 0L));
            result.add(item);
        }
        return result;
    }
}
