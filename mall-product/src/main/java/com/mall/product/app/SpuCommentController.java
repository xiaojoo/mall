package com.mall.product.app;

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

import com.mall.product.entity.SpuCommentEntity;
import com.mall.product.service.SpuCommentService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.mall.common.dto.PageQueryDTO;



/**
 * 商品评价
 *
 * @author sunxiaojie
 * @date 2024-08-01 12:44:34
 */
@RestController
@RequestMapping("api/product/spucomment")
@RequiredArgsConstructor
public class SpuCommentController {

    private final SpuCommentService spuCommentService;

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params){
        PageUtils page = spuCommentService.queryPage(params);

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<SpuCommentEntity> info(@PathVariable Long id){
		SpuCommentEntity spuComment = spuCommentService.getById(id);

        return Result.success(spuComment);
    }

    /**
     * 保存（业务规则见 SpuCommentService#saveComment：
     * 每人每商品最多 2 条，第 2 条自动为追加评论，超限抛 RRException 由全局处理器转 Result.fail）
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody SpuCommentEntity spuComment, HttpServletRequest request){
        spuCommentService.saveComment(spuComment, request);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody SpuCommentEntity spuComment){
		spuCommentService.updateById(spuComment);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids){
		spuCommentService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
