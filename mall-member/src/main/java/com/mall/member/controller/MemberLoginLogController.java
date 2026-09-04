package com.mall.member.controller;

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

import com.mall.member.entity.MemberLoginLogEntity;
import com.mall.member.service.MemberLoginLogService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import com.mall.common.dto.PageQueryDTO;



/**
 * 会员登录记录
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:14:49
 */
@RestController
@RequestMapping("api/member/memberloginlog")
@RequiredArgsConstructor
public class MemberLoginLogController {

    private final MemberLoginLogService memberLoginLogService;

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params){
        PageUtils page = memberLoginLogService.queryPage(params);

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<MemberLoginLogEntity> info(@PathVariable Long id){
		MemberLoginLogEntity memberLoginLog = memberLoginLogService.getById(id);

        return Result.success(memberLoginLog);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody MemberLoginLogEntity memberLoginLog){
		memberLoginLogService.save(memberLoginLog);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody MemberLoginLogEntity memberLoginLog){
		memberLoginLogService.updateById(memberLoginLog);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids){
		memberLoginLogService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
