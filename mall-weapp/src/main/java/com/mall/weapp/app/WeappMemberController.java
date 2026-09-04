package com.mall.weapp.app;

import com.mall.common.utils.Result;
import com.mall.weapp.entity.WeappLoginEntity;
import com.mall.weapp.feign.MemberFeignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信小程序 - 会员模块控制器
 * <p>提供用户信息查询、小程序登录等接口，供小程序端调用</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@RestController
@RequestMapping("weapp/member")
@RequiredArgsConstructor
public class WeappMemberController {

    private final MemberFeignService memberFeignService;

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<Object> info() {
        return memberFeignService.info();
    }

    /**
     * 微信小程序登录
     * <p>使用微信返回的code换取openid和session_key，生成自有系统的token</p>
     *
     * @param loginVo 登录请求参数（包含code、昵称、头像等）
     * @return 登录结果（token、用户信息等）
     */
    @PostMapping("/login")
    public Result<Object> login(@RequestBody WeappLoginEntity loginVo) {
        Map<String, Object> params = new HashMap<>();
        params.put("code", loginVo.getCode());
        params.put("nickname", loginVo.getNickname());
        params.put("avatarUrl", loginVo.getAvatarUrl());
        return memberFeignService.login(params);
    }
}
