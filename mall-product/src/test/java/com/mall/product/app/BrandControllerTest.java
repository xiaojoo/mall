package com.mall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.mall.product.entity.BrandEntity;
import com.mall.product.service.BrandService;
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
import static org.mockito.Mockito.doNothing;
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
class BrandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandService brandService;

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
        when(brandService.queryPage(anyMap())).thenReturn(pageUtils);
        mockMvc.perform(get("/api/product/brand/list").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void infos() throws Exception {
        BrandEntity brand = new BrandEntity();
        brand.setBrandId(1L);
        when(brandService.getBrandsByIds(anyList())).thenReturn(Arrays.asList(brand));
        mockMvc.perform(get("/api/product/brand/infos").param("brandIds", "1", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInfo() throws Exception {
        BrandEntity brand = new BrandEntity();
        brand.setBrandId(1L);
        when(brandService.getById(1L)).thenReturn(brand);
        mockMvc.perform(get("/api/product/brand/info/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSave() throws Exception {
        BrandEntity brand = new BrandEntity();
        brand.setName("TestBrand");
        brand.setLogo("http://test.com/logo.png");
        brand.setShowStatus(1);
        brand.setFirstLetter("A");
        brand.setSort(1);
        when(brandService.save(any(BrandEntity.class))).thenReturn(true);
        mockMvc.perform(post("/api/product/brand/save").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(brand)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        BrandEntity brand = new BrandEntity();
        brand.setBrandId(1L);
        brand.setName("UpdatedBrand");
        doNothing().when(brandService).updateDetail(any(BrandEntity.class));
        mockMvc.perform(post("/api/product/brand/update").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(brand)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateStatus() throws Exception {
        BrandEntity brand = new BrandEntity();
        brand.setBrandId(1L);
        brand.setShowStatus(1);
        when(brandService.updateById(any(BrandEntity.class))).thenReturn(true);
        mockMvc.perform(post("/api/product/brand/update/status").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(brand)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        when(brandService.removeByIds(anyList())).thenReturn(true);
        mockMvc.perform(post("/api/product/brand/delete").contentType(MediaType.APPLICATION_JSON).content("[1,2]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }
}
