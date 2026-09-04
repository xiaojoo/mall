package com.mall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.ware.entity.WareInfoEntity;
import com.mall.ware.vo.FareVo;

import java.util.Map;

/**
 * 仓库信息
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:50:20
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    FareVo getFare(Long addrId);
}

