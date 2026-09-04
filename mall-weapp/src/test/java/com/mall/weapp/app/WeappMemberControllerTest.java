package com.mall.weapp.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.weapp.entity.WeappLoginEntity;
import com.mall.weapp.feign.MemberFeignService;
import com.mall.common.utils.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeappMemberController.class)
@Import(WeappTestConfig.class)
class WeappMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberFeignService memberFeignService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void info_success() throws Exception {
        Result<Object> result = Result.success(new HashMap<>());
        when(memberFeignService.info()).thenReturn(result);

        mockMvc.perform(get("/weapp/member/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void login_success() throws Exception {
        WeappLoginEntity loginVo = new WeappLoginEntity();
        loginVo.setCode("test-code");
        loginVo.setNickname("测试用户");
        loginVo.setAvatarUrl("https://example.com/avatar.png");

        Map<String, Object> loginResult = new HashMap<>();
        loginResult.put("token", "test-token-123");
        Result<Object> result = Result.success(loginResult);
        when(memberFeignService.login(any())).thenReturn(result);

        mockMvc.perform(post("/weapp/member/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginVo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
