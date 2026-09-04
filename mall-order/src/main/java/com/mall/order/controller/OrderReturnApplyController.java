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

import com.mall.order.entity.OrderReturnApplyEntity;
import com.mall.order.service.OrderReturnApplyService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import com.mall.common.dto.PageQueryDTO;



/**
 * 订单退货申请
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:32:06
 */
@RestController
@RequestMapping("/api/order/orderreturnapply")
@RequiredArgsConstructor
public class OrderReturnApplyController {

    private final OrderReturnApplyService orderReturnApplyService;

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params){
        PageUtils page = orderReturnApplyService.queryPage(params);

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<OrderReturnApplyEntity> info(@PathVariable Long id){
		OrderReturnApplyEntity orderReturnApply = orderReturnApplyService.getById(id);

        return Result.success(orderReturnApply);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody OrderReturnApplyEntity orderReturnApply){
		orderReturnApplyService.save(orderReturnApply);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody OrderReturnApplyEntity orderReturnApply){
		orderReturnApplyService.updateById(orderReturnApply);

        return Result.success();
    }

    /**
     * 审核通过
     */
    @PostMapping("/approve")
    public Result<Void> approve(@RequestParam("id") Long id){
        orderReturnApplyService.approve(id);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids){
		orderReturnApplyService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
