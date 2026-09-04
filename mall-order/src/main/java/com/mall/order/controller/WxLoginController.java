package com.mall.order.controller;

import com.mall.common.utils.Result;
import com.mall.order.feign.ThirdPartyFeignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * 微信登录公网回调（经 FRP 隧道到达 mall-order，透传给 third-party 处理）
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class WxLoginController {

    private final ThirdPartyFeignService thirdPartyFeignService;

    /**
     * 微信登录成功后跳回的前端地址
     */
    @Value("${wx.loginRedirect:http://localhost:5173}")
    private String loginRedirect;

    @GetMapping("/wx/portal/public/callBack")
    public RedirectView callBack(@RequestParam String code) {
        RedirectView redirectView = new RedirectView();
        try {
            Result<Long> result = thirdPartyFeignService.wechatLoginByCode(code);
            log.info("微信登录结果：code={}, data={}", result.getCode(), result.getData());
            if (result.getCode() == 200 && result.getData() != null) {
                // 携带会员ID跳回前端，前端写入登录态（token 即会员ID）
                redirectView.setUrl(loginRedirect + "/?token=" + result.getData());
                return redirectView;
            }
        } catch (Exception e) {
            log.error("微信登录回调处理失败", e);
        }
        redirectView.setUrl(loginRedirect + "/#/login");
        return redirectView;
    }

    /**
     * 微信公众号服务器配置校验（GET）：透传 third-party 验签并返回 echostr
     */
    @GetMapping("/wx/portal/public")
    public String authGet(@RequestParam("signature") String signature,
                          @RequestParam("timestamp") String timestamp,
                          @RequestParam("nonce") String nonce,
                          @RequestParam("echostr") String echostr) {
        return thirdPartyFeignService.wxAuthGet(signature, timestamp, nonce, echostr);
    }

    /**
     * 微信公众号消息/事件推送（POST）：透传 third-party 处理
     */
    @PostMapping(value = "/wx/portal/public", produces = "application/xml; charset=UTF-8")
    public String post(@RequestBody String body,
                       @RequestParam("signature") String signature,
                       @RequestParam("timestamp") String timestamp,
                       @RequestParam("nonce") String nonce,
                       @RequestParam("openid") String openid,
                       @RequestParam(name = "encrypt_type", required = false) String encType,
                       @RequestParam(name = "msg_signature", required = false) String msgSignature) {
        return thirdPartyFeignService.wxPost(body, signature, timestamp, nonce, openid, encType, msgSignature);
    }
}
