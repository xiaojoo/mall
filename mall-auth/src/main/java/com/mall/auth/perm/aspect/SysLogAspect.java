package com.mall.auth.perm.aspect;

import com.alibaba.fastjson2.JSON;
import com.mall.auth.perm.entity.SysLogEntity;
import com.mall.auth.perm.service.SysLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * 操作日志切面
 * 自动记录 /perm/** 接口的操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SysLogAspect {

    /** 敏感字段：日志参数脱敏，避免明文密码等落库 */
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "\"((?i)password|oldpassword|newpassword|pwd|salt|captcha|token|authorization|secret)\"\\s*:\\s*\"(?:\\\\.|[^\"\\\\])*\"");
    private static final String MASK = "\"$1\":\"***\"";

    private final SysLogService sysLogService;

    @Pointcut("execution(* com.mall.auth.perm.controller..*.*(..))")
    public void logPointcut() {}

    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        SysLogEntity sysLog = new SysLogEntity();
        try {
            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                sysLog.setIp(getClientIp(request));
                sysLog.setMethod(request.getMethod() + " " + request.getRequestURI());
            }

            // 获取操作描述（类名.方法名）
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            sysLog.setOperation(className + "." + methodName);

            // 获取参数（敏感字段脱敏；过滤容器对象避免序列化 HttpSession 等大对象图/环导致 StackOverflowError）
            try {
                Object[] args = joinPoint.getArgs();
                Object[] filtered = Arrays.stream(args)
                        .filter(a -> !(a instanceof HttpSession)
                                && !(a instanceof HttpServletRequest)
                                && !(a instanceof HttpServletResponse))
                        .toArray();
                String params = JSON.toJSONString(filtered);
                params = maskSensitive(params);
                if (params.length() > 2000) {
                    params = params.substring(0, 2000) + "...(truncated)";
                }
                sysLog.setParams(params);
            } catch (Throwable e) {
                // 参数序列化失败（含 StackOverflowError）不影响业务
            }

            // 从 request attribute 获取 userId/username（PermissionInterceptor 由 JWT 解析后写入）
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                Object userId = attrs.getRequest().getAttribute("userId");
                if (userId != null) {
                    sysLog.setUserId(Long.parseLong(userId.toString()));
                }
                Object username = attrs.getRequest().getAttribute("username");
                if (username != null) {
                    sysLog.setUsername(username.toString());
                }
            }

            // 执行目标方法
            Object result = joinPoint.proceed();
            sysLog.setStatus(1);
            return result;

        } catch (Throwable e) {
            sysLog.setStatus(0);
            sysLog.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            sysLog.setCreateTime(new Date());
            // 异步保存日志，不阻塞主流程
            try {
                sysLogService.save(sysLog);
            } catch (Exception e) {
                log.warn("保存操作日志失败: {}", e.getMessage());
            }
            log.debug("操作日志: {} | {}ms | status={}",
                    sysLog.getOperation(),
                    System.currentTimeMillis() - startTime,
                    sysLog.getStatus());
        }
    }

    private String maskSensitive(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        return SENSITIVE_PATTERN.matcher(json).replaceAll(MASK);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
