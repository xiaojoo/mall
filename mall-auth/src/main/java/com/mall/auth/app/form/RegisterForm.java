package com.mall.auth.app.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 注册表单
 *
 * @author mall
 */
@Data
@Schema(description = "注册表单")
public class RegisterForm {
    @Schema(description = "手机号")
    @NotBlank(message="手机号不能为空")
    private String mobile;

    @Schema(description = "密码")
    @NotBlank(message="密码不能为空")
    private String password;
}
