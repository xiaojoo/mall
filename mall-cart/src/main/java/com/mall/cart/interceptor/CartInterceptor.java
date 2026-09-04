package com.mall.cart.interceptor;

import com.mall.cart.to.UserInfoTo;
import com.mall.common.jwt.MemberJwtUtils;
import com.mall.common.vo.MemberResponseVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.UUID;

import static com.mall.common.constant.AuthConstant.LOGIN_USER;
import static com.mall.common.constant.CartConstant.TEMP_USER_COOKIE_NAME;
import static com.mall.common.constant.CartConstant.TEMP_USER_COOKIE_TIMEOUT;

@RequiredArgsConstructor
public class CartInterceptor implements HandlerInterceptor {
    public static ThreadLocal<UserInfoTo> toThreadLocal = new ThreadLocal<>();

    private final MemberJwtUtils memberJwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfoTo userInfoTo = new UserInfoTo();
        // 不主动创建会话（SPA 用 token 识别用户，避免每个请求下发 MALL_SESSION cookie）
        HttpSession session = request.getSession(false);
        if (session != null) {
            // 获得当前登录用户的信息
            MemberResponseVo memberResponseVo = (MemberResponseVo) session.getAttribute(LOGIN_USER);
            if (memberResponseVo != null) {
                // 用户登录了
                userInfoTo.setUserId(memberResponseVo.getId());
            }
        }

        // 前后端分离 API：从 JWT（会员 token）解析登录会员（SPA 登录态）
        if (userInfoTo.getUserId() == null && request.getRequestURI().startsWith("/api/")) {
            String token = memberJwtUtils.extractToken(request);
            Long memberId = memberJwtUtils.parseMemberId(token);
            if (memberId != null) {
                userInfoTo.setUserId(memberId);
            }
            // token 缺失/过期/无效时忽略，按临时用户处理
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                // user-key
                String name = cookie.getName();
                if (name.equals(TEMP_USER_COOKIE_NAME)) {
                    userInfoTo.setUserKey(cookie.getValue());
                    // 标记为已是临时用户
                    userInfoTo.setTempUser(true);
                }
            }
        }
        // 如果没有临时用户一定分配一个临时用户
        if (StringUtils.isEmpty(userInfoTo.getUserKey())) {
            String uuid = UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
        }
        // 目标方法执行之前
        toThreadLocal.set(userInfoTo);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 获取当前用户的值
        UserInfoTo userInfoTo = toThreadLocal.get();
        // 如果没有临时用户一定保存一个临时用户
        if (!userInfoTo.getTempUser()) {
            // 创建一个cookie
            Cookie cookie = new Cookie(TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            // 不设 domain：浏览器自动限定到当前访问域（cart.example.com 与 localhost SPA 均生效）
            // 原 setDomain("example.com") 会导致 localhost:5173 不保存 cookie，每次请求都是新临时用户
            cookie.setMaxAge(TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
