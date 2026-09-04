package com.mall.common.jackson;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.jdk.JavaUtilDateDeserializer;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * 宽松的 java.util.Date 反序列化器。
 *
 * <p>背景：前端 el-date-picker / JS Date 提交的是 ISO-8601 UTC 字符串
 * （如 2026-08-18T16:00:00.000Z），而 spring.jackson.date-format 配置的
 * yyyy-MM-dd HH:mm:ss 无法解析该格式，导致
 * HttpMessageNotReadableException: JSON parse error（Unparseable date）。</p>
 *
 * <p>解析顺序：</p>
 * <ol>
 *   <li>优先按 ISO-8601 变体解析（含 Z / +08:00 时区、带/不带毫秒）；</li>
 *   <li>失败后回退默认逻辑（spring.jackson.date-format 的 yyyy-MM-dd HH:mm:ss 等）。</li>
 * </ol>
 *
 * <p>仅影响反序列化；序列化格式仍由 spring.jackson.date-format 控制，不改变接口返回格式。</p>
 */
public class LenientDateDeserializer extends JavaUtilDateDeserializer {

    /** ISO-8601 变体，按优先级排列 */
    private static final String[] ISO_PATTERNS = {
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", // 2026-08-18T16:00:00.000Z / .000+08:00
            "yyyy-MM-dd'T'HH:mm:ssXXX",     // 2026-08-18T16:00:00Z
            "yyyy-MM-dd'T'HH:mm:ss.SSS",    // 2026-08-18T16:00:00.000（无时区）
            "yyyy-MM-dd'T'HH:mm:ss",        // 2026-08-18T16:00:00（无时区）
    };

    @Override
    protected Date _parseDate(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String value = p.getString();
        if (value != null && !value.trim().isEmpty()) {
            // 无时区字符串按 spring.jackson.time-zone（默认 GMT+8）解释，与序列化保持一致
            TimeZone tz = configuredTimeZone(ctxt);
            for (String pattern : ISO_PATTERNS) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                    sdf.setTimeZone(tz);
                    return sdf.parse(value.trim());
                } catch (ParseException ignored) {
                    // 尝试下一个格式
                }
            }
        }
        return super._parseDate(p, ctxt);
    }

    private static TimeZone configuredTimeZone(DeserializationContext ctxt) {
        DateFormat df = ctxt.getConfig().getDateFormat();
        if (df instanceof SimpleDateFormat sdf) {
            return sdf.getTimeZone();
        }
        return TimeZone.getDefault();
    }
}
