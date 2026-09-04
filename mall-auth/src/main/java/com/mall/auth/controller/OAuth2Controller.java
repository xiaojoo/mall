package com.mall.auth.controller;

import com.alibaba.fastjson2.JSON;
import com.mall.auth.fegin.MemberFeignService;
import com.mall.auth.vo.SocialUser;
import com.mall.common.jwt.MemberJwtUtils;
import com.mall.common.utils.HttpUtils;
import com.mall.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@Slf4j
@Controller
@RequiredArgsConstructor
public class OAuth2Controller {
    private final MemberFeignService memberFeignService;
    private final MemberJwtUtils memberJwtUtils;

    // ------------------------------------------------------------------
    // 以下均来自 Nacos 配置（nacos-config/mall-auth.yaml 对应 data-id），只改 Nacos 即可。
    // ------------------------------------------------------------------
    @Value("${weibo.client-id}")          private String weiboClientId;
    @Value("${weibo.client-secret}")      private String weiboClientSecret;
    @Value("${weibo.redirect-uri}")       private String weiboRedirectUri;
    @Value("${weibo.api-host}")           private String weiboApiHost;
    @Value("${mall.oauth.success-redirect}") private String oauthSuccessRedirect;
    @Value("${mall.oauth.token-redirect}")   private String oauthTokenRedirect;
    @Value("${mall.oauth.login-redirect}")   private String oauthLoginRedirect;

    @GetMapping(value = "/oauth2.0/weibo/success")
    public String weiboLogin(@RequestParam("code") String code) throws Exception {
        // 1. 拿到授权令牌
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("client_id", weiboClientId);
        tokenMap.put("client_secret", weiboClientSecret);
        tokenMap.put("grant_type", "authorization_code");
        tokenMap.put("code", code);
        tokenMap.put("redirect_uri", weiboRedirectUri);
        HttpResponse response = HttpUtils.doPost(weiboApiHost, "/oauth2/access_token", "post", new HashMap<>(), tokenMap, new HashMap<>());
        if (response.getStatusLine().getStatusCode() == 200) {
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
            System.out.println(socialUser.getAccess_token());
            // 2. 根据令牌拿到用户信息
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "OAuth2 " + socialUser.getAccess_token());
            Map<String, String> params = new HashMap<>();
            params.put("uid", socialUser.getUid());
            // users/show 仅支持 GET（doPost 会发 POST 导致 405）
            HttpResponse userInfoResponse = HttpUtils.doGet(weiboApiHost, "/2/users/show.json", "get", headers, params);
            log.info("微博用户信息接口状态：{}", userInfoResponse.getStatusLine().getStatusCode());
            if (userInfoResponse.getStatusLine().getStatusCode() == 200) {
                // 此处的微博用户信息响应无需在此解析（会员创建/更新在 mall-member 内完成），
                // 直接调用会员登录；原 MemberResponseVo 解析字段不匹配会报 parseInt 异常
                Result<Object> oauthLogin = memberFeignService.oauthLogin(socialUser);
                log.info("会员登录结果：code={}, data={}", oauthLogin.getCode(), oauthLogin.getData());
                // Result 成功码为 200（原代码 ==0 恒为 false，导致登录永远失败）
                if (oauthLogin.getCode() == 200) {
                    log.info("登录成功：用户信息：{}", oauthLogin.getData());
                    // 签发会员 JWT 跳回前端（不再用明文 memberId）
                    Long memberId = null;
                    if (oauthLogin.getData() instanceof Map) {
                        Object id = ((Map<?, ?>) oauthLogin.getData()).get("id");
                        if (id != null) {
                            memberId = Long.valueOf(id.toString());
                        }
                    }
                    String redirect = oauthSuccessRedirect;
                    if (memberId != null) {
                        String jwt = memberJwtUtils.generateToken(memberId);
                        redirect = oauthTokenRedirect + jwt;
                    }
                    return "redirect:" + redirect;
                }
            }
        }
        return "redirect:" + oauthLoginRedirect;
    }
}
