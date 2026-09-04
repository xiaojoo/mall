package com.mall.product.app;

import java.util.Arrays;
import java.util.List;

import com.alibaba.fastjson2.JSON;
import com.mall.product.entity.CategoryEntity;
import com.mall.product.service.CategoryService;
import com.mall.product.service.impl.CategoryServiceImpl;
import com.mall.product.service.impl.SkuInfoServiceImpl;

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
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

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
        when(categoryService.listWithTree()).thenReturn(Arrays.asList());
        mockMvc.perform(get("/api/product/category/list/tree"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInfo() throws Exception {
        CategoryEntity category = new CategoryEntity();
        category.setCatId(1L);
        when(categoryService.getById(1L)).thenReturn(category);
        mockMvc.perform(get("/api/product/category/info/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSave() throws Exception {
        CategoryEntity category = new CategoryEntity();
        category.setName("TestCategory");
        when(categoryService.save(any(CategoryEntity.class))).thenReturn(true);
        mockMvc.perform(post("/api/product/category/save").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(category)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateSort() throws Exception {
        CategoryEntity category = new CategoryEntity();
        category.setCatId(1L);
        category.setSort(1);
        when(categoryService.updateBatchById(anyList())).thenReturn(true);
        mockMvc.perform(post("/api/product/category/update/sort").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(new CategoryEntity[]{category})))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        CategoryEntity category = new CategoryEntity();
        category.setCatId(1L);
        category.setName("UpdatedCategory");
        doNothing().when(categoryService).updateCascade(any(CategoryEntity.class));
        mockMvc.perform(post("/api/product/category/update").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(category)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        when(categoryService.removeByIds(anyList())).thenReturn(true);
        mockMvc.perform(post("/api/product/category/delete").contentType(MediaType.APPLICATION_JSON).content("[1,2]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }
}
