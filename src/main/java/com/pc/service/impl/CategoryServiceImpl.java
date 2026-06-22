package com.pc.service.impl;

import com.app.mq.CacheInvalidationMessage;
import com.app.mq.producer.MessageProducer;
import com.pc.dao.CategoryMapper;
import com.pc.pojo.Category;
import com.pc.service.CategoryService;
import com.pc.utils.RedisCacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CategoryServiceImpl implements CategoryService {
    
    @Autowired
    private CategoryMapper categoryMapper;
    
    @Autowired
    private RedisCacheUtil redisCacheUtil;
    
    @Autowired
    private MessageProducer messageProducer;
    
    private static final String CACHE_KEY_PREFIX = "category:";
    private static final long CACHE_EXPIRE_MINUTES = 30;
    
    @Override
    public List<Category> getAllCategories() {
        String cacheKey = CACHE_KEY_PREFIX + "all";
        List<Category> categories = redisCacheUtil.get(cacheKey, List.class);
        
        if (categories != null) {
            return categories;
        }
        
        categories = categoryMapper.selectAllCategories();
        if (categories != null && !categories.isEmpty()) {
            redisCacheUtil.setWithRandomExpire(cacheKey, categories, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        return categories;
    }
    
    @Override
    public Category getCategoryById(Integer categoryId) {
        if (categoryId == null) {
            return null;
        }
        
        String cacheKey = CACHE_KEY_PREFIX + "id:" + categoryId;
        Category category = redisCacheUtil.get(cacheKey, Category.class);
        
        if (category != null) {
            return category;
        }
        
        category = categoryMapper.selectCategoryById(categoryId);
        if (category != null) {
            redisCacheUtil.setWithRandomExpire(cacheKey, category, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        return category;
    }
    
    @Override
    public Category getCategoryByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        
        String cacheKey = CACHE_KEY_PREFIX + "name:" + name.trim();
        Category category = redisCacheUtil.get(cacheKey, Category.class);
        
        if (category != null) {
            return category;
        }
        
        category = categoryMapper.selectCategoryByName(name.trim());
        if (category != null) {
            redisCacheUtil.setWithRandomExpire(cacheKey, category, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        return category;
    }
    
    public void clearCategoryCache() {
        redisCacheUtil.clearByPattern(CACHE_KEY_PREFIX + "*");
        
        // 发送缓存失效消息到队列
        try {
            CacheInvalidationMessage message = new CacheInvalidationMessage();
            message.setType("pattern");
            message.setPattern(CACHE_KEY_PREFIX + "*");
            messageProducer.sendCacheInvalidation(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void clearCategoryCache(Integer categoryId) {
        redisCacheUtil.delete(CACHE_KEY_PREFIX + "all");
        if (categoryId != null) {
            redisCacheUtil.delete(CACHE_KEY_PREFIX + "id:" + categoryId);
        }
        
        // 发送缓存失效消息到队列
        try {
            CacheInvalidationMessage message = new CacheInvalidationMessage();
            message.setType("pattern");
            message.setPattern(CACHE_KEY_PREFIX + "*");
            messageProducer.sendCacheInvalidation(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

