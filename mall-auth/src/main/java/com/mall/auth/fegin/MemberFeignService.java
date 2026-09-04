package com.mall.auth.fegin;

import com.mall.auth.vo.SocialUser;
import com.mall.auth.vo.UserLoginVo;
import com.mall.auth.vo.UserRegisterVo;
import com.mall.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("mall-member")
public interface MemberFeignService {

    @PostMapping(value = "/api/member/member/register")
    Result<Object> register(@RequestBody UserRegisterVo vo);

    @PostMapping(value = "/api/member/member/login")
    Result<Object> login(@RequestBody UserLoginVo vo);

    @PostMapping(value = "/api/member/member/oauth2/login")
    Result<Object> oauthLogin(@RequestBody SocialUser socialUser) throws Exception;
}
