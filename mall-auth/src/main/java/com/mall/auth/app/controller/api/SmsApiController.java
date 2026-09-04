package com.mall.auth.app.controller.api;

import com.mall.auth.fegin.ThirdPartFeignService;
import com.mall.common.constant.AuthConstant;
import com.mall.common.exception.BizCodeEnum;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 短信验证码 API 接口（前后端分离）
 * <p>
 * 替代原模板引擎页面接口：/sms/sendCode（LoginController 页面版）。
 * 网关路由 /api/sms/** → mall-auth。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsApiController {

    private final ThirdPartFeignService thirdPartFeignService;

    private final StringRedisTemplate redisTemplate;

    /**
     * 发送短信验证码。
     * <ul>
     *     <li>接口防刷：同一手机号 60s 内只能发送一次</li>
     *     <li>验证码缓存 5 分钟，注册时校验并删除（令牌机制）</li>
     * </ul>
     *
     * @param phone 手机号
     */
    @GetMapping("/sendCode")
    public Result<Void> sendCode(@RequestParam("phone") String phone) {
        // 1、接口防刷
        String redisCode = redisTemplate.opsForValue().get(AuthConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (StringUtils.isNotEmpty(redisCode)) {
            long lastSendTime = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - lastSendTime < 60000) {
                return Result.fail(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }
        // 2、生成 6 位验证码并写入缓存
        int code = (int) ((Math.random() * 9 + 1) * 100000);
        String codeNum = String.valueOf(code);
        log.debug("手机号 {} 验证码: {}", phone, codeNum);
        redisTemplate.opsForValue().set(AuthConstant.SMS_CODE_CACHE_PREFIX + phone,
                codeNum + "_" + System.currentTimeMillis(), 5, TimeUnit.MINUTES);
        // 3、调用短信服务发送
        thirdPartFeignService.sendCode(phone, codeNum);
        return Result.success();
    }
}
