package com.mall.member.controller;

import com.mall.common.jwt.MemberJwtUtils;
import com.mall.member.entity.MemberCollectSpuEntity;
import com.mall.member.service.MemberCollectSpuService;
import com.mall.member.vo.CollectSpuVo;
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
import com.mall.member.service.MemberReceiveAddressService;
import com.mall.member.service.MemberLevelService;
import com.mall.member.service.MemberService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberCollectSpuController.class)
class MemberCollectSpuControllerTest {

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

    private String token() {
        return memberJwtUtils.generateToken(1L);
    }

    @Test
    void testList() throws Exception {
        CollectSpuVo vo = new CollectSpuVo();
        vo.setId(1L);
        vo.setSpuId(100L);
        vo.setSpuName("量子计算芯片 Q-9000");
        vo.setSkuId(1001L);
        when(memberCollectSpuService.listCollectByMember(1L))
                .thenReturn(new ArrayList<>(Collections.singletonList(vo)));

        mockMvc.perform(get("/api/member/membercollectspu/list")
                        .header("token", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].spuId").value("100"))
                .andExpect(jsonPath("$.data[0].id").isString());
    }

    @Test
    void testListWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/member/membercollectspu/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void testStatus() throws Exception {
        when(memberCollectSpuService.isCollected(1L, 100L)).thenReturn(true);

        mockMvc.perform(get("/api/member/membercollectspu/status/100")
                        .header("token", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void testStatusWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/member/membercollectspu/status/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    void testCount() throws Exception {
        when(memberCollectSpuService.countByMember(1L)).thenReturn(3L);

        mockMvc.perform(get("/api/member/membercollectspu/count")
                        .header("token", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    void testCountWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/member/membercollectspu/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    void testSave() throws Exception {
        MemberCollectSpuEntity entity = new MemberCollectSpuEntity();
        entity.setSpuId(100L);
        entity.setSpuName("量子计算芯片 Q-9000");
        when(memberCollectSpuService.saveCollect(anyLong(), anyLong(), any(), any(), any())).thenReturn(1L);

        mockMvc.perform(post("/api/member/membercollectspu/save")
                        .header("token", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSaveWithoutSpuId() throws Exception {
        MemberCollectSpuEntity entity = new MemberCollectSpuEntity();

        mockMvc.perform(post("/api/member/membercollectspu/save")
                        .header("token", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(post("/api/member/membercollectspu/delete")
                        .header("token", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDeleteBySpu() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("spuId", 100L);

        mockMvc.perform(post("/api/member/membercollectspu/deleteBySpu")
                        .header("token", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
