package com.mall.member.controller;

import com.mall.member.service.MemberService;
import com.mall.member.service.MemberLevelService;
import com.mall.member.service.MemberReceiveAddressService;
import com.mall.member.service.MemberCollectSpuService;
import com.mall.member.service.MemberCollectSubjectService;
import com.mall.member.service.MemberLoginLogService;
import com.mall.member.service.MemberStatisticsInfoService;
import com.mall.member.service.GrowthChangeHistoryService;
import com.mall.member.service.IntegrationChangeHistoryService;
import com.mall.member.feign.CouponFeignService;
import com.mall.common.jwt.MemberJwtUtils;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootConfiguration
@Import({
    MemberController.class,
    MemberLevelController.class,
    MemberReceiveAddressController.class,
    MemberCollectSpuController.class,
    MemberCollectSubjectController.class,
    MemberLoginLogController.class,
    MemberStatisticsInfoController.class,
    GrowthChangeHistoryController.class,
    IntegrationChangeHistoryController.class
})
class TestConfig implements WebMvcConfigurer {

    // @WebMvcTest 切片不加载 AutoConfiguration，这里手动补上控制器新增的 JWT 依赖
    @Bean
    public MemberJwtUtils memberJwtUtils() {
        return new MemberJwtUtils();
    }
}
