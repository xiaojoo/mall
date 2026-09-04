package com.mall.order.controller;

import com.mall.common.utils.PageUtils;
import com.mall.order.config.OrderWebConfiguration;
import com.mall.order.entity.OrderSettingEntity;
import com.mall.order.interceptor.LoginUserInterceptor;
import com.mall.order.service.OrderSettingService;
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

@WebMvcTest(value = OrderSettingController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {OrderWebConfiguration.class, LoginUserInterceptor.class}))
class OrderSettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderSettingService orderSettingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testList() throws Exception {
        PageUtils page = new PageUtils(Collections.emptyList(), 1, 10, 0);
        when(orderSettingService.queryPage(anyMap())).thenReturn(page);

        mockMvc.perform(get("/order/ordersetting/list")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testInfo() throws Exception {
        OrderSettingEntity setting = new OrderSettingEntity();
        setting.setId(1L);
        when(orderSettingService.getById(1L)).thenReturn(setting);

        mockMvc.perform(get("/order/ordersetting/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void testSave() throws Exception {
        OrderSettingEntity setting = new OrderSettingEntity();
        when(orderSettingService.save(any(OrderSettingEntity.class))).thenReturn(true);

        mockMvc.perform(post("/order/ordersetting/save")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(setting)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        OrderSettingEntity setting = new OrderSettingEntity();
        setting.setId(1L);
        when(orderSettingService.updateById(any(OrderSettingEntity.class))).thenReturn(true);

        mockMvc.perform(post("/order/ordersetting/update")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(setting)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        when(orderSettingService.removeByIds(any())).thenReturn(true);

        mockMvc.perform(post("/order/ordersetting/delete")
                        .contentType("application/json")
                        .content("[1,2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
