package com.mall.weapp.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 微信小程序登录返回实体
 * <p>包含登录成功后的token和用户信息</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@Data
public class WeappLoginResultEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 登录token
     */
    private String token;

    /**
     * 用户ID
     */
    private Long memberId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像
     */
    private String avatar;
}
