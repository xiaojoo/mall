package com.mall.cart.exception;

import com.mall.common.utils.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class RuntimeExceptionHandler {
    /**
     * 全局统一异常处理
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseBody
    public Result<Object> handler(RuntimeException exception) {
        return Result.fail(exception.getMessage());
    }

    @ExceptionHandler(CartExceptionHandler.class)
    @ResponseBody
    public Result<Object> userHandler(CartExceptionHandler exception) {
        return Result.fail("购物车无此商品");
    }
}
