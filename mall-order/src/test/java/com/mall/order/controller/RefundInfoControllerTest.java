package com.mall.order.controller;

import com.mall.common.utils.PageUtils;
import com.mall.order.config.OrderWebConfiguration;
import com.mall.order.entity.RefundInfoEntity;
import com.mall.order.interceptor.LoginUserInterceptor;
import com.mall.order.service.RefundInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = RefundInfoController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {OrderWebConfiguration.class, LoginUserInterceptor.class}))
class RefundInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RefundInfoService refundInfoService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testList() throws Exception {
        PageUtils page = new PageUtils(Collections.emptyList(), 1, 10, 0);
        when(refundInfoService.queryPage(anyMap())).thenReturn(page);

        mockMvc.perform(get("/order/refundinfo/list")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testInfo() throws Exception {
        RefundInfoEntity refund = new RefundInfoEntity();
        refund.setId(1L);
        when(refundInfoService.getById(1L)).thenReturn(refund);

        mockMvc.perform(get("/order/refundinfo/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void testSave() throws Exception {
        RefundInfoEntity refund = new RefundInfoEntity();
        when(refundInfoService.save(any(RefundInfoEntity.class))).thenReturn(true);

        mockMvc.perform(post("/order/refundinfo/save")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(refund)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        RefundInfoEntity refund = new RefundInfoEntity();
        refund.setId(1L);
        when(refundInfoService.updateById(any(RefundInfoEntity.class))).thenReturn(true);

        mockMvc.perform(post("/order/refundinfo/update")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(refund)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        when(refundInfoService.removeByIds(any())).thenReturn(true);

        mockMvc.perform(post("/order/refundinfo/delete")
                        .contentType("application/json")
                        .content("[1,2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
