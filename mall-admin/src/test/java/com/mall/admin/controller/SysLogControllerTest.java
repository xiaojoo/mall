package com.mall.admin.controller;

import com.mall.admin.dao.*;
import com.mall.admin.service.SysLogService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SysLogController.class)
class SysLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SysScheduleJobLogDao sysScheduleJobLogDao;

    @MockitoBean
    private SysScheduleJobDao sysScheduleJobDao;

    @MockitoBean
    private SysLogService sysLogService;

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

    @Test
    void testList() throws Exception {
        PageUtils page = new PageUtils(new ArrayList<>(), 0, 10, 1);
        when(sysLogService.queryPage(anyMap())).thenReturn(page);

        mockMvc.perform(get("/sys/log/list")
                        .header("token", "test-token")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testDelete() throws Exception {
        when(sysLogService.removeByIds(anyCollection())).thenReturn(true);

        mockMvc.perform(post("/sys/log/delete")
                        .header("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2, 3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysLogService).removeByIds(anyCollection());
    }
}
