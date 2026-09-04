package com.mall.weapp.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.weapp.feign.ProductFeignService;
import com.mall.common.utils.Result;
import com.mall.common.utils.PageUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeappProductController.class)
@Import(WeappTestConfig.class)
class WeappProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductFeignService productFeignService;

    @Test
    void list_success() throws Exception {
        Result<Object> result = Result.success(new java.util.ArrayList<>());
        when(productFeignService.list(any())).thenReturn(result);

        mockMvc.perform(get("/weapp/product/list").param("page", "1").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void list_withCatalogId() throws Exception {
        Result<Object> result = Result.success(new java.util.ArrayList<>());
        when(productFeignService.list(any())).thenReturn(result);

        mockMvc.perform(get("/weapp/product/list").param("catalogId", "1").param("page", "1").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void info_success() throws Exception {
        Result<Object> result = Result.success(new HashMap<>());
        when(productFeignService.info(1L)).thenReturn(result);

        mockMvc.perform(get("/weapp/product/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void category_success() throws Exception {
        Result<Object> result = Result.success(java.util.Collections.emptyList());
        when(productFeignService.categoryList()).thenReturn(result);

        mockMvc.perform(get("/weapp/product/category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void hot_success() throws Exception {
        Result<Object> result = Result.success(java.util.Collections.emptyList());
        when(productFeignService.list(any())).thenReturn(result);

        mockMvc.perform(get("/weapp/product/hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void newSuccess() throws Exception {
        Result<Object> result = Result.success(java.util.Collections.emptyList());
        when(productFeignService.list(any())).thenReturn(result);

        mockMvc.perform(get("/weapp/product/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
