package com.mall.order.controller;

import com.mall.common.utils.PageUtils;
import com.mall.order.config.OrderWebConfiguration;
import com.mall.order.entity.OrderReturnReasonEntity;
import com.mall.order.interceptor.LoginUserInterceptor;
import com.mall.order.service.OrderReturnReasonService;
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

@WebMvcTest(value = OrderReturnReasonController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {OrderWebConfiguration.class, LoginUserInterceptor.class}))
class OrderReturnReasonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderReturnReasonService orderReturnReasonService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testList() throws Exception {
        PageUtils page = new PageUtils(Collections.emptyList(), 1, 10, 0);
        when(orderReturnReasonService.queryPage(anyMap())).thenReturn(page);

        mockMvc.perform(get("/order/orderreturnreason/list")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testInfo() throws Exception {
        OrderReturnReasonEntity reason = new OrderReturnReasonEntity();
        reason.setId(1L);
        when(orderReturnReasonService.getById(1L)).thenReturn(reason);

        mockMvc.perform(get("/order/orderreturnreason/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void testSave() throws Exception {
        OrderReturnReasonEntity reason = new OrderReturnReasonEntity();
        reason.setName("质量问题");
        when(orderReturnReasonService.save(any(OrderReturnReasonEntity.class))).thenReturn(true);

        mockMvc.perform(post("/order/orderreturnreason/save")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(reason)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        OrderReturnReasonEntity reason = new OrderReturnReasonEntity();
        reason.setId(1L);
        when(orderReturnReasonService.updateById(any(OrderReturnReasonEntity.class))).thenReturn(true);

        mockMvc.perform(post("/order/orderreturnreason/update")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(reason)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        when(orderReturnReasonService.removeByIds(any())).thenReturn(true);

        mockMvc.perform(post("/order/orderreturnreason/delete")
                        .contentType("application/json")
                        .content("[1,2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
