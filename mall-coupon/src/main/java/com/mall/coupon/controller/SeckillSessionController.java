package com.mall.coupon.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.mall.coupon.entity.SeckillSessionEntity;
import com.mall.coupon.service.SeckillSessionService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import com.mall.coupon.dto.SeckillSessionQueryDto;


/**
 * 秒杀活动场次
 *
 * @author sunxiaojie
 * @date 2024-08-01 13:52:57
 */
@RestController
@RequestMapping("coupon/seckillsession")
@RequiredArgsConstructor
public class SeckillSessionController {

    private final SeckillSessionService seckillSessionService;

    /**
     * seckill模块，远程查询最近三天需要参加秒杀商品的信息
     */
    @GetMapping(value = "/Lates3DaySession")
    public Result<Object> getLates3DaySession() {

        List<SeckillSessionEntity> seckillSessionEntities = seckillSessionService.getLates3DaySession();

        return Result.success().setData(seckillSessionEntities);
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(SeckillSessionQueryDto query) {
        PageUtils page = seckillSessionService.queryPage(query.toMap());

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<SeckillSessionEntity> info(@PathVariable Long id) {
        SeckillSessionEntity seckillSession = seckillSessionService.getById(id);

        return Result.success(seckillSession);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody SeckillSessionEntity seckillSession) {
        // 时间重叠校验：重叠则返回「秒杀场次时间重叠，请重新输入」
        seckillSessionService.validateNoOverlap(seckillSession, null);
        seckillSessionService.save(seckillSession);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody SeckillSessionEntity seckillSession) {
        // 时间重叠校验（排除自身）
        seckillSessionService.validateNoOverlap(seckillSession, seckillSession.getId());
        seckillSessionService.updateById(seckillSession);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        seckillSessionService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
