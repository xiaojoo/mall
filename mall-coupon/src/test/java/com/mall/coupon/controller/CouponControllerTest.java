package com.mall.coupon.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.mall.coupon.entity.CouponEntity;
import com.mall.coupon.service.CouponService;
import com.mall.common.utils.PageUtils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.cloud.context.scope.GenericScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CouponController.class)
@Import({CouponController.class, CouponControllerTest.RefreshScopeConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class CouponControllerTest {

    @Configuration
    static class RefreshScopeConfig {
        @Bean
        public static BeanFactoryPostProcessor refreshScopeRegistrar() {
            return (ConfigurableListableBeanFactory beanFactory) -> {
                beanFactory.registerScope("refresh", new GenericScope());
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponService couponService;

    @MockitoBean
    private com.mall.common.jwt.MemberJwtUtils memberJwtUtils;

    @MockitoBean
    private com.mall.coupon.dao.CouponDao couponDao;
    @MockitoBean
    private com.mall.coupon.dao.CouponHistoryDao couponHistoryDao;
    @MockitoBean
    private com.mall.coupon.dao.CouponSpuCategoryRelationDao couponSpuCategoryRelationDao;
    @MockitoBean
    private com.mall.coupon.dao.CouponSpuRelationDao couponSpuRelationDao;
    @MockitoBean
    private com.mall.coupon.dao.HomeAdvDao homeAdvDao;
    @MockitoBean
    private com.mall.coupon.dao.HomeCarouselDao homeCarouselDao;
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
    void test1() throws Exception {
        mockMvc.perform(get("/coupon/coupon/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void memberList() throws Exception {
        mockMvc.perform(get("/coupon/coupon/member/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testList() throws Exception {
        PageUtils pageUtils = new PageUtils(new ArrayList<>(), 0, 10, 1);
        when(couponService.queryPage(anyMap())).thenReturn(pageUtils);

        mockMvc.perform(get("/coupon/coupon/list")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInfo() throws Exception {
        CouponEntity coupon = new CouponEntity();
        coupon.setId(1L);
        coupon.setCouponName("Test Coupon");
        when(couponService.getById(1L)).thenReturn(coupon);

        mockMvc.perform(get("/coupon/coupon/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSave() throws Exception {
        CouponEntity coupon = new CouponEntity();
        coupon.setCouponName("New Coupon");
        when(couponService.save(any(CouponEntity.class))).thenReturn(true);

        mockMvc.perform(post("/coupon/coupon/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(coupon)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        CouponEntity coupon = new CouponEntity();
        coupon.setId(1L);
        coupon.setCouponName("Updated Coupon");
        when(couponService.updateById(any(CouponEntity.class))).thenReturn(true);

        mockMvc.perform(post("/coupon/coupon/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(coupon)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        when(couponService.removeByIds(anyList())).thenReturn(true);

        mockMvc.perform(post("/coupon/coupon/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
