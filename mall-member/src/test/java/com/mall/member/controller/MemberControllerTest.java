package com.mall.member.controller;

import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import com.mall.member.entity.MemberEntity;
import com.mall.member.feign.CouponFeignService;
import com.mall.member.service.MemberService;
import com.mall.member.vo.MemberUserLoginVo;
import com.mall.member.vo.MemberUserRegisterVo;
import com.mall.member.vo.SocialUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.mall.member.service.IntegrationChangeHistoryService;
import com.mall.member.service.GrowthChangeHistoryService;
import com.mall.member.service.MemberStatisticsInfoService;
import com.mall.member.service.MemberLoginLogService;
import com.mall.member.service.MemberCollectSubjectService;
import com.mall.member.service.MemberCollectSpuService;
import com.mall.member.service.MemberReceiveAddressService;
import com.mall.member.service.MemberLevelService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = MemberController.class,
    properties = "spring.session.store-type=none"
)
class MemberControllerTest {

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
        when(memberService.queryPage(any())).thenReturn(pageUtils);

        mockMvc.perform(get("/api/member/member/list")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInfo() throws Exception {
        MemberEntity member = new MemberEntity();
        member.setId(1L);
        member.setUsername("testuser");
        when(memberService.getById(1L)).thenReturn(member);

        mockMvc.perform(get("/api/member/member/info/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSave() throws Exception {
        MemberEntity member = new MemberEntity();
        member.setUsername("newuser");

        mockMvc.perform(post("/api/member/member/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testUpdate() throws Exception {
        MemberEntity member = new MemberEntity();
        member.setId(1L);
        member.setUsername("updated");

        mockMvc.perform(post("/api/member/member/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(post("/api/member/member/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testLoginSuccess() throws Exception {
        MemberEntity member = new MemberEntity();
        member.setUsername("testuser");
        MemberUserLoginVo vo = new MemberUserLoginVo();
        vo.setLoginUser("testuser");
        vo.setPassword("123456");
        when(memberService.login(any(MemberUserLoginVo.class))).thenReturn(member);

        mockMvc.perform(post("/api/member/member/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testLoginFailure() throws Exception {
        MemberUserLoginVo vo = new MemberUserLoginVo();
        vo.setLoginUser("testuser");
        vo.setPassword("wrong");
        when(memberService.login(any(MemberUserLoginVo.class))).thenReturn(null);

        mockMvc.perform(post("/api/member/member/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(15003));
    }

    @Test
    void testRegisterSuccess() throws Exception {
        MemberUserRegisterVo vo = new MemberUserRegisterVo();
        vo.setUserName("newuser");
        vo.setPassword("123456");
        vo.setPhone("13800138000");

        mockMvc.perform(post("/api/member/member/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testOauthLoginSuccess() throws Exception {
        SocialUser socialUser = new SocialUser();
        socialUser.setAccess_token("token123");
        socialUser.setUid("uid123");
        MemberEntity member = new MemberEntity();
        member.setUsername("socialuser");
        when(memberService.login(any(SocialUser.class))).thenReturn(member);

        mockMvc.perform(post("/api/member/member/oauth2/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(socialUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
