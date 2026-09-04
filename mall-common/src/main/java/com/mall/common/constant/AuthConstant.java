package com.mall.common.constant;

public class AuthConstant {
    public static final String SMS_CODE_CACHE_PREFIX = "sms:code:";
    /**
     * 图形验证码 Redis 前缀（key 为 uuid，5 分钟有效）
     */
    public static final String CAPTCHA_CODE_PREFIX = "captcha:code:";
    public static final String LOGIN_USER = "loginUser";
}
