package com.mall.auth.app.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录表单
 *
 * @author mall
 */
@Data
@Schema(description = "登录表单")
public class LoginForm {
    @Schema(description = "手机号")
    @NotBlank(message="手机号不能为空")
    private String mobile;

    @Schema(description = "密码")
    @NotBlank(message="密码不能为空")
    private String password;

    @Schema(description = "图形验证码 key")
    private String captchaKey;

    @Schema(description = "图形验证码")
    private String captchaCode;
}
