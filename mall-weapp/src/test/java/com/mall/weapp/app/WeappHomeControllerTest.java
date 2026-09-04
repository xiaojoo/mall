package com.mall.weapp.app;

import com.mall.weapp.feign.ProductFeignService;
import com.mall.common.utils.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeappHomeController.class)
@Import(WeappTestConfig.class)
class WeappHomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductFeignService productFeignService;

    @Test
    void index_success() throws Exception {
        Result<Object> hotResult = Result.success(new HashMap<>());
        Result<Object> categoryResult = Result.success(new ArrayList<>());
        when(productFeignService.list(anyMap())).thenReturn(hotResult);
        when(productFeignService.categoryList()).thenReturn(categoryResult);

        mockMvc.perform(get("/weapp/home/index"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.banners").isArray());
    }
}
