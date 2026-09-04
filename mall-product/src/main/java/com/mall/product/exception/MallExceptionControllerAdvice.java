package com.mall.product.exception;

import com.mall.common.exception.BizCodeEnum;
import com.mall.common.exception.RRException;
import com.mall.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 集中处理所有异常
 */
@Slf4j
@RestControllerAdvice(basePackages = {"com.mall.product.controller", "com.mall.product.app"})
public class MallExceptionControllerAdvice {
    /**
     * 业务异常
     */
    @ExceptionHandler(value = RRException.class)
    public Result<Object> handleRRException(RRException e) {
        log.error("业务异常", e);
        return Result.fail(e.getCode(), e.getMsg());
    }

    /**
     * 参数校检异常
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result<Object> handleVaildException(MethodArgumentNotValidException e) {
        log.error("数据校检出现问题{}，异常类型：{}", e.getMessage(), e.getClass());
        BindingResult bindingResult = e.getBindingResult();
        Map<String, String> errorMap = new HashMap<>();
        bindingResult.getFieldErrors().forEach(fieldError -> {
            errorMap.put(fieldError.getField(), fieldError.getDefaultMessage());
        });
        return Result.fail(BizCodeEnum.VAILD_EXCEPTION.getCode(), BizCodeEnum.VAILD_EXCEPTION.getMsg()).putExtra("data", errorMap);
    }

    /**
     * Throwable抛出异常
     */
    @ExceptionHandler(value = Throwable.class)
    public Result<Object> handleException(Throwable throwable) {
        log.error("错误", throwable);
        return Result.fail(BizCodeEnum.UNKNOW_EXCEPTION.getCode(), BizCodeEnum.UNKNOW_EXCEPTION.getMsg());
    }
}
