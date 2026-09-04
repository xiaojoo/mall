package com.mall.search.controller.api;

import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import com.mall.search.vo.CatalogVO;
import com.mall.search.vo.BrandVO;
import com.mall.search.vo.SearchParam;
import com.mall.search.vo.SearchResult;
import com.mall.search.service.MallSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * 搜索 API 接口 (前后端分离)
 * 
 * @author sunxiaojie
 * @date 2024-08-01
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchApiController {
    
    private final MallSearchService mallSearchService;
    
    /**
     * 搜索商品
     */
    @GetMapping("/sku")
    public Result<SearchResult> search(SearchParam param) {
        SearchResult result = mallSearchService.search(param);
        return Result.success( result);
    }
    
    /**
     * 获取搜索建议
     */
    @GetMapping("/suggest")
    public Result<List<String>> suggest(@RequestParam("keyword") String keyword) {
        List<String> suggestions = mallSearchService.getSuggestions(keyword);
        return Result.success( suggestions);
    }
    
    /**
     * 获取热门搜索
     */
    @GetMapping("/hot")
    public Result<List<String>> hot() {
        List<String> hotKeys = mallSearchService.getHotKeys();
        return Result.success( hotKeys);
    }
    
    /**
     * 获取分类
     */
    @GetMapping("/catalog")
    public Result<List<CatalogVO>> getCatalog() {
        List<CatalogVO> catalogs = mallSearchService.getCatalogs();
        return Result.success( catalogs);
    }
    
    /**
     * 获取品牌
     */
    @GetMapping("/brand")
    public Result<List<BrandVO>> getBrand(@RequestParam("catalogId") Long catalogId) {
        List<BrandVO> brands = mallSearchService.getBrands(catalogId);
        return Result.success( brands);
    }
}
