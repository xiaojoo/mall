package com.mall.seckill.config;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson2.JSON;
import com.mall.common.exception.BizCodeEnum;
import com.mall.common.utils.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestControllerAdvice
public class MySeckillSentinelConfig {

    @ExceptionHandler(BlockException.class)
    public void handleBlockException(BlockException e, HttpServletResponse response) throws IOException {
        Result<Void> error = Result.fail(BizCodeEnum.TO_MANY_REQUEST.getCode(), BizCodeEnum.TO_MANY_REQUEST.getMsg());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(JSON.toJSONString(error));
    }
}
