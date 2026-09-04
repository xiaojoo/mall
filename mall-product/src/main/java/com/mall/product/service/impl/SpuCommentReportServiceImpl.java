package com.mall.product.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.product.dao.SpuCommentReportDao;
import com.mall.product.entity.SpuCommentReportEntity;
import com.mall.product.service.SpuCommentReportService;


@Service("spuCommentReportService")
public class SpuCommentReportServiceImpl extends ServiceImpl<SpuCommentReportDao, SpuCommentReportEntity> implements SpuCommentReportService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<SpuCommentReportEntity> wrapper = new LambdaQueryWrapper<>();
        // 支持按被举报评论过滤
        Object commentId = params.get("commentId");
        if (commentId != null && !String.valueOf(commentId).isBlank()) {
            wrapper.eq(SpuCommentReportEntity::getCommentId, commentId);
        }
        Object status = params.get("status");
        if (status != null && !String.valueOf(status).isBlank()) {
            wrapper.eq(SpuCommentReportEntity::getStatus, status);
        }
        wrapper.orderByDesc(SpuCommentReportEntity::getCreateTime);
        IPage<SpuCommentReportEntity> page = this.page(
                new Query<SpuCommentReportEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

}
