package com.mall.order.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mall.order.entity.OrderReturnReasonEntity;
import com.mall.order.service.OrderReturnReasonService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import com.mall.common.dto.PageQueryDTO;



/**
 * 退货原因
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:32:06
 */
@RestController
@RequestMapping("order/orderreturnreason")
@RequiredArgsConstructor
public class OrderReturnReasonController {

    private final OrderReturnReasonService orderReturnReasonService;

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params){
        PageUtils page = orderReturnReasonService.queryPage(params);

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<OrderReturnReasonEntity> info(@PathVariable Long id){
		OrderReturnReasonEntity orderReturnReason = orderReturnReasonService.getById(id);

        return Result.success(orderReturnReason);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody OrderReturnReasonEntity orderReturnReason){
		orderReturnReasonService.save(orderReturnReason);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody OrderReturnReasonEntity orderReturnReason){
		orderReturnReasonService.updateById(orderReturnReason);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids){
		orderReturnReasonService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
