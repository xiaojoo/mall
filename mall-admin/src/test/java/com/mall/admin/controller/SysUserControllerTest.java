package com.mall.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.admin.dao.*;
import com.mall.admin.entity.SysUserEntity;
import com.mall.admin.service.SysUserRoleService;
import com.mall.admin.service.SysUserService;
import com.mall.admin.service.SysUserTokenService;
import com.mall.common.utils.PageUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.mall.admin.dao.SysScheduleJobLogDao;
import com.mall.admin.dao.SysScheduleJobDao;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SysUserController.class)
class SysUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SysScheduleJobLogDao sysScheduleJobLogDao;

    @MockitoBean
    private SysScheduleJobDao sysScheduleJobDao;

    @MockitoBean
    private SysUserService sysUserService;

    @MockitoBean
    private SysUserRoleService sysUserRoleService;

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
    void testList() throws Exception {
        PageUtils page = new PageUtils(new ArrayList<>(), 0, 10, 1);
        when(sysUserService.queryPage(anyMap())).thenReturn(page);

        mockMvc.perform(get("/sys/user/list")
                        .header("token", "test-token")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInfo() throws Exception {
        SysUserEntity user = new SysUserEntity();
        user.setUserId(1L);
        user.setUsername("admin");
        user.setEmail("admin@example.com");
        when(sysUserService.getById(1L)).thenReturn(user);
        when(sysUserRoleService.queryRoleIdList(1L)).thenReturn(List.of(1L, 2L));

        mockMvc.perform(get("/sys/user/info/1")
                        .header("token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void testSave() throws Exception {
        SysUserEntity user = new SysUserEntity();
        user.setUsername("newuser");
        user.setPassword("123456");

        mockMvc.perform(post("/sys/user/save")
                        .header("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysUserService).saveUser(any(SysUserEntity.class));
    }

    @Test
    void testUpdate() throws Exception {
        SysUserEntity user = new SysUserEntity();
        user.setUserId(1L);
        user.setUsername("updateduser");

        mockMvc.perform(post("/sys/user/update")
                        .header("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysUserService).updateUser(any(SysUserEntity.class));
    }

    @Test
    void testDeleteSuccess() throws Exception {
        mockMvc.perform(post("/sys/user/delete")
                        .header("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[2, 3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysUserService).deleteBatch(argThat(ids -> ids.length == 2));
    }

    @Test
    void testDeleteSuperAdmin() throws Exception {
        mockMvc.perform(post("/sys/user/delete")
                        .header("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统管理员不能删除"));

        verify(sysUserService, never()).deleteBatch(any());
    }

    @Test
    void testPassword() throws Exception {
        Map<String, String> form = new HashMap<>();
        form.put("password", "old123");
        form.put("newPassword", "new456");

        mockMvc.perform(post("/sys/user/password")
                        .header("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
