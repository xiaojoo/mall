package com.mall.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一返回结果类
 * @param <T> 数据类型
 */
@Data
@Accessors(chain = true)
public class Result<T> {

    private int code;
    private String message;
    private T data;
    private Map<String, Object> extra;

    private Result() {
        this.extra = new HashMap<>();
    }

    public Result<T> putExtra(String key, Object value) {
        this.extra.put(key, value);
        return this;
    }

    /**
     * 从 Result 的 data（Map 类型）中提取指定 key 的值，并转为目标类型
     * 使用方式：ResultUtil.getData(result, "key", new TypeReference<Vo>() {})
     *
     * @deprecated 请使用 {@link ResultUtil#getData(Result, String, TypeReference)}
     */
    @Deprecated
    public <U> U getData(String key, TypeReference<U> typeReference) {
        return ResultUtil.getData(this, key, typeReference);
    }

    // ======================= 静态工厂方法 =======================

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return success("操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<T>()
                .setCode(200)
                .setMessage(message)
                .setData(data);
    }

    public static <T> Result<T> fail() {
        return fail("操作失败");
    }

    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<T>()
                .setCode(code)
                .setMessage(message);
    }

    public static <T> Result<T> result(T data, ResultCode resultCodeEnum) {
        return result(data, resultCodeEnum.getCode(), resultCodeEnum.getMessage());
    }

    public static <T> Result<T> result(T data, int code, String message) {
        return new Result<T>()
                .setCode(code)
                .setMessage(message)
                .setData(data);
    }
}
