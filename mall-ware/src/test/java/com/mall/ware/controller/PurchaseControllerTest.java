package com.mall.ware.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.mall.ware.entity.PurchaseEntity;
import com.mall.ware.service.PurchaseService;
import com.mall.ware.vo.MergeVo;
import com.mall.ware.vo.PurchaseDoneVo;
import com.mall.ware.vo.PurchaseItemDoneVo;
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
    value = PurchaseController.class,
    excludeAutoConfiguration = {DataSourceAutoConfiguration.class, RabbitAutoConfiguration.class}
)
class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PurchaseService purchaseService;

    @Test
    void finish() throws Exception {
        PurchaseDoneVo doneVo = new PurchaseDoneVo();
        doneVo.setId(1L);
        List<PurchaseItemDoneVo> items = new ArrayList<>();
        PurchaseItemDoneVo item = new PurchaseItemDoneVo();
        item.setItemId(1L);
        item.setStatus(3);
        items.add(item);
        doneVo.setItems(items);
        doNothing().when(purchaseService).done(any(PurchaseDoneVo.class));

        mockMvc.perform(post("/ware/purchase/done")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(doneVo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void received() throws Exception {
        List<Long> ids = Arrays.asList(1L, 2L);
        doNothing().when(purchaseService).received(anyList());

        mockMvc.perform(post("/ware/purchase/received")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void merge() throws Exception {
        MergeVo mergeVo = new MergeVo();
        mergeVo.setPurchaseId(1L);
        mergeVo.setItems(Arrays.asList(1L, 2L, 3L));
        doNothing().when(purchaseService).mergePurchase(any(MergeVo.class));

        mockMvc.perform(post("/ware/purchase/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(mergeVo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void unreceivelist() throws Exception {
        PageUtils pageUtils = new PageUtils(new ArrayList<>(), 0, 10, 1);
        when(purchaseService.queryPageUnreceivePurchase(anyMap())).thenReturn(pageUtils);

        mockMvc.perform(get("/ware/purchase/unreceive/list")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testList() throws Exception {
        PageUtils pageUtils = new PageUtils(new ArrayList<>(), 0, 10, 1);
        when(purchaseService.queryPage(anyMap())).thenReturn(pageUtils);

        mockMvc.perform(get("/ware/purchase/list")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInfo() throws Exception {
        PurchaseEntity entity = new PurchaseEntity();
        entity.setId(1L);
        when(purchaseService.getById(1L)).thenReturn(entity);

        mockMvc.perform(get("/ware/purchase/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSave() throws Exception {
        PurchaseEntity entity = new PurchaseEntity();
        entity.setAssigneeName("Test");
        when(purchaseService.save(any(PurchaseEntity.class))).thenReturn(true);

        mockMvc.perform(post("/ware/purchase/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        PurchaseEntity entity = new PurchaseEntity();
        entity.setId(1L);
        when(purchaseService.updateById(any(PurchaseEntity.class))).thenReturn(true);

        mockMvc.perform(post("/ware/purchase/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        when(purchaseService.removeByIds(anyList())).thenReturn(true);

        mockMvc.perform(post("/ware/purchase/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
