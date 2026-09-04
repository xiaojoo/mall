package com.mall.order.feign;

import com.mall.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 远程调用 mall-third-party：微信登录相关
 */
@FeignClient("mall-third-party")
public interface ThirdPartyFeignService {

    /**
     * 微信 OAuth code 换取会员ID（内部接口，公网回调由 mall-order 透传）
     * 注意：Feign 直连服务，路径为 third-party 实际控制器映射（不走网关 /api/thirdparty 前缀）
     */
    @GetMapping(value = "/wx/portal/public/loginByCode")
    Result<Long> wechatLoginByCode(@RequestParam("code") String code);

    /**
     * 微信公众号服务器配置校验（GET echostr）
     */
    @GetMapping(value = "/wx/portal/public")
    String wxAuthGet(@RequestParam("signature") String signature,
                     @RequestParam("timestamp") String timestamp,
                     @RequestParam("nonce") String nonce,
                     @RequestParam("echostr") String echostr);

    /**
     * 微信公众号消息/事件推送（POST XML）
     */
    @PostMapping(value = "/wx/portal/public", consumes = "application/xml", produces = "application/xml")
    String wxPost(@RequestBody String body,
                  @RequestParam("signature") String signature,
                  @RequestParam("timestamp") String timestamp,
                  @RequestParam("nonce") String nonce,
                  @RequestParam("openid") String openid,
                  @RequestParam(name = "encrypt_type", required = false) String encType,
                  @RequestParam(name = "msg_signature", required = false) String msgSignature);
}
