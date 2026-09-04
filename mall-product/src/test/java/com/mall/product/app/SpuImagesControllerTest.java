package com.mall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.mall.product.entity.SpuImagesEntity;
import com.mall.product.service.SpuImagesService;
import com.mall.product.service.impl.CategoryServiceImpl;
import com.mall.product.service.impl.SkuInfoServiceImpl;
import com.mall.common.utils.PageUtils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
    webEnvironment = WebEnvironment.MOCK,
    properties = {
        "spring.session.store-type=none",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "management.health.redis.enabled=false",
        "management.endpoints.enabled-by-default=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "feign.client.enabled=false",
        "spring.config.import=optional:none:",
        "spring.datasource.url=jdbc:h2:mem:test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.mybatis-plus.mapper-locations=classpath*:mapper/**/*.xml"
    }
)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisabledInAotMode
class SpuImagesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpuImagesService spuImagesService;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @MockitoBean
    private javax.sql.DataSource dataSource;

    @MockitoBean
    private CategoryServiceImpl categoryServiceImpl;

    @MockitoBean
    private SkuInfoServiceImpl skuInfoServiceImpl;

    @Test
    void testList() throws Exception {
        PageUtils pageUtils = new PageUtils(new java.util.ArrayList<>(), 0, 10, 1);
        when(spuImagesService.queryPage(anyMap())).thenReturn(pageUtils);
        mockMvc.perform(get("/api/product/spuimages/list").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInfo() throws Exception {
        SpuImagesEntity entity = new SpuImagesEntity();
        entity.setId(1L);
        when(spuImagesService.getById(1L)).thenReturn(entity);
        mockMvc.perform(get("/api/product/spuimages/info/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSave() throws Exception {
        SpuImagesEntity entity = new SpuImagesEntity();
        entity.setSpuId(1L);
        when(spuImagesService.save(any(SpuImagesEntity.class))).thenReturn(true);
        mockMvc.perform(post("/api/product/spuimages/save").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(entity)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        SpuImagesEntity entity = new SpuImagesEntity();
        entity.setId(1L);
        when(spuImagesService.updateById(any(SpuImagesEntity.class))).thenReturn(true);
        mockMvc.perform(post("/api/product/spuimages/update").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(entity)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        when(spuImagesService.removeByIds(anyList())).thenReturn(true);
        mockMvc.perform(post("/api/product/spuimages/delete").contentType(MediaType.APPLICATION_JSON).content("[1,2]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }
}
