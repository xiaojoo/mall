package com.mall.auth.exception;

import com.mall.common.exception.RRException;
import com.mall.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * mall-auth 全局异常处理器
 * 注意：mall-common 的 RRExceptionHandler 在 com.mall.common 包，不在本服务扫描范围，
 * 这里单独声明，保证 RRException 等异常能返回 {code, message} 给前端
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RRException.class)
    public Result<Object> handleRRException(RRException e) {
        return Result.result(null, e.getCode(), e.getMessage());
    }

    /**
     * 静态资源/未知路径 404（如旧页面流遗留的 /login.html 请求）：
     * 返回 JSON 404 即可，不打 ERROR 日志，避免刷屏
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Object> handleNoResourceFound(NoResourceFoundException e) {
        return Result.fail(404, "资源不存在");
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception e) {
        log.error(e.getMessage(), e);
        return Result.fail(500, "系统异常，请稍后重试");
    }
}
