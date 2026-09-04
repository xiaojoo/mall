package com.mall.weapp.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 微信小程序登录请求实体
 * <p>用于接收小程序端传来的登录code</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@Data
public class WeappLoginEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 微信小程序登录code，用于换取openid和session_key
     */
    private String code;

    /**
     * 微信用户昵称（可选）
     */
    private String nickname;

    /**
     * 微信用户头像URL（可选）
     */
    private String avatarUrl;
}
