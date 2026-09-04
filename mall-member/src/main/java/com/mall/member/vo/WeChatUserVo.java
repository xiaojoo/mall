package com.mall.member.vo;

import lombok.Data;

/**
 * 微信网页授权登录用户信息
 */
@Data
public class WeChatUserVo {

    /**
     * 微信 openid（作为会员 social_uid 唯一标识）
     */
    private String openid;

    /**
     * 微信昵称
     */
    private String nickname;

    /**
     * 头像地址
     */
    private String header;

    /**
     * 性别：0未知 1男 2女
     */
    private Integer gender;
}
