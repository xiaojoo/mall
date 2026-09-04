package com.mall.member.controller;

import com.mall.common.utils.PageUtils;
import com.mall.member.entity.MemberCollectSubjectEntity;
import com.mall.member.service.MemberCollectSubjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import com.mall.member.feign.CouponFeignService;
import com.mall.member.service.IntegrationChangeHistoryService;
import com.mall.member.service.GrowthChangeHistoryService;
import com.mall.member.service.MemberStatisticsInfoService;
import com.mall.member.service.MemberLoginLogService;
import com.mall.member.service.MemberCollectSpuService;
import com.mall.member.service.MemberReceiveAddressService;
import com.mall.member.service.MemberLevelService;
import com.mall.member.service.MemberService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberCollectSubjectController.class)
class MemberCollectSubjectControllerTest {

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private MemberLevelService memberLevelService;

    @MockitoBean
    private MemberReceiveAddressService memberReceiveAddressService;

    @MockitoBean
    private MemberCollectSpuService memberCollectSpuService;

    @MockitoBean
    private MemberCollectSubjectService memberCollectSubjectService;

    @MockitoBean
    private MemberLoginLogService memberLoginLogService;

    @MockitoBean
    private MemberStatisticsInfoService memberStatisticsInfoService;

    @MockitoBean
    private GrowthChangeHistoryService growthChangeHistoryService;

    @MockitoBean
    private IntegrationChangeHistoryService integrationChangeHistoryService;

    @MockitoBean
    private CouponFeignService couponFeignService;


    @Autowired
    private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testList() throws Exception {
        PageUtils pageUtils = new PageUtils(new ArrayList<>(), 0, 10, 1);
        when(memberCollectSubjectService.queryPage(any())).thenReturn(pageUtils);

        mockMvc.perform(get("/api/member/membercollectsubject/list")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInfo() throws Exception {
        MemberCollectSubjectEntity entity = new MemberCollectSubjectEntity();
        entity.setId(1L);
        when(memberCollectSubjectService.getById(1L)).thenReturn(entity);

        mockMvc.perform(get("/api/member/membercollectsubject/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSave() throws Exception {
        MemberCollectSubjectEntity entity = new MemberCollectSubjectEntity();

        mockMvc.perform(post("/api/member/membercollectsubject/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        MemberCollectSubjectEntity entity = new MemberCollectSubjectEntity();
        entity.setId(1L);

        mockMvc.perform(post("/api/member/membercollectsubject/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(post("/api/member/membercollectsubject/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
