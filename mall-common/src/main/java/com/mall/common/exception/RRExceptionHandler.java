package com.mall.common.exception;

import com.mall.common.utils.Result;
import com.mall.common.utils.ResultCode;
import org.slf4j.Logger;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 异常处理器
 */
@RestControllerAdvice
public class RRExceptionHandler {
    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 处理自定义异常（业务校验拒绝，如已抢购/库存不足/参数不合法）
     */
    @ExceptionHandler(RRException.class)
    public Result<Object> handleRRException(RRException e) {
        // 业务拒绝属预期情况：WARN 记录（不带堆栈），不污染错误日志
        logger.warn("业务校验未通过: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.result(null, e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Object> handlerNoFoundException(Exception e) {
        logger.error(e.getMessage(), e);
        return Result.fail(ResultCode.NOT_FOUND.getCode(), "路径不存在，请检查路径是否正确");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Object> handleNoResourceFound(NoResourceFoundException e) {
        return Result.fail(ResultCode.NOT_FOUND.getCode(), "资源不存在");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Object> handleDuplicateKeyException(DuplicateKeyException e) {
        logger.error(e.getMessage(), e);
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "数据库中已存在该记录");
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public Result<Object> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException e) {
        logger.error(e.getMessage(), e);
        return Result.fail(ResultCode.FORBIDDEN.getCode(), "没有权限，请联系管理员授权");
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public Result<Object> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException e) {
        logger.error(e.getMessage(), e);
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "数据保存失败：请检查必填项、字段长度或唯一约束");
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public Result<Object> handleHttpMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException e) {
        logger.error(e.getMessage(), e);
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "请求参数格式错误：" + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception e) {
        logger.error(e.getMessage(), e);
        // 内部项目透出真实原因便于排查；如对外部署可改回通用文案
        String msg = e.getMessage();
        if (StringUtils.isBlank(msg) && e.getCause() != null) {
            msg = e.getCause().getMessage();
        }
        if (StringUtils.isBlank(msg)) {
            msg = "系统异常，请稍后重试";
        } else {
            msg = "系统异常：" + msg;
        }
        return Result.fail(ResultCode.INTERNAL_ERROR.getCode(), msg);
    }
}
