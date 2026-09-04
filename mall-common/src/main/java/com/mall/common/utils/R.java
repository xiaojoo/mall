package com.mall.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * 返回数据 - 兼容层
 * 
 * @deprecated 请使用 Result<T> 类替代
 */
@Deprecated
public class R extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    public R() {
        put("code", "200");
        put("msg", "success");
    }

    public R setData(Object data) {
        put("data", data);
        return this;
    }

    // 利用 fastjson2 进行反序列化
    public <T> T getData(TypeReference<T> typeReference) {
        Object data = get("data");    // 默认是 Map
        String jsonString = JSON.toJSONString(data);
        T t = JSON.parseObject(jsonString, typeReference);
        return t;
    }

    // 利用 fastjson2 进行反序列化
    public <T> T getData(String key, TypeReference<T> typeReference) {
        Object data = get(key);    // 默认是 Map
        String jsonString = JSON.toJSONString(data);
        T t = JSON.parseObject(jsonString, typeReference);
        return t;
    }

    public static R error() {
        return error("500", "未知异常，请联系管理员");
    }

    public static R error(String msg) {
        return error("500", msg);
    }

    public static R error(String code, String msg) {
        R r = new R();
        r.put("code", code);
        r.put("msg", msg);
        return r;
    }

    public static R ok(String msg) {
        R r = new R();
        r.put("msg", msg);
        return r;
    }

    public static R ok(Map<String, Object> map) {
        R r = new R();
        r.putAll(map);
        return r;
    }

    public static R ok() {
        return new R();
    }

    public R put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    public String getCode() {
        return (String) this.get("code");
    }
}