package com.mall.weapp.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.weapp.feign.CartFeignService;
import com.mall.common.utils.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeappCartController.class)
@Import(WeappTestConfig.class)
class WeappCartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartFeignService cartFeignService;

    @Test
    void list_success() throws Exception {
        Result<Object> result = Result.success(Collections.emptyList());
        when(cartFeignService.list()).thenReturn(result);

        mockMvc.perform(get("/weapp/cart/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void add_success() throws Exception {
        Result<Object> result = Result.success("添加成功");
        when(cartFeignService.addToCart(any(), any())).thenReturn(result);

        mockMvc.perform(post("/weapp/cart/add")
                        .param("skuId", "1")
                        .param("num", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void update_success() throws Exception {
        Result<Object> result = Result.success("更新成功");
        when(cartFeignService.updateCart(any(), any())).thenReturn(result);

        mockMvc.perform(post("/weapp/cart/update")
                        .param("skuId", "1")
                        .param("num", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void delete_success() throws Exception {
        Result<Object> result = Result.success("删除成功");
        when(cartFeignService.deleteCart(any())).thenReturn(result);

        mockMvc.perform(post("/weapp/cart/delete")
                        .param("skuIds", "1")
                        .param("skuIds", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
