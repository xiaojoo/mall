package com.mall.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.admin.entity.SysUserEntity;
import com.mall.admin.entity.SysUserTokenEntity;
import com.mall.admin.service.SysUserTokenService;
import com.mall.admin.service.SysUserService;
import com.mall.admin.utils.AdminConstants;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SysLoginController {

    private final SysUserService sysUserService;
    private final SysUserTokenService sysUserTokenService;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @PostMapping("/sys/login")
    public Result<String> login(@RequestBody Map<String, String> form) {
        String username = form.get("username");
        String password = form.get("password");

        SysUserEntity user = sysUserService.queryByUserName(username);
        if (user == null || !PASSWORD_ENCODER.matches(password, user.getPassword())) {
            return Result.fail("账号或密码不正确");
        }
        if (user.getStatus() == 0) {
            return Result.fail("账号已被锁定,请联系管理员");
        }
        return sysUserTokenService.createToken(user.getUserId());
    }

    @PostMapping("/sys/logout")
    public Result<Void> logout(@RequestHeader(value = "token", required = false) String token) {
        if (token != null && !token.isEmpty()) {
            SysUserTokenEntity tokenEntity = sysUserTokenService.getOne(
                new LambdaQueryWrapper<SysUserTokenEntity>().eq(SysUserTokenEntity::getToken, token)
            );
            if (tokenEntity != null) {
                sysUserTokenService.removeById(tokenEntity.getUserId());
            }
        }
        return Result.success();
    }
}
