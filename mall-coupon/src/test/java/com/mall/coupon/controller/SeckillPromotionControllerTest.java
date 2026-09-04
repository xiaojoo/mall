package com.mall.coupon.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.mall.coupon.entity.SeckillPromotionEntity;
import com.mall.coupon.service.SeckillPromotionService;
import com.mall.common.utils.PageUtils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SeckillPromotionController.class, excludeAutoConfiguration = {
    DataSourceAutoConfiguration.class,
})
class SeckillPromotionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeckillPromotionService seckillPromotionService;

    @MockitoBean
    private com.mall.coupon.dao.SeckillPromotionDao seckillPromotionDao;
    @MockitoBean
    private com.mall.coupon.dao.CouponDao couponDao;
    @MockitoBean
    private com.mall.coupon.dao.CouponHistoryDao couponHistoryDao;
    @MockitoBean
    private com.mall.coupon.dao.CouponSpuCategoryRelationDao couponSpuCategoryRelationDao;
    @MockitoBean
    private com.mall.coupon.dao.CouponSpuRelationDao couponSpuRelationDao;
    @MockitoBean
    private com.mall.coupon.dao.FooterLinkDao footerLinkDao;
    @MockitoBean
    private com.mall.coupon.dao.HomeAdvDao homeAdvDao;
    @MockitoBean
    private com.mall.coupon.dao.HomeCarouselDao homeCarouselDao;
    @MockitoBean
    private com.mall.coupon.dao.HomeNavDao homeNavDao;
    @MockitoBean
    private com.mall.coupon.dao.HomeSubjectDao homeSubjectDao;
    @MockitoBean
    private com.mall.coupon.dao.HomeSubjectSpuDao homeSubjectSpuDao;
    @MockitoBean
    private com.mall.coupon.dao.MemberPriceDao memberPriceDao;
    @MockitoBean
    private com.mall.coupon.dao.PromoDao promoDao;
    @MockitoBean
    private com.mall.coupon.dao.SeckillSessionDao seckillSessionDao;
    @MockitoBean
    private com.mall.coupon.dao.SeckillSkuNoticeDao seckillSkuNoticeDao;
    @MockitoBean
    private com.mall.coupon.dao.SeckillSkuRelationDao seckillSkuRelationDao;
    @MockitoBean
    private com.mall.coupon.dao.SkuFullReductionDao skuFullReductionDao;
    @MockitoBean
    private com.mall.coupon.dao.SkuLadderDao skuLadderDao;
    @MockitoBean
    private com.mall.coupon.dao.SpuBoundsDao spuBoundsDao;
    @MockitoBean
    private com.mall.coupon.dao.TickerDao tickerDao;

    @Test
    void testList() throws Exception {
        PageUtils pageUtils = new PageUtils(new ArrayList<>(), 0, 10, 1);
        when(seckillPromotionService.queryPage(anyMap())).thenReturn(pageUtils);

        mockMvc.perform(get("/coupon/seckillpromotion/list")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInfo() throws Exception {
        SeckillPromotionEntity entity = new SeckillPromotionEntity();
        entity.setId(1L);
        entity.setTitle("Test Promotion");
        when(seckillPromotionService.getById(1L)).thenReturn(entity);

        mockMvc.perform(get("/coupon/seckillpromotion/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSave() throws Exception {
        SeckillPromotionEntity entity = new SeckillPromotionEntity();
        entity.setTitle("New Promotion");
        when(seckillPromotionService.save(any(SeckillPromotionEntity.class))).thenReturn(true);

        mockMvc.perform(post("/coupon/seckillpromotion/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        SeckillPromotionEntity entity = new SeckillPromotionEntity();
        entity.setId(1L);
        entity.setTitle("Updated Promotion");
        when(seckillPromotionService.updateById(any(SeckillPromotionEntity.class))).thenReturn(true);

        mockMvc.perform(post("/coupon/seckillpromotion/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        when(seckillPromotionService.removeByIds(anyList())).thenReturn(true);

        mockMvc.perform(post("/coupon/seckillpromotion/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
