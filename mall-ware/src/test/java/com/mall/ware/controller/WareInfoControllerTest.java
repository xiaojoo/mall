package com.mall.ware.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.mall.ware.entity.WareInfoEntity;
import com.mall.ware.service.WareInfoService;
import com.mall.ware.vo.FareVo;
import com.mall.ware.vo.MemberAddressVo;
import com.mall.common.utils.PageUtils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    value = WareInfoController.class,
    excludeAutoConfiguration = {DataSourceAutoConfiguration.class, RabbitAutoConfiguration.class}
)
class WareInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WareInfoService wareInfoService;

    @Test
    void getFare() throws Exception {
        FareVo fareVo = new FareVo();
        MemberAddressVo address = new MemberAddressVo();
        address.setId(1L);
        address.setName("Test User");
        fareVo.setAddress(address);
        fareVo.setFare(new BigDecimal("10.00"));
        when(wareInfoService.getFare(1L)).thenReturn(fareVo);

        mockMvc.perform(get("/ware/wareinfo/fare")
                        .param("addrId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testList() throws Exception {
        PageUtils pageUtils = new PageUtils(new ArrayList<>(), 0, 10, 1);
        when(wareInfoService.queryPage(anyMap())).thenReturn(pageUtils);

        mockMvc.perform(get("/ware/wareinfo/list")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInfo() throws Exception {
        WareInfoEntity entity = new WareInfoEntity();
        entity.setId(1L);
        entity.setName("Test Warehouse");
        when(wareInfoService.getById(1L)).thenReturn(entity);

        mockMvc.perform(get("/ware/wareinfo/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSave() throws Exception {
        WareInfoEntity entity = new WareInfoEntity();
        entity.setName("New Warehouse");
        when(wareInfoService.save(any(WareInfoEntity.class))).thenReturn(true);

        mockMvc.perform(post("/ware/wareinfo/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        WareInfoEntity entity = new WareInfoEntity();
        entity.setId(1L);
        entity.setName("Updated Warehouse");
        when(wareInfoService.updateById(any(WareInfoEntity.class))).thenReturn(true);

        mockMvc.perform(post("/ware/wareinfo/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        when(wareInfoService.removeByIds(anyList())).thenReturn(true);

        mockMvc.perform(post("/ware/wareinfo/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
