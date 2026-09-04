package com.mall.product.app;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mall.product.entity.SpuCommentReportEntity;
import com.mall.product.service.SpuCommentReportService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;

/**
 * 商品评论举报
 *
 * @author sunxiaojie
 * @date 2024-08-01 12:44:34
 */
@RestController
@RequestMapping("api/product/spucommentreport")
@RequiredArgsConstructor
public class SpuCommentReportController {

    private final SpuCommentReportService spuCommentReportService;

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params){
        PageUtils page = spuCommentReportService.queryPage(params);

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<SpuCommentReportEntity> info(@PathVariable Long id){
		SpuCommentReportEntity spuCommentReport = spuCommentReportService.getById(id);

        return Result.success(spuCommentReport);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody SpuCommentReportEntity spuCommentReport){
        if (spuCommentReport.getCreateTime() == null) {
            spuCommentReport.setCreateTime(new Date());
        }
		spuCommentReportService.save(spuCommentReport);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody SpuCommentReportEntity spuCommentReport){
		spuCommentReportService.updateById(spuCommentReport);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids){
		spuCommentReportService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
