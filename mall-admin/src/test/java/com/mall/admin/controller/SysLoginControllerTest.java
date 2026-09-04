package com.mall.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.admin.dao.*;
import com.mall.admin.entity.SysUserEntity;
import com.mall.admin.entity.SysUserTokenEntity;
import com.mall.admin.service.SysUserTokenService;
import com.mall.admin.service.SysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.mall.admin.dao.SysScheduleJobLogDao;
import com.mall.admin.dao.SysScheduleJobDao;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SysLoginController.class)
class SysLoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SysScheduleJobLogDao sysScheduleJobLogDao;

    @MockitoBean
    private SysScheduleJobDao sysScheduleJobDao;

    @MockitoBean
    private SysUserService sysUserService;

    @MockitoBean
    private SysUserTokenService sysUserTokenService;

    @MockitoBean private SysConfigDao sysConfigDao;
    @MockitoBean private SysLogDao sysLogDao;
    @MockitoBean private SysMenuDao sysMenuDao;
    @MockitoBean private SysOssDao sysOssDao;
    @MockitoBean private SysRoleDao sysRoleDao;
    @MockitoBean private SysRoleMenuDao sysRoleMenuDao;
    @MockitoBean private SysUserDao sysUserDao;
    @MockitoBean private SysUserRoleDao sysUserRoleDao;
    @MockitoBean private SysUserTokenDao sysUserTokenDao;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testLoginSuccess() throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodedPassword = encoder.encode("123456");

        SysUserEntity user = new SysUserEntity();
        user.setUserId(1L);
        user.setUsername("admin");
        user.setPassword(encodedPassword);
        user.setStatus(1);

        when(sysUserService.queryByUserName("admin")).thenReturn(user);
        when(sysUserTokenService.createToken(1L)).thenReturn(com.mall.common.utils.Result.success("token123"));

        Map<String, String> form = new HashMap<>();
        form.put("username", "admin");
        form.put("password", "123456");

        mockMvc.perform(post("/sys/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testLoginFailWrongPassword() throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        SysUserEntity user = new SysUserEntity();
        user.setUserId(1L);
        user.setUsername("admin");
        user.setPassword(encoder.encode("123456"));
        user.setStatus(1);

        when(sysUserService.queryByUserName("admin")).thenReturn(user);

        Map<String, String> form = new HashMap<>();
        form.put("username", "admin");
        form.put("password", "wrongpassword");

        mockMvc.perform(post("/sys/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("账号或密码不正确"));
    }

    @Test
    void testLoginFailUserNotFound() throws Exception {
        when(sysUserService.queryByUserName("unknown")).thenReturn(null);

        Map<String, String> form = new HashMap<>();
        form.put("username", "unknown");
        form.put("password", "123456");

        mockMvc.perform(post("/sys/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("账号或密码不正确"));
    }

    @Test
    void testLogout() throws Exception {
        // 控制器实现：按 token 查实体后 removeById（与旧版 logout(userId) API 不同）
        SysUserTokenEntity tokenEntity = new SysUserTokenEntity();
        tokenEntity.setUserId(1L);
        when(sysUserTokenService.getOne(any())).thenReturn(tokenEntity);

        mockMvc.perform(post("/sys/logout")
                        .header("token", "test-token")
                        .header("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysUserTokenService).removeById(1L);
    }
}
