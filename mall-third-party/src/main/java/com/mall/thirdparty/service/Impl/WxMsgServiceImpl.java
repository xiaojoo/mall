package com.mall.thirdparty.service.Impl;

import com.mall.common.utils.Result;
import com.mall.thirdparty.adapter.TextBuilder;
import com.mall.thirdparty.feign.MemberFeignService;
import com.mall.thirdparty.service.WxMsgService;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class WxMsgServiceImpl implements WxMsgService {
    private static final ConcurrentHashMap<String, Integer> WAIT_AUTHORIZE_MAP = new ConcurrentHashMap<>();
    /**
     * 扫码登录会话结果：sessionId → 会员ID（前端轮询用）
     */
    private static final ConcurrentHashMap<Integer, Long> LOGIN_RESULT_MAP = new ConcurrentHashMap<>();
    // 微信网页授权基址（来自 Nacos mall-third-party 配置，只改 Nacos 即可）
    @Value("${wx.mp.authorize-url:https://open.weixin.qq.com/connect/oauth2/authorize}")
    private String authorizeUrl;

    @Value("${wx.mp.callback}")
    private String callback;

    private final MemberFeignService memberFeignService;

    public WxMsgServiceImpl(MemberFeignService memberFeignService) {
        this.memberFeignService = memberFeignService;
    }

    @Override
    public WxMpXmlOutMessage scan(WxMpService wxMpService, WxMpXmlMessage wxMpXmlMessage) throws UnsupportedEncodingException {
        String openId = wxMpXmlMessage.getFromUser();
        Integer loginCode = Integer.parseInt(getEventKey(wxMpXmlMessage));
        // 推送链接让用户授权
        WAIT_AUTHORIZE_MAP.put(openId, loginCode);
        String format = authorizeUrl + "?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
        String skipUrl = String.format(format, wxMpService.getWxMpConfigStorage().getAppId(), URLEncoder
                .encode(callback + "/wx/portal/public/callBack", "utf-8"));
        return TextBuilder.build("请点击链接授权：<a href=\"" + skipUrl + "\">登录</a>", wxMpXmlMessage);
    }

    /**
     * 微信用户授权后登录：调用 mall-member 创建/查询会员（social_uid=openid）
     *
     * @return 会员ID，失败返回 null
     */
    @Override
    public Long authorize(WxOAuth2UserInfo userInfo) {
        String openId = userInfo.getOpenid();
        try {
            Map<String, Object> weChatUser = new HashMap<>();
            weChatUser.put("openid", openId);
            weChatUser.put("nickname", userInfo.getNickname());
            weChatUser.put("header", userInfo.getHeadImgUrl());
            // 微信性别：1男 2女 0未知
            weChatUser.put("gender", userInfo.getSex() != null ? userInfo.getSex() : 0);
            Result<Object> result = memberFeignService.wechatLogin(weChatUser);
            log.info("微信登录结果：code={}, data={}", result.getCode(), result.getData());
            if (result.getCode() == 200 && result.getData() != null) {
                if (result.getData() instanceof Map) {
                    Object id = ((Map<?, ?>) result.getData()).get("id");
                    if (id != null) {
                        Long memberId = Long.valueOf(id.toString());
                        // 记录扫码登录会话结果，供前端轮询
                        Integer sessionId = WAIT_AUTHORIZE_MAP.get(openId);
                        if (sessionId != null) {
                            LOGIN_RESULT_MAP.put(sessionId, memberId);
                            WAIT_AUTHORIZE_MAP.remove(openId);
                        }
                        return memberId;
                    }
                }
            }
        } catch (Exception e) {
            log.error("微信登录失败", e);
        }
        return null;
    }

    private String getEventKey(WxMpXmlMessage wxMpXmlMessage) {
        //扫码关注的渠道事件有前缀，需要去除
        return wxMpXmlMessage.getEventKey().replace("qrscene_", "");
    }

    /**
     * 查询扫码登录结果（前端轮询）
     */
    public Long getLoginResult(Integer sessionId) {
        return LOGIN_RESULT_MAP.get(sessionId);
    }
}
