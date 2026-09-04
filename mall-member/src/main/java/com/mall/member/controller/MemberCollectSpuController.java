package com.mall.member.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mall.member.entity.MemberCollectSpuEntity;
import com.mall.member.service.MemberCollectSpuService;
import com.mall.member.vo.CollectSpuVo;
import com.mall.common.jwt.MemberJwtUtils;
import com.mall.common.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;


/**
 * 会员收藏的商品（C 端：JWT 取当前会员，收藏列表/状态/添加/删除）
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:14:49
 */
@RestController
@RequestMapping("api/member/membercollectspu")
@RequiredArgsConstructor
public class MemberCollectSpuController {

    private final MemberCollectSpuService memberCollectSpuService;

    private final MemberJwtUtils memberJwtUtils;

    /**
     * 当前会员收藏列表（聚合商品名称/主图/价格/分类）
     */
    @GetMapping("/list")
    public Result<List<CollectSpuVo>> list(HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        if (memberId == null) {
            return Result.fail("未登录");
        }
        return Result.success(memberCollectSpuService.listCollectByMember(memberId));
    }

    /**
     * 当前会员收藏数量（Header 角标）
     */
    @GetMapping("/count")
    public Result<Long> count(HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        if (memberId == null) {
            return Result.success(0L);
        }
        return Result.success(memberCollectSpuService.countByMember(memberId));
    }

    /**
     * 当前会员是否已收藏该 SPU（详情页收藏按钮状态）
     */
    @GetMapping("/status/{spuId}")
    public Result<Boolean> status(@PathVariable Long spuId, HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        if (memberId == null) {
            return Result.success(false);
        }
        return Result.success(memberCollectSpuService.isCollected(memberId, spuId));
    }

    /**
     * 添加收藏（幂等；memberId 以 JWT 为准）
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody MemberCollectSpuEntity memberCollectSpu, HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        if (memberId == null) {
            return Result.fail("未登录");
        }
        if (memberCollectSpu.getSpuId() == null) {
            return Result.fail("缺少 spuId");
        }
        memberCollectSpuService.saveCollect(memberId, memberCollectSpu.getSpuId(),
                memberCollectSpu.getSpuName(), memberCollectSpu.getSpuImg(),
                memberCollectSpu.getSkuParams());
        return Result.success();
    }

    /**
     * 取消收藏（仅删除当前会员的记录，防止越权删他人数据）
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids, HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        if (memberId == null) {
            return Result.fail("未登录");
        }
        if (ids != null && ids.length > 0) {
            memberCollectSpuService.removeCollect(memberId, Arrays.asList(ids));
        }
        return Result.success();
    }

    /**
     * 按 spuId 取消收藏（详情页收藏按钮反选）
     */
    @PostMapping("/deleteBySpu")
    public Result<Void> deleteBySpu(@RequestBody Map<String, Long> body, HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        if (memberId == null) {
            return Result.fail("未登录");
        }
        Long spuId = body == null ? null : body.get("spuId");
        if (spuId == null) {
            return Result.fail("缺少 spuId");
        }
        memberCollectSpuService.removeCollectBySpu(memberId, spuId);
        return Result.success();
    }

}
