package com.mall.coupon.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.mall.coupon.entity.SeckillSessionEntity;
import com.mall.coupon.service.SeckillSessionService;
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

@WebMvcTest(value = SeckillSessionController.class, excludeAutoConfiguration = {
    DataSourceAutoConfiguration.class,
})
class SeckillSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeckillSessionService seckillSessionService;

    @MockitoBean
    private com.mall.coupon.dao.SeckillSessionDao seckillSessionDao;
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
    private com.mall.coupon.dao.SeckillPromotionDao seckillPromotionDao;
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
    void getLates3DaySession() throws Exception {
        List<SeckillSessionEntity> sessions = new ArrayList<>();
        SeckillSessionEntity session = new SeckillSessionEntity();
        session.setId(1L);
        session.setName("Test Session");
        sessions.add(session);
        when(seckillSessionService.getLates3DaySession()).thenReturn(sessions);

        mockMvc.perform(get("/coupon/seckillsession/Lates3DaySession"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void testList() throws Exception {
        PageUtils pageUtils = new PageUtils(new ArrayList<>(), 0, 10, 1);
        when(seckillSessionService.queryPage(anyMap())).thenReturn(pageUtils);

        mockMvc.perform(get("/coupon/seckillsession/list")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInfo() throws Exception {
        SeckillSessionEntity entity = new SeckillSessionEntity();
        entity.setId(1L);
        entity.setName("Test Session");
        when(seckillSessionService.getById(1L)).thenReturn(entity);

        mockMvc.perform(get("/coupon/seckillsession/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSave() throws Exception {
        SeckillSessionEntity entity = new SeckillSessionEntity();
        entity.setName("New Session");
        when(seckillSessionService.save(any(SeckillSessionEntity.class))).thenReturn(true);

        mockMvc.perform(post("/coupon/seckillsession/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        SeckillSessionEntity entity = new SeckillSessionEntity();
        entity.setId(1L);
        entity.setName("Updated Session");
        when(seckillSessionService.updateById(any(SeckillSessionEntity.class))).thenReturn(true);

        mockMvc.perform(post("/coupon/seckillsession/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        when(seckillSessionService.removeByIds(anyList())).thenReturn(true);

        mockMvc.perform(post("/coupon/seckillsession/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
