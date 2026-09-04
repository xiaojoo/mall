package com.mall.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.admin.dao.*;
import com.mall.admin.entity.SysOssEntity;
import com.mall.admin.service.SysOssService;
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
import java.util.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SysOssController.class)
class SysOssControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SysScheduleJobLogDao sysScheduleJobLogDao;

    @MockitoBean
    private SysScheduleJobDao sysScheduleJobDao;

    @MockitoBean
    private SysOssService sysOssService;

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
        when(sysOssService.queryPage(anyMap())).thenReturn(page);

        mockMvc.perform(get("/sys/oss/list")
                        .header("token", "test-token")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testInfo() throws Exception {
        SysOssEntity oss = new SysOssEntity();
        oss.setId(1L);
        oss.setUrl("https://example.com/file.jpg");
        oss.setCreateDate(new Date());
        when(sysOssService.getById(1L)).thenReturn(oss);

        mockMvc.perform(get("/sys/oss/info/1")
                        .header("token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").value("https://example.com/file.jpg"));
    }

    @Test
    void testSave() throws Exception {
        SysOssEntity oss = new SysOssEntity();
        oss.setUrl("https://example.com/new.jpg");

        mockMvc.perform(post("/sys/oss/save")
                        .header("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oss)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysOssService).save(any(SysOssEntity.class));
    }

    @Test
    void testDelete() throws Exception {
        when(sysOssService.removeByIds(anyCollection())).thenReturn(true);

        mockMvc.perform(post("/sys/oss/delete")
                        .header("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysOssService).removeByIds(anyCollection());
    }
}
