package com.mall.member.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.mall.member.entity.MemberReceiveAddressEntity;
import com.mall.member.service.MemberReceiveAddressService;
import com.mall.common.jwt.MemberJwtUtils;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.mall.common.dto.PageQueryDTO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;


/**
 * 会员收货地址
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:14:49
 */
@RestController
@RequestMapping("api/member/memberreceiveaddress")
@RequiredArgsConstructor
public class MemberReceiveAddressController {

    private final MemberReceiveAddressService memberReceiveAddressService;

    private final MemberJwtUtils memberJwtUtils;

    /**
     * order模块远程调用，根据会员id查询会员的所有地址
     */
    @GetMapping(value = "/{memberId}/address")
    public List<MemberReceiveAddressEntity> getAddress(@PathVariable Long memberId) {

        List<MemberReceiveAddressEntity> addressList = memberReceiveAddressService.getAddress(memberId);

        return addressList;
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params) {
        PageUtils page = memberReceiveAddressService.queryPage(params);

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<MemberReceiveAddressEntity> info(@PathVariable Long id) {
        MemberReceiveAddressEntity memberReceiveAddress = memberReceiveAddressService.getById(id);

        return Result.success(memberReceiveAddress);
    }

    /**
     * 保存（memberId 以 JWT 为准，防止伪造归属）
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody MemberReceiveAddressEntity memberReceiveAddress, HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        if (memberId == null) {
            return Result.fail("未登录");
        }
        memberReceiveAddress.setMemberId(memberId);
        memberReceiveAddressService.save(memberReceiveAddress);

        return Result.success();
    }

    /**
     * 修改（memberId 以 JWT 为准）
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody MemberReceiveAddressEntity memberReceiveAddress, HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        if (memberId == null) {
            return Result.fail("未登录");
        }
        memberReceiveAddress.setMemberId(memberId);
        // 设为默认地址时，先清除该会员其他地址的默认标记，避免出现多个默认
        if (memberReceiveAddress.getDefaultStatus() != null
                && memberReceiveAddress.getDefaultStatus() == 1
                && memberReceiveAddress.getMemberId() != null
                && memberReceiveAddress.getId() != null) {
            memberReceiveAddressService.update(new LambdaUpdateWrapper<MemberReceiveAddressEntity>()
                    .eq(MemberReceiveAddressEntity::getMemberId, memberReceiveAddress.getMemberId())
                    .ne(MemberReceiveAddressEntity::getId, memberReceiveAddress.getId())
                    .set(MemberReceiveAddressEntity::getDefaultStatus, 0));
        }
        memberReceiveAddressService.updateById(memberReceiveAddress);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        memberReceiveAddressService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
