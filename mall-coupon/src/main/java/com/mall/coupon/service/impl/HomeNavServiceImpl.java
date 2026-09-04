package com.mall.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;
import com.mall.coupon.dao.HomeNavDao;
import com.mall.coupon.entity.HomeNavEntity;
import com.mall.coupon.service.HomeNavService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 首页快捷导航
 */
@Slf4j
@Service("homeNavService")
@RequiredArgsConstructor
public class HomeNavServiceImpl extends ServiceImpl<HomeNavDao, HomeNavEntity> implements HomeNavService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        // status 可能为空串/非法值，先安全解析（eq 的 value 是急切求值，直接 Integer.valueOf 会炸）
        Integer statusVal = null;
        Object status = params.get("status");
        if (status != null && StringUtils.isNotBlank(String.valueOf(status))) {
            try {
                statusVal = Integer.valueOf(String.valueOf(status));
            } catch (NumberFormatException ignored) {
                log.warn("homenav 状态参数非法，忽略: {}", status);
            }
        }
        LambdaQueryWrapper<HomeNavEntity> wrapper = new LambdaQueryWrapper<HomeNavEntity>()
                .like(StringUtils.isNotBlank(key), HomeNavEntity::getName, key)
                .eq(statusVal != null, HomeNavEntity::getShowStatus, statusVal)
                .orderByAsc(HomeNavEntity::getSort);
        IPage<HomeNavEntity> page = this.page(new Query<HomeNavEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    @Override
    public List<HomeNavEntity> listEnabled() {
        return this.list(new LambdaQueryWrapper<HomeNavEntity>()
                .eq(HomeNavEntity::getShowStatus, 1)
                .orderByAsc(HomeNavEntity::getSort));
    }
}
