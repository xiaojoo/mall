package com.mall.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.admin.dao.*;
import com.mall.admin.entity.SysMenuEntity;
import com.mall.admin.service.SysMenuService;
import com.mall.admin.service.SysUserTokenService;
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

@WebMvcTest(SysMenuController.class)
class SysMenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SysScheduleJobLogDao sysScheduleJobLogDao;

    @MockitoBean
    private SysScheduleJobDao sysScheduleJobDao;

    @MockitoBean
    private SysMenuService sysMenuService;

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
        List<SysMenuEntity> menus = new ArrayList<>();
        SysMenuEntity menu = new SysMenuEntity();
        menu.setMenuId(1L);
        menu.setName("系统管理");
        menus.add(menu);

        when(sysMenuService.queryNotButtonList()).thenReturn(menus);

        mockMvc.perform(get("/sys/menu/list")
                        .header("token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("系统管理"));
    }

    @Test
    void testNav() throws Exception {
        List<SysMenuEntity> menus = new ArrayList<>();
        SysMenuEntity menu = new SysMenuEntity();
        menu.setMenuId(2L);
        menu.setName("用户管理");
        menus.add(menu);

        when(sysMenuService.getUserMenuList(1L)).thenReturn(menus);

        mockMvc.perform(get("/sys/menu/nav")
                        .header("token", "test-token")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("用户管理"));
    }

    @Test
    void testInfo() throws Exception {
        SysMenuEntity menu = new SysMenuEntity();
        menu.setMenuId(1L);
        menu.setName("菜单详情");
        menu.setType(1);

        when(sysMenuService.getById(1L)).thenReturn(menu);

        mockMvc.perform(get("/sys/menu/info/1")
                        .header("token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("菜单详情"));
    }

    @Test
    void testSave() throws Exception {
        SysMenuEntity menu = new SysMenuEntity();
        menu.setName("新菜单");
        menu.setType(1);

        mockMvc.perform(post("/sys/menu/save")
                        .header("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menu)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysMenuService).save(any(SysMenuEntity.class));
    }

    @Test
    void testUpdate() throws Exception {
        SysMenuEntity menu = new SysMenuEntity();
        menu.setMenuId(1L);
        menu.setName("更新菜单");

        mockMvc.perform(post("/sys/menu/update")
                        .header("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menu)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysMenuService).updateById(any(SysMenuEntity.class));
    }

    @Test
    void testDelete() throws Exception {
        doNothing().when(sysMenuService).delete(1L);

        mockMvc.perform(post("/sys/menu/delete/1")
                        .header("token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysMenuService).delete(1L);
    }
}
