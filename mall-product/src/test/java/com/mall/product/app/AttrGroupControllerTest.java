package com.mall.product.app;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.mall.product.entity.AttrEntity;
import com.mall.product.entity.AttrGroupEntity;
import com.mall.product.service.AttrAttrgroupRelationService;
import com.mall.product.service.AttrGroupService;
import com.mall.product.service.AttrService;
import com.mall.product.service.impl.CategoryServiceImpl;
import com.mall.product.service.impl.SkuInfoServiceImpl;
import com.mall.product.vo.AttrGroupRelationVo;
import com.mall.product.vo.AttrGroupWithAttrsVo;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
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
class AttrGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttrGroupService attrGroupService;

    @MockitoBean
    private CategoryServiceImpl categoryService;

    @MockitoBean
    private AttrService attrService;

    @MockitoBean
    private AttrAttrgroupRelationService relationService;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @MockitoBean
    private javax.sql.DataSource dataSource;

    @MockitoBean
    private SkuInfoServiceImpl skuInfoServiceImpl;

    @Test
    void addRelation() throws Exception {
        AttrGroupRelationVo vo = new AttrGroupRelationVo();
        vo.setAttrGroupId(1L);
        vo.setAttrId(1L);
        doNothing().when(relationService).saveBatch(org.mockito.ArgumentMatchers.<List<AttrGroupRelationVo>>any());
        mockMvc.perform(post("/api/product/attrgroup/attr/relation").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(Arrays.asList(vo))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void attrRelation() throws Exception {
        AttrEntity attr = new AttrEntity();
        attr.setAttrId(1L);
        when(attrService.getReLationAttr(1L)).thenReturn(Arrays.asList(attr));
        mockMvc.perform(get("/api/product/attrgroup/1/attr/relation"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInfo() throws Exception {
        AttrGroupEntity entity = new AttrGroupEntity();
        entity.setAttrGroupId(1L);
        entity.setCatelogId(1L);
        when(attrGroupService.getById(1L)).thenReturn(entity);
        when(categoryService.findCatelogPath(1L)).thenReturn(new Long[]{1L});
        mockMvc.perform(get("/api/product/attrgroup/info/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSave() throws Exception {
        AttrGroupEntity entity = new AttrGroupEntity();
        entity.setAttrGroupName("TestGroup");
        when(attrGroupService.save(any(AttrGroupEntity.class))).thenReturn(true);
        mockMvc.perform(post("/api/product/attrgroup/save").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(entity)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        AttrGroupEntity entity = new AttrGroupEntity();
        entity.setAttrGroupId(1L);
        when(attrGroupService.updateById(any(AttrGroupEntity.class))).thenReturn(true);
        mockMvc.perform(post("/api/product/attrgroup/update").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(entity)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        when(attrGroupService.removeByIds(anyList())).thenReturn(true);
        mockMvc.perform(post("/api/product/attrgroup/delete").contentType(MediaType.APPLICATION_JSON).content("[1,2]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void listByCatelog() throws Exception {
        AttrGroupEntity entity = new AttrGroupEntity();
        entity.setAttrGroupId(1L);
        PageUtils page = new PageUtils(Arrays.asList(entity), 1, 10, 1);
        when(attrGroupService.queryPage(anyMap(), eq(1L))).thenReturn(page);
        mockMvc.perform(get("/api/product/attrgroup/list/1").param("page", "1").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].attrGroupId").value(1));
    }

    @Test
    void listWithCatelogFilter() throws Exception {
        AttrGroupEntity entity = new AttrGroupEntity();
        entity.setAttrGroupId(1L);
        PageUtils page = new PageUtils(Arrays.asList(entity), 1, 10, 1);
        when(attrGroupService.queryPage(anyMap(), eq(1L))).thenReturn(page);
        mockMvc.perform(get("/api/product/attrgroup/list").param("catelogId", "1").param("page", "1").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].attrGroupId").value(1));
    }
}
