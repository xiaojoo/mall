package com.mall.auth.app.controller;

import com.mall.common.utils.Result;
import com.mall.common.validator.ValidatorUtils;
import com.mall.auth.app.entity.UserEntity;
import com.mall.auth.app.form.RegisterForm;
import com.mall.auth.app.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import java.util.Date;

/**
 * APP 注册接口
 *
 * @author mall
 */
@RestController
@RequestMapping("/app")
@Tag(name = "APP 注册接口")
@RequiredArgsConstructor
public class AppRegisterController {
    private final UserService userService;

    @PostMapping("register")
    public Result<Object> register(@RequestBody RegisterForm form) {
        // 表单校验
        ValidatorUtils.validateEntity(form);

        UserEntity user = new UserEntity();
        user.setMobile(form.getMobile());
        user.setUsername(form.getMobile());
        user.setPassword(DigestUtils.sha256Hex(form.getPassword()));
        user.setCreateTime(new Date());
        userService.save(user);

        return Result.success();
    }
}
