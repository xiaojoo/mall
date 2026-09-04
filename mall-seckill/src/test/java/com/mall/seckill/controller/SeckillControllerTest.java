package com.mall.seckill.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.mall.seckill.controller.api.SeckillApiController;
import com.mall.seckill.service.SeckillService;
import com.mall.seckill.to.SeckillSkuRedisTo;
import com.mall.seckill.vo.SkuInfoVo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({SeckillController.class, SeckillApiController.class})
class SeckillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeckillService seckillService;

    @MockitoBean
    private com.mall.common.jwt.MemberJwtUtils memberJwtUtils;

    @Test
    void getCurrentSeckillSkus() throws Exception {
        List<SeckillSkuRedisTo> vos = new ArrayList<>();
        SeckillSkuRedisTo to = new SeckillSkuRedisTo();
        to.setPromotionId(1L);
        to.setSkuId(100L);
        to.setSeckillPrice(new BigDecimal("9.99"));
        to.setSeckillCount(100);
        to.setSeckillLimit(1);
        vos.add(to);
        when(seckillService.getCurrentSeckillSkus()).thenReturn(vos);

        mockMvc.perform(get("/getCurrentSeckillSkus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].promotionId").value(1));
    }

    @Test
    void getSkuSeckilInfo() throws Exception {
        SeckillSkuRedisTo to = new SeckillSkuRedisTo();
        to.setPromotionId(1L);
        to.setSkuId(100L);
        to.setSeckillPrice(new BigDecimal("9.99"));
        SkuInfoVo skuInfo = new SkuInfoVo();
        skuInfo.setSkuId(100L);
        skuInfo.setSkuName("Test SKU");
        to.setSkuInfo(skuInfo);
        when(seckillService.getSkuSeckilInfo(100L)).thenReturn(to);

        mockMvc.perform(get("/sku/seckill/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.skuId").value(100));
    }

    @Test
    void seckill() throws Exception {
        when(memberJwtUtils.extractToken(any())).thenReturn("mock-token");
        when(memberJwtUtils.parseMemberId("mock-token")).thenReturn(1L);
        when(seckillService.kill(anyString(), anyString(), anyInt())).thenReturn("ORDER_SN_001");

        mockMvc.perform(post("/api/seckill/kill")
                        .param("killId", "2-65")
                        .param("key", "abc123")
                        .param("num", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("ORDER_SN_001"));
    }
}
