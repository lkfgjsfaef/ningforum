package com.pc.service.impl;

import com.app.mq.CacheInvalidationMessage;
import com.app.mq.producer.MessageProducer;
import com.pc.dao.TagMapper;
import com.pc.pojo.Tag;
import com.pc.service.TagService;
import com.pc.utils.RedisCacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TagServiceImpl implements TagService {
    
    @Autowired
    private TagMapper tagMapper;
    
    @Autowired
    private RedisCacheUtil redisCacheUtil;
    
    @Autowired
    private MessageProducer messageProducer;
    
    private static final String CACHE_KEY_PREFIX = "tag:";
    private static final long CACHE_EXPIRE_MINUTES = 30;
    
    @Override
    public List<Tag> getAllTags(String tagName, Integer categoryId) {
        String cacheKey = CACHE_KEY_PREFIX + "all:" + (tagName != null ? tagName : "") + ":" + (categoryId != null ? categoryId : "");
        List<Tag> tags = redisCacheUtil.get(cacheKey, List.class);
        
        if (tags != null) {
            return tags;
        }
        
        tags = tagMapper.selectAllTags(tagName, categoryId);
        if (tags != null && !tags.isEmpty()) {
            redisCacheUtil.setWithRandomExpire(cacheKey, tags, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        return tags;
    }
    
    @Override
    public Tag getTagById(Integer tagId) {
        if (tagId == null) {
            return null;
        }
        
        String cacheKey = CACHE_KEY_PREFIX + "id:" + tagId;
        Tag tag = redisCacheUtil.get(cacheKey, Tag.class);
        
        if (tag != null) {
            return tag;
        }
        
        tag = tagMapper.selectTagById(tagId);
        if (tag != null) {
            redisCacheUtil.setWithRandomExpire(cacheKey, tag, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        return tag;
    }
    
    @Override
    public boolean addTag(String tagName, Integer categoryId) {
        try {
            if (tagName == null || tagName.trim().isEmpty()) {
                return false;
            }
            
            Tag existingTag = tagMapper.selectTagByName(tagName.trim());
            if (existingTag != null) {
                return false;
            }
            
            Tag tag = new Tag();
            tag.setName(tagName.trim());
            int result = tagMapper.insertTag(tag);
            if (result > 0 && tag.getTagId() != null && categoryId != null) {
                tagMapper.insertCategoryTag(categoryId, tag.getTagId());
            }
            
            if (result > 0) {
                clearTagCache();
            }
            
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean updateTag(Integer tagId, String tagName, Integer categoryId) {
        try {
            if (tagId == null || tagName == null || tagName.trim().isEmpty()) {
                return false;
            }
            
            Tag existingTag = tagMapper.selectTagById(tagId);
            if (existingTag == null) {
                return false;
            }
            
            if (!existingTag.getName().equals(tagName.trim())) {
                Tag tagWithSameName = tagMapper.selectTagByName(tagName.trim());
                if (tagWithSameName != null) {
                    return false;
                }
            }
            
            Tag tag = new Tag();
            tag.setTagId(tagId);
            tag.setName(tagName.trim());
            int result = tagMapper.updateTag(tag);
            
            if (result > 0 && categoryId != null) {
                tagMapper.deleteCategoryTagByTagId(tagId);
                tagMapper.insertCategoryTag(categoryId, tagId);
            } else if (result > 0 && categoryId == null) {
                tagMapper.deleteCategoryTagByTagId(tagId);
            }
            
            if (result > 0) {
                clearTagCache(tagId);
            }
            
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean deleteTag(Integer tagId) {
        try {
            if (tagId == null) {
                return false;
            }
            tagMapper.deleteCategoryTagByTagId(tagId);
            int result = tagMapper.deleteTag(tagId);
            
            if (result > 0) {
                clearTagCache(tagId);
            }
            
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean batchDeleteTags(List<Integer> tagIds) {
        try {
            if (tagIds == null || tagIds.isEmpty()) {
                return false;
            }
            tagMapper.batchDeleteCategoryTags(tagIds);
            int result = tagMapper.batchDeleteTags(tagIds);
            
            if (result > 0) {
                clearTagCache();
                for (Integer tagId : tagIds) {
                    clearTagCache(tagId);
                }
            }
            
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public void clearTagCache() {
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
    
    public void clearTagCache(Integer tagId) {
        redisCacheUtil.clearByPattern(CACHE_KEY_PREFIX + "all*");
        if (tagId != null) {
            redisCacheUtil.delete(CACHE_KEY_PREFIX + "id:" + tagId);
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

