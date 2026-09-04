package com.mall.product.app;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.mall.product.entity.SkuInfoEntity;
import com.mall.product.service.SkuInfoService;
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
class SkuInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SkuInfoService skuInfoService;

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
        when(skuInfoService.queryPageByCondition(anyMap())).thenReturn(pageUtils);
        mockMvc.perform(get("/api/product/skuinfo/list").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSearch() throws Exception {
        PageUtils pageUtils = new PageUtils(new java.util.ArrayList<>(), 0, 10, 1);
        when(skuInfoService.queryPageByCondition(anyMap())).thenReturn(pageUtils);
        mockMvc.perform(get("/api/product/skuinfo/search").param("key", "小米"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSearchEmptyKey() throws Exception {
        // key 传空串：控制器拦截，返回业务码 400
        mockMvc.perform(get("/api/product/skuinfo/search").param("key", ""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        // 不传 key：同样返回业务码 400（项目约定：业务错误 HTTP 200 + code 字段）
        mockMvc.perform(get("/api/product/skuinfo/search"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void testInfo() throws Exception {
        SkuInfoEntity entity = new SkuInfoEntity();
        entity.setSkuId(1L);
        when(skuInfoService.getById(1L)).thenReturn(entity);
        mockMvc.perform(get("/api/product/skuinfo/info/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSave() throws Exception {
        SkuInfoEntity entity = new SkuInfoEntity();
        entity.setSkuName("TestSku");
        when(skuInfoService.save(any(SkuInfoEntity.class))).thenReturn(true);
        mockMvc.perform(post("/api/product/skuinfo/save").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(entity)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        SkuInfoEntity entity = new SkuInfoEntity();
        entity.setSkuId(1L);
        when(skuInfoService.updateById(any(SkuInfoEntity.class))).thenReturn(true);
        mockMvc.perform(post("/api/product/skuinfo/update").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(entity)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        when(skuInfoService.removeByIds(anyList())).thenReturn(true);
        mockMvc.perform(post("/api/product/skuinfo/delete").contentType(MediaType.APPLICATION_JSON).content("[1,2]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }
}
