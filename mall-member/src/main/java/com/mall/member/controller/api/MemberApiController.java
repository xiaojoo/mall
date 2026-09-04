package com.mall.member.controller.api;

import com.mall.common.jwt.MemberJwtUtils;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import com.mall.member.entity.MemberEntity;
import com.mall.member.entity.MemberReceiveAddressEntity;
import com.mall.member.service.MemberService;
import com.mall.member.service.MemberReceiveAddressService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 会员 API 接口 (前后端分离)
 *
 * @author sunxiaojie
 * @date 2024-08-01
 */
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    private final MemberReceiveAddressService memberReceiveAddressService;

    private final MemberJwtUtils memberJwtUtils;

    /**
     * 解析当前登录会员：优先 JWT（token header，mall-ui 登录后自动携带），
     * 兼容旧的 memberId 参数（迁移期过渡，Feign 内部调用不受影响）
     */
    private Long resolveMemberId(HttpServletRequest request, Long paramMemberId) {
        if (paramMemberId != null) {
            return paramMemberId;
        }
        return memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
    }

    /**
     * 获取会员信息
     */
    @GetMapping("/info")
    public Result<MemberEntity> info(@RequestParam(required = false) Long memberId, HttpServletRequest request) {
        Long id = resolveMemberId(request, memberId);
        if (id == null) {
            return Result.fail("未登录");
        }
        MemberEntity member = memberService.getById(id);
        Result<MemberEntity> result = Result.success(member);
        return result;
    }

    /**
     * 更新会员信息（memberId 以 JWT 为准，忽略请求体传入，防止伪造归属）
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody MemberEntity member, HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        if (memberId == null) {
            return Result.fail("未登录");
        }
        member.setId(memberId);
        memberService.updateById(member);
        return Result.success();
    }

    /**
     * 获取收货地址列表
     */
    @GetMapping("/address/list")
    public Result<List<MemberReceiveAddressEntity>> addressList(@RequestParam(required = false) Long memberId, HttpServletRequest request) {
        Long id = resolveMemberId(request, memberId);
        if (id == null) {
            return Result.fail("未登录");
        }
        List<MemberReceiveAddressEntity> list = memberReceiveAddressService.list(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MemberReceiveAddressEntity>()
            .eq(MemberReceiveAddressEntity::getMemberId, id));
        return Result.success(list);
    }

    /**
     * 获取默认收货地址
     */
    @GetMapping("/address/default")
    public Result<MemberReceiveAddressEntity> defaultAddress(@RequestParam(required = false) Long memberId, HttpServletRequest request) {
        Long id = resolveMemberId(request, memberId);
        if (id == null) {
            return Result.fail("未登录");
        }
        MemberReceiveAddressEntity address = memberReceiveAddressService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MemberReceiveAddressEntity>()
            .eq(MemberReceiveAddressEntity::getMemberId, id)
            .eq(MemberReceiveAddressEntity::getDefaultStatus, 1));
        return Result.success(address);
    }

    /**
     * 添加收货地址（memberId 以 JWT 为准，忽略请求体传入，防止伪造归属）
     */
    @PostMapping("/address")
    public Result<Void> addAddress(@RequestBody MemberReceiveAddressEntity address, HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        if (memberId == null) {
            return Result.fail("未登录");
        }
        address.setMemberId(memberId);
        memberReceiveAddressService.save(address);
        return Result.success();
    }

    /**
     * 更新收货地址（memberId 以 JWT 为准）
     */
    @PostMapping("/address/update")
    public Result<Void> updateAddress(@RequestBody MemberReceiveAddressEntity address, HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        if (memberId == null) {
            return Result.fail("未登录");
        }
        address.setMemberId(memberId);
        memberReceiveAddressService.updateById(address);
        return Result.success();
    }

    /**
     * 删除收货地址
     */
    @PostMapping("/address/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id) {
        memberReceiveAddressService.removeById(id);
        return Result.success();
    }

    /**
     * 设置默认收货地址
     */
    @PostMapping("/address/default/{id}")
    public Result<Void> setDefaultAddress(@PathVariable Long id) {
        // 先将该会员的其他地址设为非默认
        MemberReceiveAddressEntity address = memberReceiveAddressService.getById(id);
        if (address != null) {
            memberReceiveAddressService.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MemberReceiveAddressEntity>()
                .eq(MemberReceiveAddressEntity::getMemberId, address.getMemberId())
                .set(MemberReceiveAddressEntity::getDefaultStatus, 0));
            // 再设置当前地址为默认
            memberReceiveAddressService.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MemberReceiveAddressEntity>()
                .eq(MemberReceiveAddressEntity::getId, id)
                .set(MemberReceiveAddressEntity::getDefaultStatus, 1));
        }
        return Result.success();
    }
}
