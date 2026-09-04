package com.mall.search.service;

import com.mall.search.vo.CatalogVO;
import com.mall.search.vo.BrandVO;
import com.mall.search.vo.SearchParam;
import com.mall.search.vo.SearchResult;

import java.util.List;

public interface MallSearchService {
    SearchResult search(SearchParam param);
    
    List<String> getSuggestions(String keyword);
    
    List<String> getHotKeys();
    
    List<CatalogVO> getCatalogs();
    
    List<BrandVO> getBrands(Long catalogId);
}
