package com.mall.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import java.util.Map;
import java.util.Optional;

/**
 * Result 工具类
 * 提供类型安全的数据提取方法，替代 Result 内部的 unchecked 泛型转换
 */
public final class ResultUtil {

    private ResultUtil() {}

    /**
     * 从 Result 的 data（Map 类型）中提取指定 key 的值，并转为目标类型
     *
     * <pre>
     * Result&lt;Map&lt;String, Object&gt;&gt; result = feignService.someApi();
     * MemberVo vo = ResultUtil.getData(result, "data", new TypeReference&lt;MemberVo&gt;() {});
     * </pre>
     *
     * @param result 接口返回的 Result
     * @param key Map 中的 key
     * @param typeReference 目标类型
     * @return 转换后的对象，data 为 null 或 key 不存在时返回 null
     */
    public static <U> U getData(
            Result<?> result,
            String key,
            TypeReference<U> typeReference) {
        if (result == null || result.getData() == null) return null;
        Object data = result.getData();
        if (!(data instanceof Map)) return null;
        Object value = ((Map<?, ?>) data).get(key);
        if (value == null || typeReference == null) return null;
        return JSON.parseObject(JSON.toJSONString(value), typeReference.getType());
    }

    /**
     * 从 Result 的 data（Map 类型）中提取指定 key 的值，返回 Object
     *
     * @param result 接口返回的 Result
     * @param key Map 中的 key
     * @return 原始对象，data 为 null 或 key 不存在时返回 null
     */
    public static Object getData(Result<?> result, String key) {
        if (result == null || result.getData() == null) return null;
        Object data = result.getData();
        if (!(data instanceof Map)) return null;
        return ((Map<?, ?>) data).get(key);
    }

    /**
     * 从 Result 的 data（Map 类型）中提取指定 key 的值，并转为目标类型，返回 Optional
     *
     * <pre>
     * ResultUtil.getDataOptional(result, "data", new TypeReference&lt;MemberVo&gt;() {})
     *     .ifPresent(vo -> ...);
     * </pre>
     *
     * @param result 接口返回的 Result
     * @param key Map 中的 key
     * @param typeReference 目标类型
     * @return Optional 包装的转换结果
     */
    public static <U> Optional<U> getDataOptional(
            Result<?> result,
            String key,
            TypeReference<U> typeReference) {
        return Optional.ofNullable(getData(result, key, typeReference));
    }

    /**
     * 从 Result 的 data（Map 类型）中提取指定 key 的值，返回 Optional&lt;Object&gt;
     *
     * @param result 接口返回的 Result
     * @param key Map 中的 key
     * @return Optional 包装的原始对象
     */
    public static Optional<Object> getDataOptional(
            Result<?> result,
            String key) {
        return Optional.ofNullable(getData(result, key));
    }
}
