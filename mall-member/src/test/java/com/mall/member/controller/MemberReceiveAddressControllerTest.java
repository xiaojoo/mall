package com.mall.member.controller;

import com.mall.common.jwt.MemberJwtUtils;
import com.mall.common.utils.PageUtils;
import com.mall.member.entity.MemberReceiveAddressEntity;
import com.mall.member.service.MemberReceiveAddressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import com.mall.member.feign.CouponFeignService;
import com.mall.member.service.IntegrationChangeHistoryService;
import com.mall.member.service.GrowthChangeHistoryService;
import com.mall.member.service.MemberStatisticsInfoService;
import com.mall.member.service.MemberLoginLogService;
import com.mall.member.service.MemberCollectSubjectService;
import com.mall.member.service.MemberCollectSpuService;
import com.mall.member.service.MemberLevelService;
import com.mall.member.service.MemberService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberReceiveAddressController.class)
class MemberReceiveAddressControllerTest {

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

    @Autowired
    private MemberJwtUtils memberJwtUtils;

        private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testGetAddress() throws Exception {
        MemberReceiveAddressEntity addr = new MemberReceiveAddressEntity();
        addr.setId(1L);
        addr.setMemberId(100L);
        addr.setName("张三");
        when(memberReceiveAddressService.getAddress(100L)).thenReturn(List.of(addr));

        mockMvc.perform(get("/api/member/memberreceiveaddress/100/address"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("张三"));
    }

    @Test
    void testList() throws Exception {
        PageUtils pageUtils = new PageUtils(new ArrayList<>(), 0, 10, 1);
        when(memberReceiveAddressService.queryPage(any())).thenReturn(pageUtils);

        mockMvc.perform(get("/api/member/memberreceiveaddress/list")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInfo() throws Exception {
        MemberReceiveAddressEntity entity = new MemberReceiveAddressEntity();
        entity.setId(1L);
        when(memberReceiveAddressService.getById(1L)).thenReturn(entity);

        mockMvc.perform(get("/api/member/memberreceiveaddress/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSave() throws Exception {
        MemberReceiveAddressEntity entity = new MemberReceiveAddressEntity();
        entity.setName("张三");
        // save/update 现在要求会员 JWT（memberId 以 JWT 为准）
        String token = memberJwtUtils.generateToken(1L);

        mockMvc.perform(post("/api/member/memberreceiveaddress/save")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        MemberReceiveAddressEntity entity = new MemberReceiveAddressEntity();
        entity.setId(1L);
        // save/update 现在要求会员 JWT（memberId 以 JWT 为准）
        String token = memberJwtUtils.generateToken(1L);

        mockMvc.perform(post("/api/member/memberreceiveaddress/update")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(post("/api/member/memberreceiveaddress/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
