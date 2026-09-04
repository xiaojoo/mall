package com.mall.weapp.feign;

import com.mall.common.utils.Result;
import com.mall.weapp.feign.fallback.MemberFeignServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 会员服务远程调用接口
 * <p>通过OpenFeign调用mall-member服务的会员相关接口</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@FeignClient(value = "mall-member", fallback = MemberFeignServiceFallback.class)
public interface MemberFeignService {

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    @GetMapping("api/member/member/info")
    Result<Object> info();

    /**
     * 小程序登录，使用code换取openid和token
     *
     * @param params 登录参数（包含code等）
     * @return 登录结果（token等）
     */
    @PostMapping("api/member/member/login")
    Result<Object> login(@RequestBody Map<String, Object> params);
}
