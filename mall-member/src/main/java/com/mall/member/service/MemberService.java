package com.mall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.member.entity.MemberEntity;
import com.mall.member.exception.PhoneException;
import com.mall.member.exception.UsernameException;
import com.mall.member.vo.MemberUserLoginVo;
import com.mall.member.vo.MemberUserRegisterVo;
import com.mall.member.vo.SocialUser;
import com.mall.member.vo.WeChatUserVo;

import java.util.Map;

/**
 * 会员
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:14:49
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void register(MemberUserRegisterVo vo);

    /**
     * 判断邮箱是否重复
     */
    void checkPhoneUnique(String phone) throws PhoneException;

    /**
     * 判断用户名是否重复
     */
    void checkUserNameUnique(String userName) throws UsernameException;

    /**
     * 会员登录
     */
    MemberEntity login(MemberUserLoginVo vo);

    /**
     * 社交用户的登录/微博登录
     */
    MemberEntity login(SocialUser socialUser) throws Exception;

    /**
     * 微信网页授权登录：按 openid 查找，不存在则自动注册
     */
    MemberEntity login(WeChatUserVo weChatUser);
}

