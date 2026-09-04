package com.mall.thirdparty.controller;

import com.mall.common.utils.Result;
import com.mall.thirdparty.component.SmsComponent;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "sms")
@RequiredArgsConstructor
public class SmsSendController {

    private final SmsComponent smsComponent;

    /**
     * 提供给别的服务进行调用
     *
     * @param phone
     * @param code
     * @return
     */
    @GetMapping(value = "/sendCode")
    public Result<Void> sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code) {

        //发送验证码
        smsComponent.sendCode(phone, code);

        return Result.success();
    }
}