package com.mall.order.controller;

import com.mall.common.utils.PageUtils;
import com.mall.order.config.OrderWebConfiguration;
import com.mall.order.entity.OrderItemEntity;
import com.mall.order.interceptor.LoginUserInterceptor;
import com.mall.order.service.OrderItemService;
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

@WebMvcTest(value = OrderItemController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {OrderWebConfiguration.class, LoginUserInterceptor.class}))
class OrderItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderItemService orderItemService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testList() throws Exception {
        PageUtils page = new PageUtils(Collections.emptyList(), 1, 10, 0);
        when(orderItemService.queryPage(anyMap())).thenReturn(page);

        mockMvc.perform(get("/order/orderitem/list")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testInfo() throws Exception {
        OrderItemEntity item = new OrderItemEntity();
        item.setId(1L);
        when(orderItemService.getById(1L)).thenReturn(item);

        mockMvc.perform(get("/order/orderitem/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void testSave() throws Exception {
        OrderItemEntity item = new OrderItemEntity();
        when(orderItemService.save(any(OrderItemEntity.class))).thenReturn(true);

        mockMvc.perform(post("/order/orderitem/save")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        OrderItemEntity item = new OrderItemEntity();
        item.setId(1L);
        when(orderItemService.updateById(any(OrderItemEntity.class))).thenReturn(true);

        mockMvc.perform(post("/order/orderitem/update")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        when(orderItemService.removeByIds(any())).thenReturn(true);

        mockMvc.perform(post("/order/orderitem/delete")
                        .contentType("application/json")
                        .content("[1,2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
