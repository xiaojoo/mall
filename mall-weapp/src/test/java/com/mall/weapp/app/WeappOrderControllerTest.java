package com.mall.weapp.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.weapp.entity.WeappOrderCreateEntity;
import com.mall.weapp.feign.OrderFeignService;
import com.mall.common.utils.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeappOrderController.class)
@Import(WeappTestConfig.class)
class WeappOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderFeignService orderFeignService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void list_success() throws Exception {
        Result<Object> result = Result.success(Collections.emptyList());
        when(orderFeignService.list(any())).thenReturn(result);

        mockMvc.perform(get("/weapp/order/list").param("page", "1").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void list_withStatus() throws Exception {
        Result<Object> result = Result.success(Collections.emptyList());
        when(orderFeignService.list(any())).thenReturn(result);

        mockMvc.perform(get("/weapp/order/list")
                        .param("page", "1")
                        .param("limit", "10")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void info_success() throws Exception {
        Result<Object> result = Result.success(new HashMap<>());
        when(orderFeignService.info(1L)).thenReturn(result);

        mockMvc.perform(get("/weapp/order/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void create_success() throws Exception {
        WeappOrderCreateEntity vo = new WeappOrderCreateEntity();
        vo.setCartIds("1,2");
        vo.setAddressId(10L);

        Map<String, Object> orderResult = new HashMap<>();
        orderResult.put("orderId", "20240330001");
        Result<Object> result = Result.success(orderResult);
        when(orderFeignService.createOrder(any(), any())).thenReturn(result);

        mockMvc.perform(post("/weapp/order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void cancel_success() throws Exception {
        Result<Object> result = Result.success("取消成功");
        when(orderFeignService.cancel(1L)).thenReturn(result);

        mockMvc.perform(post("/weapp/order/cancel/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
