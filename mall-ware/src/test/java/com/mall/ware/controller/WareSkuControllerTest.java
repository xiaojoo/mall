package com.mall.ware.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.mall.ware.entity.WareSkuEntity;
import com.mall.ware.service.WareSkuService;
import com.mall.ware.vo.OrderItemVo;
import com.mall.ware.vo.SkuHasStockVo;
import com.mall.ware.vo.WareSkuLockVo;
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
    value = WareSkuController.class,
    excludeAutoConfiguration = {DataSourceAutoConfiguration.class, RabbitAutoConfiguration.class}
)
class WareSkuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WareSkuService wareSkuService;

    @Test
    void orderLockStock_success() throws Exception {
        WareSkuLockVo lockVo = new WareSkuLockVo();
        lockVo.setOrderSn("ORDER001");
        List<OrderItemVo> locks = new ArrayList<>();
        OrderItemVo item = new OrderItemVo();
        item.setSkuId(1L);
        item.setCount(2);
        item.setPrice(new BigDecimal("99.99"));
        locks.add(item);
        lockVo.setLocks(locks);
        when(wareSkuService.orderLockStock(any(WareSkuLockVo.class))).thenReturn(true);

        mockMvc.perform(post("/ware/waresku/lock/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(lockVo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getSkuStock() throws Exception {
        List<SkuHasStockVo> vos = new ArrayList<>();
        SkuHasStockVo vo = new SkuHasStockVo();
        vo.setSkuId(1L);
        vo.setHasStock(true);
        vos.add(vo);
        when(wareSkuService.getSkusHasStock(anyList())).thenReturn(vos);

        mockMvc.perform(post("/ware/waresku/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getSkuHasStock() throws Exception {
        List<SkuHasStockVo> vos = new ArrayList<>();
        SkuHasStockVo vo = new SkuHasStockVo();
        vo.setSkuId(1L);
        vo.setHasStock(true);
        vos.add(vo);
        when(wareSkuService.getSkusHasStock(anyList())).thenReturn(vos);

        mockMvc.perform(post("/ware/waresku/hasstock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2]"))
                .andExpect(status().isOk());
    }

    @Test
    void testList() throws Exception {
        PageUtils pageUtils = new PageUtils(new ArrayList<>(), 0, 10, 1);
        when(wareSkuService.queryPage(anyMap())).thenReturn(pageUtils);

        mockMvc.perform(get("/ware/waresku/list")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInfo() throws Exception {
        WareSkuEntity entity = new WareSkuEntity();
        entity.setId(1L);
        entity.setSkuId(100L);
        when(wareSkuService.getById(1L)).thenReturn(entity);

        mockMvc.perform(get("/ware/waresku/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSave() throws Exception {
        WareSkuEntity entity = new WareSkuEntity();
        entity.setSkuId(100L);
        entity.setWareId(1L);
        entity.setStock(100);
        when(wareSkuService.save(any(WareSkuEntity.class))).thenReturn(true);

        mockMvc.perform(post("/ware/waresku/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        WareSkuEntity entity = new WareSkuEntity();
        entity.setId(1L);
        entity.setStock(200);
        when(wareSkuService.updateById(any(WareSkuEntity.class))).thenReturn(true);

        mockMvc.perform(post("/ware/waresku/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        when(wareSkuService.removeByIds(anyList())).thenReturn(true);

        mockMvc.perform(post("/ware/waresku/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
