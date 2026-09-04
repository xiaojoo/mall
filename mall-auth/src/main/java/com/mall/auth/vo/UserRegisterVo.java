package com.mall.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户注册 VO
 */
@Data
@Schema(description = "用户注册 VO")
public class UserRegisterVo {
    @Schema(description = "用户名")
    @NotBlank(message="用户名不能为空")
    private String userName;

    @Schema(description = "密码")
    @NotBlank(message="密码不能为空")
    private String password;

    @Schema(description = "手机号")
    @NotBlank(message="手机号不能为空")
    private String phone;

    @Schema(description = "验证码")
    @NotBlank(message="验证码不能为空")
    private String code;

    @Schema(description = "图形验证码 key")
    private String captchaKey;

    @Schema(description = "图形验证码")
    private String captchaCode;
}
