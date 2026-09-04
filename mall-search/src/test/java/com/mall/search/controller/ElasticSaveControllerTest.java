package com.mall.search.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson2.JSON;
import com.mall.common.to.es.SkuEsModel;
import com.mall.search.service.ProductSaveService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    value = ElasticSaveController.class,
    excludeAutoConfiguration = {
        DataRedisAutoConfiguration.class,
        DataRedisRepositoriesAutoConfiguration.class,
    }
)
class ElasticSaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductSaveService productSaveService;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    void productStatusUp_success() throws Exception {
        List<SkuEsModel> skuEsModels = new ArrayList<>();
        SkuEsModel model = new SkuEsModel();
        model.setSkuId(1L);
        model.setSkuTitle("Test Product");
        model.setSkuPrice(new BigDecimal("99.99"));
        skuEsModels.add(model);

        when(productSaveService.productStatusUp(skuEsModels)).thenReturn(false);

        mockMvc.perform(post("/search/save/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(skuEsModels)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void productStatusUp_fail() throws Exception {
        List<SkuEsModel> skuEsModels = new ArrayList<>();
        SkuEsModel model = new SkuEsModel();
        model.setSkuId(1L);
        model.setSkuTitle("Test Product");
        skuEsModels.add(model);

        when(productSaveService.productStatusUp(skuEsModels)).thenReturn(true);

        mockMvc.perform(post("/search/save/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(skuEsModels)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(11000));
    }
}
