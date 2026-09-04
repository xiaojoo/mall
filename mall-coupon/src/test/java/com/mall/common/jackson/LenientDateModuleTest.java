package com.mall.common.jackson;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证宽松日期反序列化：
 * 模拟 mall-coupon.yaml 的 jackson 配置（date-format=yyyy-MM-dd HH:mm:ss, time-zone=GMT+8）
 * + 全局模块注册后，前端 ISO-8601 UTC 字符串（2026-08-18T16:00:00.000Z）可正常解析，
 * 原有 yyyy-MM-dd HH:mm:ss 格式不受影响，序列化格式保持不变。
 */
class LenientDateModuleTest {

    private static final long EXPECTED_MS = 1787068800000L; // 2026-08-19 00:00:00 GMT+8

    private JsonMapper buildMapper() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        fmt.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        SimpleModule module = new SimpleModule("LenientDateModule");
        module.addDeserializer(Date.class, new LenientDateDeserializer());

        return JsonMapper.builder()
                .defaultDateFormat(fmt)
                .addModule(module)
                .build();
    }

    private static String formatGmt8(Date d) {
        SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        out.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return out.format(d);
    }

    @Test
    void parsesIso8601UtcWithZ() throws Exception {
        // 前端 el-date-picker / JS Date 默认序列化格式（本次报错的输入）
        Date d = buildMapper().readValue("\"2026-08-18T16:00:00.000Z\"", Date.class);
        assertEquals(EXPECTED_MS, d.getTime());
    }

    @Test
    void parsesIso8601WithoutMillis() throws Exception {
        Date d = buildMapper().readValue("\"2026-08-18T16:00:00Z\"", Date.class);
        assertEquals(EXPECTED_MS, d.getTime());
    }

    @Test
    void stillParsesLegacySpaceFormat() throws Exception {
        // 原有接口客户端格式不受影响
        Date d = buildMapper().readValue("\"2026-08-19 00:00:00\"", Date.class);
        assertEquals(EXPECTED_MS, d.getTime());
    }

    @Test
    void serializationFormatUnchanged() throws Exception {
        JsonMapper mapper = buildMapper();
        Date d = new Date(EXPECTED_MS);
        // 序列化仍输出 yyyy-MM-dd HH:mm:ss（GMT+8），不改变接口返回格式
        assertEquals("\"2026-08-19 00:00:00\"", mapper.writeValueAsString(d));
    }
}
