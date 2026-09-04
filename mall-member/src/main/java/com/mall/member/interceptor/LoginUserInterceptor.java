package com.mall.member.interceptor;

import com.mall.common.constant.AuthConstant;
import com.mall.common.vo.MemberResponseVo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {
    public static ThreadLocal<MemberResponseVo> loginUser = new ThreadLocal<>();

    // 登录跳转前端地址（来自 Nacos mall-member 配置）
    @Value("${mall.web.login-url:http://example.com/#/login}")
    private String loginUrl;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        AntPathMatcher matcher = new AntPathMatcher();
        // 内部页面与前后端分离接口直接放行（登录态由各自体系处理）
        if (matcher.match("/member/**", uri) || matcher.match("/api/**", uri)) {
            return true;
        }

        MemberResponseVo attribute = (MemberResponseVo) request.getSession().getAttribute(AuthConstant.LOGIN_USER);
        if (attribute != null) {
            // 把登录后用户的信息放在ThreadLocal里面进行保存
            loginUser.set(attribute);
            return true;
        } else {
            // 没有登录就去登录
            request.getSession().setAttribute("msg", "请先进行登录");
            // 旧页面流遗留：跳转商城 SPA 登录页（原 auth.example.com/login.html 已随纯 API 化删除）
            response.sendRedirect(loginUrl);
            return false;
        }
    }
}
