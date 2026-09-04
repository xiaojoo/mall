package com.mall.member.entity;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 雪花主键序列化验证：Spring Boot 4 默认 Jackson 3（tools.jackson），
 * id 必须输出为字符串，避免 JS 精度丢失
 */
class MemberEntityJsonTest {

    @Test
    void idSerializedAsString() throws Exception {
        MemberEntity member = new MemberEntity();
        member.setId(2088281316027752449L);
        member.setUsername("test");

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(member);

        System.out.println("序列化结果: " + json);
        // id 必须是带引号的字符串，且保留完整 19 位
        assertTrue(json.contains("\"id\":\"2088281316027752449\""),
                "id 未序列化为字符串: " + json);
    }
}
