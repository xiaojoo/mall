package com.mall.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.admin.dao.*;
import com.mall.admin.entity.SysRoleEntity;
import com.mall.admin.service.SysRoleMenuService;
import com.mall.admin.service.SysRoleService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SysRoleController.class)
class SysRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SysScheduleJobLogDao sysScheduleJobLogDao;

    @MockitoBean
    private SysScheduleJobDao sysScheduleJobDao;

    @MockitoBean
    private SysRoleService sysRoleService;

    @MockitoBean
    private SysRoleMenuService sysRoleMenuService;

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
        when(sysRoleService.queryPage(anyMap())).thenReturn(page);

        mockMvc.perform(get("/sys/role/list")
                        .header("token", "test-token")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInfo() throws Exception {
        SysRoleEntity role = new SysRoleEntity();
        role.setRoleId(1L);
        role.setRoleName("管理员");
        when(sysRoleService.getById(1L)).thenReturn(role);
        when(sysRoleMenuService.queryMenuIdList(1L)).thenReturn(List.of(1L, 2L));

        mockMvc.perform(get("/sys/role/info/1")
                        .header("token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roleName").value("管理员"));
    }

    @Test
    void testSave() throws Exception {
        SysRoleEntity role = new SysRoleEntity();
        role.setRoleName("新角色");

        mockMvc.perform(post("/sys/role/save")
                        .header("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysRoleService).saveRole(any(SysRoleEntity.class));
    }

    @Test
    void testUpdate() throws Exception {
        SysRoleEntity role = new SysRoleEntity();
        role.setRoleId(1L);
        role.setRoleName("更新角色");

        mockMvc.perform(post("/sys/role/update")
                        .header("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysRoleService).updateRole(any(SysRoleEntity.class));
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(post("/sys/role/delete")
                        .header("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysRoleService).deleteBatch(argThat(ids -> ids.length == 2));
    }

    @Test
    void testSelect() throws Exception {
        List<SysRoleEntity> roles = new ArrayList<>();
        SysRoleEntity role = new SysRoleEntity();
        role.setRoleId(1L);
        role.setRoleName("管理员");
        roles.add(role);
        when(sysRoleService.list()).thenReturn(roles);

        mockMvc.perform(get("/sys/role/select")
                        .header("token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].roleName").value("管理员"));
    }
}
