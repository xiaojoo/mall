package com.mall.order.controller;

import com.mall.common.utils.PageUtils;
import com.mall.order.config.OrderWebConfiguration;
import com.mall.order.entity.OrderEntity;
import com.mall.order.interceptor.LoginUserInterceptor;
import com.mall.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = OrderController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {OrderWebConfiguration.class, LoginUserInterceptor.class}))
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testGetOrderStatus() throws Exception {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setOrderSn("SN20240801001");
        when(orderService.getOrderByOrderSn("SN20240801001")).thenReturn(order);

        mockMvc.perform(get("/order/order/status/SN20240801001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void testList() throws Exception {
        PageUtils page = new PageUtils(Collections.emptyList(), 1, 10, 0);
        when(orderService.queryPage(anyMap())).thenReturn(page);

        mockMvc.perform(get("/order/order/list")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testListWithItem() throws Exception {
        PageUtils page = new PageUtils(Collections.emptyList(), 1, 10, 0);
        when(orderService.queryPageWithItem(anyMap())).thenReturn(page);

        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);

        mockMvc.perform(post("/order/order/listWithItem")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testInfo() throws Exception {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        when(orderService.getById(1L)).thenReturn(order);

        mockMvc.perform(get("/order/order/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void testSave() throws Exception {
        OrderEntity order = new OrderEntity();
        order.setOrderSn("SN20240801001");
        when(orderService.save(any(OrderEntity.class))).thenReturn(true);

        mockMvc.perform(post("/order/order/save")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        when(orderService.updateById(any(OrderEntity.class))).thenReturn(true);

        mockMvc.perform(post("/order/order/update")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        when(orderService.removeByIds(any())).thenReturn(true);

        mockMvc.perform(post("/order/order/delete")
                        .contentType("application/json")
                        .content("[1,2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
