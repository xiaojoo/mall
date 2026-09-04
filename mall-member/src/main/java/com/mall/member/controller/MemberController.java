package com.mall.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.mall.common.exception.BizCodeEnum;
import com.mall.member.exception.PhoneException;
import com.mall.member.exception.UsernameException;
import com.mall.member.feign.CouponFeignService;
import com.mall.member.vo.MemberUserLoginVo;
import com.mall.member.vo.MemberUserRegisterVo;
import com.mall.member.vo.SocialUser;
import com.mall.member.vo.WeChatUserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.mall.member.entity.MemberEntity;
import com.mall.member.service.MemberService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import com.mall.common.dto.PageQueryDTO;


/**
 * 会员
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:14:49
 */
@RestController
@RequestMapping("api/member/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 测试Feign远程
     */

    private final CouponFeignService couponFeignService;

    @GetMapping("/test")
    public Result test1() {
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("张三");
        Result<?> test = couponFeignService.test();
        return Result.success(memberEntity).putExtra("coupon", (Object) test.getData());
    }

    /**
     * 微博登录
     */
    @PostMapping(value = "/oauth2/login")
    public Result<Object> oauthLogin(@RequestBody SocialUser socialUser) throws Exception {

        MemberEntity memberEntity = memberService.login(socialUser);

        if (memberEntity != null) {
            return Result.success().setData(memberEntity);
        } else {
            return Result.fail(BizCodeEnum.LOGINACCT_PASSWORD_EXCEPTION.getCode(),BizCodeEnum.LOGINACCT_PASSWORD_EXCEPTION.getMsg());
        }
    }

    /**
     * 微信网页授权登录
     */
    @PostMapping(value = "/oauth2/wechat/login")
    public Result<Object> wechatLogin(@RequestBody WeChatUserVo weChatUser) {

        MemberEntity memberEntity = memberService.login(weChatUser);

        if (memberEntity != null) {
            return Result.success().setData(memberEntity);
        } else {
            return Result.fail(BizCodeEnum.LOGINACCT_PASSWORD_EXCEPTION.getCode(), BizCodeEnum.LOGINACCT_PASSWORD_EXCEPTION.getMsg());
        }
    }

    /**
     * 会员登录
     */
    @PostMapping(value = "/login")
    public Result<Object> login(@RequestBody MemberUserLoginVo vo) {

        MemberEntity memberEntity = memberService.login(vo);

        if (memberEntity != null) {
            return Result.success().setData(memberEntity);
        } else {
            return Result.fail(BizCodeEnum.LOGINACCT_PASSWORD_EXCEPTION.getCode(), BizCodeEnum.LOGINACCT_PASSWORD_EXCEPTION.getMsg());
        }
    }

    /**
     * 会员注册
     */
    @PostMapping(value = "/register")
    public Result<Void> register(@RequestBody MemberUserRegisterVo vo) {

        try {
            memberService.register(vo);
        } catch (PhoneException e) {
            return Result.fail(BizCodeEnum.PHONE_EXIST_EXCEPTION.getCode(), BizCodeEnum.PHONE_EXIST_EXCEPTION.getMsg());
        } catch (UsernameException e) {
            return Result.fail(BizCodeEnum.USER_EXIST_EXCEPTION.getCode(), BizCodeEnum.USER_EXIST_EXCEPTION.getMsg());
        }

        return Result.success();
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params) {
        PageUtils page = memberService.queryPage(params);

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<MemberEntity> info(@PathVariable Long id) {
        MemberEntity member = memberService.getById(id);

        return Result.success(member);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody MemberEntity member) {
        memberService.save(member);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody MemberEntity member) {
        memberService.updateById(member);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        memberService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
