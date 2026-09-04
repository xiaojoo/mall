package com.mall.ware.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.mall.common.utils.Result;
import com.mall.common.utils.ResultUtil;
import com.mall.ware.feign.MemberFeignService;
import com.mall.ware.vo.FareVo;
import com.mall.ware.vo.MemberAddressVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.ware.dao.WareInfoDao;
import com.mall.ware.entity.WareInfoEntity;
import com.mall.ware.service.WareInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("wareInfoService")
@RequiredArgsConstructor
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    private final MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<WareInfoEntity> queryWrapper = new LambdaQueryWrapper<>();
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.eq(WareInfoEntity::getId, key).or()
                    .like(WareInfoEntity::getName, key)
                    .or().like(WareInfoEntity::getAddress, key)
                    .or().like(WareInfoEntity::getAreacode, key);
        }
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params), queryWrapper
        );
        return new PageUtils(page);
    }

    @Override
    public FareVo getFare(Long addrId) {
        FareVo fareVo = new FareVo();
        // 收获地址的详细信息（data 直接是地址实体，非 Map，直接反序列化）
        Result<Object> addrInfo = memberFeignService.info(addrId);
        log.info("[getFare] addrId={}, addrInfo={}", addrId, addrInfo);
        MemberAddressVo memberAddressVo = JSON.parseObject(
                JSON.toJSONString(addrInfo == null ? null : addrInfo.getData()),
                MemberAddressVo.class);
        log.info("[getFare] memberAddressVo={}", memberAddressVo);
        if (memberAddressVo != null) {
            String phone = memberAddressVo.getPhone();
            // 截取用户手机号码最后一位作为我们的运费计算
            String fare = phone.substring(phone.length() - 10, phone.length() - 8);
            BigDecimal bigDecimal = new BigDecimal(fare);
            fareVo.setFare(bigDecimal);
            fareVo.setAddress(memberAddressVo);
            return fareVo;
        }
        return null;
    }
}