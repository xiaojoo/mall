package com.mall.weapp.feign.fallback;

import com.mall.common.utils.Result;
import com.mall.weapp.feign.MemberFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 会员服务Feign熔断降级
 * <p>当mall-member服务不可用时，返回错误信息避免级联故障</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@Slf4j
@Component
public class MemberFeignServiceFallback implements MemberFeignService {

    @Override
    public Result<Object> info() {
        log.warn("会员服务调用失败，获取用户信息为空");
        return Result.fail("会员服务暂时不可用，请稍后重试");
    }

    @Override
    public Result<Object> login(Map<String, Object> params) {
        log.warn("会员服务调用失败，登录失败");
        return Result.fail("会员服务暂时不可用，请稍后重试");
    }
}
