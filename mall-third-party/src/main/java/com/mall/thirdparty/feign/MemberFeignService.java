package com.mall.thirdparty.feign;

import com.mall.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 远程调用 mall-member：微信登录创建/查询会员
 */
@FeignClient("mall-member")
public interface MemberFeignService {

    /**
     * 微信网页授权登录（openid → 会员）
     */
    @PostMapping(value = "/api/member/member/oauth2/wechat/login")
    Result<Object> wechatLogin(@RequestBody Map<String, Object> weChatUser);
}
