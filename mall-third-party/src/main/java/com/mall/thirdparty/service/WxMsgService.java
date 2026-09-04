package com.mall.thirdparty.service;

import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;

import java.io.UnsupportedEncodingException;

public interface WxMsgService {
    WxMpXmlOutMessage scan(me.chanjar.weixin.mp.api.WxMpService wxMpService, WxMpXmlMessage wxMpXmlMessage) throws UnsupportedEncodingException;

    /**
     * 微信用户授权后登录：创建/查询会员，返回会员ID（失败返回 null）
     */
    Long authorize(WxOAuth2UserInfo userInfo);

    /**
     * 查询扫码登录结果（前端轮询）
     */
    Long getLoginResult(Integer sessionId);
}
