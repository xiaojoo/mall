package com.mall.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.admin.dao.*;
import com.mall.admin.entity.SysConfigEntity;
import com.mall.admin.service.SysConfigService;
import com.mall.admin.service.SysUserTokenService;
import com.mall.common.utils.PageUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.mall.admin.dao.SysScheduleJobLogDao;
import com.mall.admin.dao.SysScheduleJobDao;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SysConfigController.class)
class SysConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SysScheduleJobLogDao sysScheduleJobLogDao;

    @MockitoBean
    private SysScheduleJobDao sysScheduleJobDao;

    @MockitoBean
    private SysConfigService sysConfigService;

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
        when(sysConfigService.queryPage(anyMap())).thenReturn(page);

        mockMvc.perform(get("/sys/config/list")
                        .header("token", "test-token")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInfo() throws Exception {
        SysConfigEntity config = new SysConfigEntity();
        config.setId(1L);
        config.setParamKey("test.key");
        config.setParamValue("test.value");
        when(sysConfigService.getById(1L)).thenReturn(config);

        mockMvc.perform(get("/sys/config/info/1")
                        .header("token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.paramKey").value("test.key"));
    }

    @Test
    void testSave() throws Exception {
        SysConfigEntity config = new SysConfigEntity();
        config.setParamKey("new.key");
        config.setParamValue("new.value");

        mockMvc.perform(post("/sys/config/save")
                        .header("token", "test-token")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysConfigService).saveConfig(any(SysConfigEntity.class));
    }

    @Test
    void testUpdate() throws Exception {
        SysConfigEntity config = new SysConfigEntity();
        config.setId(1L);
        config.setParamKey("updated.key");

        mockMvc.perform(post("/sys/config/update")
                        .header("token", "test-token")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysConfigService).updateConfig(any(SysConfigEntity.class));
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(post("/sys/config/delete")
                        .header("token", "test-token")
                        .contentType("application/json")
                        .content("[1, 2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysConfigService).deleteBatch(argThat(ids -> ids.length == 2));
    }
}
