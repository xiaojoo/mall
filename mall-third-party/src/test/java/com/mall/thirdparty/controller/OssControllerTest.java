package com.mall.thirdparty.controller;

import java.util.Date;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PolicyConditions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OssController.class)
class OssControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OSS ossClient;

    @Value("${spring.cloud.alicloud.oss.endpoint:}")
    private String endpoint;

    @Value("${spring.cloud.alicloud.oss.bucket:}")
    private String bucket;

    @Value("${spring.cloud.alicloud.access-key:}")
    private String accessId;

    @Test
    void policy() throws Exception {
        when(ossClient.generatePostPolicy(any(Date.class), any(PolicyConditions.class)))
                .thenReturn("{\"expiration\":\"2025-01-01T00:00:00.000Z\",\"conditions\":[[\"content-length-range\",0,1048576000]]}");
        when(ossClient.calculatePostSignature(anyString())).thenReturn("mockSignature");

        mockMvc.perform(get("/oss/policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
