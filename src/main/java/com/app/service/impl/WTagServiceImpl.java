package com.app.service.impl;

import com.app.dao.WTagMapper;
import com.app.service.WTagService;
import com.app.utils.RedisCacheUtil;
import com.pc.pojo.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class WTagServiceImpl implements WTagService {

    @Autowired
    private WTagMapper tagMapper;

    @Autowired
    @Qualifier("appRedisCacheUtil")
    private RedisCacheUtil redisCacheUtil;

    private static final String CACHE_KEY_PREFIX = "tag:";
    private static final long CACHE_EXPIRE_MINUTES = 60;

    @Override
    public List<Map<String, Object>> getTagsByCategory(Integer categoryId) {
        String cacheKey = CACHE_KEY_PREFIX + "category:" + categoryId;
        List<Map<String, Object>> cachedResult = redisCacheUtil.get(cacheKey, List.class);
        
        if (cachedResult != null) {
            return cachedResult;
        }
        
        List<Tag> tags = tagMapper.selectTagsByCategory(categoryId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Tag tag : tags) {
            Map<String, Object> map = new HashMap<>();
            map.put("tagId", tag.getTagId());
            map.put("name", tag.getName());
            map.put("categoryId", tag.getCategoryId());
            map.put("categoryName", tag.getCategoryName());
            result.add(map);
        }
        
        redisCacheUtil.setWithRandomExpire(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        return result;
    }
}
