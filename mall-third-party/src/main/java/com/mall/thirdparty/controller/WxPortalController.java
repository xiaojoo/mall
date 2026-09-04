package com.mall.thirdparty.controller;

import com.mall.common.jwt.MemberJwtUtils;
import com.mall.thirdparty.service.WxMsgService;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.result.WxMpQrCodeTicket;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;

@Slf4j
@RestController
@RequestMapping("wx/portal/public")
public class WxPortalController {

    private final WxMpService wxService;
    private final WxMpMessageRouter messageRouter;
    private final WxMsgService wxMsgService;
    private final MemberJwtUtils memberJwtUtils;

    public WxPortalController(WxMpService wxService, WxMpMessageRouter messageRouter, WxMsgService wxMsgService, MemberJwtUtils memberJwtUtils) {
        this.wxService = wxService;
        this.messageRouter = messageRouter;
        this.wxMsgService = wxMsgService;
        this.memberJwtUtils = memberJwtUtils;
    }

    /**
     * 微信 OAuth 回调的对外基础地址（公众号服务器必须能访问，如 FRP 公网地址）
     */
    @Value("${wx.mp.callback:}")
    private String callback;

    // 微信网页授权基址（来自 Nacos mall-third-party 配置，只改 Nacos 即可）
    @Value("${wx.mp.authorize-url:https://open.weixin.qq.com/connect/oauth2/authorize}")
    private String authorizeUrl;

    /**
     * 登录成功后跳回的前端地址
     */
    @Value("${wx.mp.login-redirect:http://localhost:5173}")
    private String loginRedirect;

    /**
     * 生成二维码验证
     */
    @GetMapping("/test")
    public String getQrCode(@RequestParam Integer code) throws WxErrorException {
        WxMpQrCodeTicket wxMpQrCodeTicket = wxService.getQrcodeService().qrCodeCreateTmpTicket(code, 1000);
        return wxMpQrCodeTicket.getUrl();
    }

    /**
     * 扫码登录：生成带场景值(sessionId)的临时二维码
     */
    @GetMapping("/qrCode")
    public com.mall.common.utils.Result<String> qrCode(@RequestParam Integer sessionId) throws WxErrorException {
        WxMpQrCodeTicket ticket = wxService.getQrcodeService().qrCodeCreateTmpTicket(sessionId, 300);
        return com.mall.common.utils.Result.success(ticket.getUrl());
    }

    /**
     * 扫码登录：前端轮询登录结果（返回会员 JWT，未登录返回 null）
     */
    @GetMapping("/loginStatus")
    public com.mall.common.utils.Result<String> loginStatus(@RequestParam Integer sessionId) {
        Long memberId = wxMsgService.getLoginResult(sessionId);
        if (memberId == null) {
            return com.mall.common.utils.Result.fail("未登录");
        }
        // 签发会员 JWT（不再返回明文 memberId）
        return com.mall.common.utils.Result.success(memberJwtUtils.generateToken(memberId));
    }

    @GetMapping(produces = "text/plain;charset=utf-8")
    public String authGet(@RequestParam(name = "signature", required = false) String signature,
                          @RequestParam(name = "timestamp", required = false) String timestamp,
                          @RequestParam(name = "nonce", required = false) String nonce,
                          @RequestParam(name = "echostr", required = false) String echostr) {

        log.info("\n接收到来自微信服务器的认证消息：[{}, {}, {}, {}]", signature,
                timestamp, nonce, echostr);
        if (StringUtils.isAnyBlank(signature, timestamp, nonce, echostr)) {
            throw new IllegalArgumentException("请求参数非法，请核实!");
        }


        if (wxService.checkSignature(timestamp, nonce, signature)) {
            return echostr;
        }

        return "非法请求";
    }

    @GetMapping("/callBack")
    public RedirectView callBack(@RequestParam String code) {
        Long memberId = null;
        try {
            WxOAuth2AccessToken accessToken = wxService.getOAuth2Service().getAccessToken(code);
            WxOAuth2UserInfo userInfo = wxService.getOAuth2Service().getUserInfo(accessToken, "zh_CN");
            log.info("微信授权用户：{}", userInfo.getOpenid());
            // 创建/查询会员并登录
            memberId = wxMsgService.authorize(userInfo);
        } catch (Exception e) {
            log.error("callBack error", e);
        }
        RedirectView redirectView = new RedirectView();
        if (memberId != null) {
            // 签发会员 JWT 跳回前端（不再用明文 memberId，避免 URL 泄露/伪造）
            redirectView.setUrl(loginRedirect + "/?token=" + memberJwtUtils.generateToken(memberId));
        } else {
            redirectView.setUrl(loginRedirect + "/#/login");
        }
        return redirectView;
    }

    /**
     * 生成前端「微信登录」按钮的授权链接
     */
    @GetMapping("/loginUrl")
    public String loginUrl() {
        String appId = wxService.getWxMpConfigStorage().getAppId();
        String redirectUri = URLEncoder.encode(callback + "/wx/portal/public/callBack", java.nio.charset.StandardCharsets.UTF_8);
        return authorizeUrl + "?appid=" + appId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
    }

    /**
     * 内部登录接口（mall-order 公网回调透传用）：code 换会员ID
     */
    @GetMapping("/loginByCode")
    public com.mall.common.utils.Result<Long> loginByCode(@RequestParam String code) {
        Long memberId = null;
        try {
            WxOAuth2AccessToken accessToken = wxService.getOAuth2Service().getAccessToken(code);
            WxOAuth2UserInfo userInfo = wxService.getOAuth2Service().getUserInfo(accessToken, "zh_CN");
            memberId = wxMsgService.authorize(userInfo);
        } catch (Exception e) {
            log.error("loginByCode error", e);
        }
        if (memberId != null) {
            return com.mall.common.utils.Result.success(memberId);
        }
        return com.mall.common.utils.Result.fail("微信登录失败");
    }

    @PostMapping(produces = "application/xml; charset=UTF-8")
    public String post(@RequestBody String requestBody,
                       @RequestParam("signature") String signature,
                       @RequestParam("timestamp") String timestamp,
                       @RequestParam("nonce") String nonce,
                       @RequestParam("openid") String openid,
                       @RequestParam(name = "encrypt_type", required = false) String encType,
                       @RequestParam(name = "msg_signature", required = false) String msgSignature) {
        log.info("\n接收微信请求：[openid=[{}], [signature=[{}], encType=[{}], msgSignature=[{}],"
                        + " timestamp=[{}], nonce=[{}], requestBody=[\n{}\n] ",
                openid, signature, encType, msgSignature, timestamp, nonce, requestBody);

        if (!wxService.checkSignature(timestamp, nonce, signature)) {
            throw new IllegalArgumentException("非法请求，可能属于伪造的请求！");
        }

        String out = null;
        if (encType == null) {
            // 明文传输的消息
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromXml(requestBody);
            WxMpXmlOutMessage outMessage = this.route(inMessage);
            if (outMessage == null) {
                return "";
            }

            out = outMessage.toXml();
        } else if ("aes".equalsIgnoreCase(encType)) {
            // aes加密的消息
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromEncryptedXml(requestBody, wxService.getWxMpConfigStorage(),
                    timestamp, nonce, msgSignature);
            log.debug("\n消息解密后内容为：\n{} ", inMessage.toString());
            WxMpXmlOutMessage outMessage = this.route(inMessage);
            if (outMessage == null) {
                return "";
            }

            out = outMessage.toEncryptedXml(wxService.getWxMpConfigStorage());
        }

        log.debug("\n组装回复信息：{}", out);
        return out;
    }

    private WxMpXmlOutMessage route(WxMpXmlMessage message) {
        try {
            return this.messageRouter.route(message);
        } catch (Exception e) {
            log.error("路由消息时出现异常！", e);
        }

        return null;
    }
}