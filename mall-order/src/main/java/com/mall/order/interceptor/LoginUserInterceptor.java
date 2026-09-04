package com.mall.order.interceptor;

import com.mall.common.constant.AuthConstant;
import com.mall.common.jwt.MemberJwtUtils;
import com.mall.common.vo.MemberResponseVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 登录用户拦截器
 * <p>
 * 页面请求（Session）：从 Session 中解析登录会员；
 * 前后端分离 API（/api/**）：从 JWT（会员 token，微博/微信登录签发）解析登录会员，
 * 解析结果统一放入 {@link #loginUser} ThreadLocal 供业务层使用。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberResponseVo> loginUser = new ThreadLocal<>();

    private final MemberJwtUtils memberJwtUtils;

    // 登录跳转前端地址（来自 Nacos mall-order 配置）
    @Value("${mall.web.login-url:http://example.com/#/login}")
    private String loginUrl;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 订单状态回调和支付回调接口放行
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean match = antPathMatcher.match("/order/order/status/**", uri);
        boolean matchPay = antPathMatcher.match("/payed/notify", uri);
        // 微信公众平台回调（服务器校验/消息推送/OAuth 回调）无需登录
        boolean matchWx = antPathMatcher.match("/wx/portal/public/**", uri);
        // 商品评价购买校验（服务间 Feign 内部接口，memberId 由调用方传入，无需登录态）
        boolean matchPaidCheck = antPathMatcher.match("/api/order/paid/check", uri);
        // 管理端售后申请/退款审核接口（admin 前端调用，携带 admin JWT 而非会员 JWT，放行）
        boolean matchReturnApplyAdmin = antPathMatcher.match("/api/order/orderreturnapply/**", uri);
        // 管理端订单管理接口（列表/详情/发货/关闭/删除，admin 前端调用，携带 admin JWT，放行）
        boolean matchOrderAdmin = antPathMatcher.match("/api/order/order/**", uri);
        if (match || matchPay || matchWx || matchPaidCheck || matchReturnApplyAdmin || matchOrderAdmin) {
            return true;
        }

        // 前后端分离 API：基于 JWT（会员 token，微博/微信登录签发）解析登录用户
        if (uri.startsWith("/api/")) {
            String token = memberJwtUtils.extractToken(request);
            if (StringUtils.isBlank(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            Long memberId = memberJwtUtils.parseMemberId(token);
            if (memberId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            MemberResponseVo member = new MemberResponseVo();
            member.setId(memberId);
            loginUser.set(member);
            return true;
        }

        // 页面请求：从 Session 解析登录会员
        MemberResponseVo attribute = (MemberResponseVo) request.getSession().getAttribute(AuthConstant.LOGIN_USER);
        if (attribute != null) {
            loginUser.set(attribute);
            return true;
        }

        // 没有登录就去登录
        request.getSession().setAttribute("msg", "请先进行登录");
        // 旧页面流遗留：跳转商城 SPA 登录页（原 auth.example.com/login.html 已随纯 API 化删除）
        response.sendRedirect(loginUrl);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理 ThreadLocal，避免线程复用导致用户串号
        loginUser.remove();
    }
}
