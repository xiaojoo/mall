package com.mall.product.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.mall.product.service.CategoryBrandRelationService;
import com.mall.product.vo.Catalog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.product.dao.CategoryDao;
import com.mall.product.entity.CategoryEntity;
import com.mall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;


@Service("categoryService")
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    final CategoryBrandRelationService categoryBrandRelationService;


    final RedisTemplate<String, String> redisTemplate;


    final RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new LambdaQueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // 1、查询出所有分类
//        List<CategoryEntity> entities = baseMapper.selectList(null);
//
//        // 2、组装成父子树形结构
//        // 2.1、找到所有一级分类
//        return entities.stream()
//                .filter(categoryEntity -> categoryEntity.getParentCid() == 0)
//                .map((menu) -> {
//                    menu.setChildren(getChildren(menu, entities));
//                    return menu;
//                }).sorted((menu1, menu2) -> {
//                    return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
//                })
//                .collect(Collectors.toList());
        // 1. 查询出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        // 2. 将所有分类按照父子关系进行分组，减少后续查找次数
        Map<Long, List<CategoryEntity>> groupByParent = entities.stream()
                .collect(Collectors.groupingBy(CategoryEntity::getParentCid));

        // 3. 组装成父子树形结构
        return buildTree(groupByParent, 0L);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);
        Collections.reverse(parentPath);
        return parentPath.toArray(new Long[0]);
    }

    /**
     * 级联更新所有关联的数据
     */
    // 第一种方式
//    @Caching(evict = {
//            @CacheEvict(value = "category", key = "'getLevel1Categorys'"),
//            @CacheEvict(value = "category", key = "'getCatalogJson'")
//    })
    // 第二种方式
    @CacheEvict(value = "category", allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    @Cacheable(value = {"category"}, key = "#root.method.name", sync = true)
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        return baseMapper.selectList(new LambdaQueryWrapper<CategoryEntity>().eq(CategoryEntity::getParentCid, 0));
    }

    @Cacheable(value = "category", key = "#root.methodName", sync = true)
    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        // 1、查出所有分类
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        // 2、将分类按 parentCid 分组存入 Map
        Map<Long, List<CategoryEntity>> categoryMap = selectList.stream()
                .collect(Collectors.groupingBy(CategoryEntity::getParentCid));
        // 3、获取所有一级分类 (parentCid 为 0 的分类)
        List<CategoryEntity> level1Categorys = categoryMap.getOrDefault(0L, Collections.emptyList());
        // 4、封装数据
        Map<String, List<Catalog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 获取当前一级分类的二级分类
            List<CategoryEntity> level2Categories = categoryMap.getOrDefault(v.getCatId(), Collections.emptyList());
            // 封装二级分类
            return level2Categories.stream().map(l2 -> {
                Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                // 获取当前二级分类的三级分类
                List<CategoryEntity> level3Categories = categoryMap.getOrDefault(l2.getCatId(), Collections.emptyList());
                // 封装三级分类
                List<Catalog2Vo.Catalog3Vo> catalog3Vos = level3Categories.stream()
                        .map(l3 -> new Catalog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName()))
                        .collect(Collectors.toList());
                catalog2Vo.setCatalog3List(catalog3Vos);
                return catalog2Vo;
            }).collect(Collectors.toList());
        }));

        return parent_cid;
    }

    public Map<String, List<Catalog2Vo>> getCatalogJson2() {
        // 加入缓存，格式是json
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.isEmpty(catalogJSON)) {
            return getCatalogJsonFromDBWithRedissonLock();
        }
        return JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catalog2Vo>>>() {
        });
    }

    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDBWithRedissonLock() {
        // 锁的名字，锁的粒度，越细越好
        RLock lock = redissonClient.getLock("CatalogJson-lock");
        lock.lock();
        Map<String, List<Catalog2Vo>> dataFromDB;
        try {
            dataFromDB = getDataFromDB();
        } finally {
            lock.unlock();
        }
        return dataFromDB;
    }

    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDBWithRedisLock() {
        // 1、占分布式锁，在redis占坑
        String uuid = UUID.randomUUID().toString();
        // Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "111");
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (lock != null) {
            // 设置过期时间，不能与加锁分开
            // redisTemplate.expire("lock", 30, TimeUnit.SECONDS);
            // 加锁成功，执行业务
            Map<String, List<Catalog2Vo>> dataFromDB;
            try {
                dataFromDB = getDataFromDB();
            } finally {
                // 删除加锁，lua脚本，保证原子性
                // redisTemplate.delete("lock");
                String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1] then\n" +
                        "    return redis.call(\"del\",KEYS[1])\n" +
                        "else\n" +
                        "    return 0\n" +
                        "end";
                redisTemplate.execute(new DefaultRedisScript<>(script, Integer.class), Arrays.asList("lock"), uuid);
            }
            return dataFromDB;
        } else {
            // 加锁失败，重试
            return getCatalogJsonFromDBWithRedisLock();
        }
    }

    private Map<String, List<Catalog2Vo>> getDataFromDB() {
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (!StringUtils.isEmpty(catalogJSON)) {
            // 缓存不为null直接返回
            return JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catalog2Vo>>>() {
            });
        }
        // 1、查出所有分类
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        // 2、将分类按 parentCid 分组存入 Map
        Map<Long, List<CategoryEntity>> categoryMap = selectList.stream()
                .collect(Collectors.groupingBy(CategoryEntity::getParentCid));
        // 3、获取所有一级分类 (parentCid 为 0 的分类)
        List<CategoryEntity> level1Categorys = categoryMap.getOrDefault(0L, Collections.emptyList());
        // 4、封装数据
        Map<String, List<Catalog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 获取当前一级分类的二级分类
            List<CategoryEntity> level2Categories = categoryMap.getOrDefault(v.getCatId(), Collections.emptyList());
            // 封装二级分类
            return level2Categories.stream().map(l2 -> {
                Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                // 获取当前二级分类的三级分类
                List<CategoryEntity> level3Categories = categoryMap.getOrDefault(l2.getCatId(), Collections.emptyList());
                // 封装三级分类
                List<Catalog2Vo.Catalog3Vo> catalog3Vos = level3Categories.stream()
                        .map(l3 -> new Catalog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName()))
                        .collect(Collectors.toList());
                catalog2Vo.setCatalog3List(catalog3Vos);
                return catalog2Vo;
            }).collect(Collectors.toList());
        }));

        String s = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
        return parent_cid;
    }

    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDBWithLocalLock() {
//        // 1、查出所有1级分类
//        List<CategoryEntity> selectList = baseMapper.selectList(null);
//        List<CategoryEntity> level1Categorys = getParentCid(selectList, 0L);
//        // 2、封装数据
//        return level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
//            // 1、每一个的一级分类，查到这个一级分类的二级分类
//            List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());
//            // 2、封装上面结果
//            if (categoryEntities == null || categoryEntities.isEmpty()) {
//                return Collections.emptyList();
//            }
//            return categoryEntities.stream().map(l2 -> {
//                Catalog2Vo catalog2Vo1 = new Catalog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
//                // 1、找当前二级分类的三级分类封装成vo
//                List<CategoryEntity> level3Catalog = getParentCid(selectList, l2.getCatId());
//                if (level3Catalog != null) {
//                    List<Catalog2Vo.Catalog3Vo> catalog3Vo = level3Catalog.stream().map(l3 -> {
//                        // 2、封装成指定格式
//                        return new Catalog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
//                    }).collect(Collectors.toList());
//                    catalog2Vo1.setCatalog3List(catalog3Vo);
//                }
//                return catalog2Vo1;
//            }).collect(Collectors.toList());
//        }));
        // 本地缓存锁
        synchronized (this) {
            // 得到锁以后，再查缓存，没有继续查询数据库
            return getDataFromDB();
        }
    }

    private List<CategoryEntity> getParentCid(List<CategoryEntity> selectList, Long parentCid) {
        return selectList.stream().filter(item -> item.getParentCid().equals(parentCid)).collect(Collectors.toList());
    }

    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

    // 递归查找所哟菜单的子菜单
    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {
        return all.stream().filter(categoryEntity -> {
            return Objects.equals(categoryEntity.getParentCid(), root.getCatId());
        }).map(categoryEntity -> {
            // 1、找到子菜单
            categoryEntity.setChildren(getChildren(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            // 2、菜单的排序
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
    }

    private List<CategoryEntity> buildTree(Map<Long, List<CategoryEntity>> groupByParent, Long parentId) {
        return groupByParent.getOrDefault(parentId, Collections.emptyList())
                .stream()
                .map(menu -> {
                    // 递归设置子菜单
                    menu.setChildren(buildTree(groupByParent, menu.getCatId()));
                    return menu;
                })
                .sorted(Comparator.comparingInt(menu -> menu.getSort() == null ? 0 : menu.getSort()))
                .collect(Collectors.toList());
    }
}