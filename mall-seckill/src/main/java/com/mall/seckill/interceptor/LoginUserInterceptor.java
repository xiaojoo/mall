package com.mall.seckill.interceptor;

import com.mall.common.constant.AuthConstant;
import com.mall.common.jwt.MemberJwtUtils;
import com.mall.common.vo.MemberResponseVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 秒杀登录拦截器。
 *
 * <p>兼容两种认证来源：</p>
 * <ol>
 *   <li>API 流（mall-ui，JWT token header）：通过 MemberJwtUtils 解析会员 id；</li>
 *   <li>旧页面流（历史模板引擎，session）：读取 session 中的登录用户。</li>
 * </ol>
 *
 * <p>未登录时不再重定向（API 场景），由业务层抛 RRException("请先登录")，
 * 经全局异常处理器返回统一 JSON，前端可展示。</p>
 */
@Component
@RequiredArgsConstructor
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberResponseVo> loginUser = new ThreadLocal<>();

    private final MemberJwtUtils memberJwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        // 兼容旧页面流(/kill)与新 API 流(/api/seckill/kill)
        boolean match = antPathMatcher.match("/kill", uri)
                || antPathMatcher.match("/api/seckill/kill", uri);
        if (match) {
            MemberResponseVo attribute = (MemberResponseVo) request.getSession()
                    .getAttribute(AuthConstant.LOGIN_USER);
            if (attribute == null) {
                // API 流：从 JWT token 解析会员
                Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
                if (memberId != null) {
                    attribute = new MemberResponseVo();
                    attribute.setId(memberId);
                }
            }
            if (attribute != null) {
                loginUser.set(attribute);
            }
            // 未登录不在此处拦截，由业务层抛"请先登录"
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 防止 ThreadLocal 在线程池复用下串号
        loginUser.remove();
    }
}
