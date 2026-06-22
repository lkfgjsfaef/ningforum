package com.pc.service.impl;

import com.app.mq.CacheInvalidationMessage;
import com.app.mq.producer.MessageProducer;
import com.pc.dao.PostMapper;
import com.pc.pojo.PostVO;
import com.pc.service.PostService;
import com.pc.utils.RedisCacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class PostServiceImpl implements PostService {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private RedisCacheUtil redisCacheUtil;
    
    @Autowired
    private MessageProducer messageProducer;

    private static final String CACHE_KEY_PREFIX = "post:";
    private static final long CACHE_EXPIRE_MINUTES = 10;
    private static final long CACHE_EXPIRE_MINUTES_LIST = 5;

    @Override
    public PostVO getPostById(Integer postId) {
        if (postId == null) {
            return null;
        }
        
        String cacheKey = CACHE_KEY_PREFIX + "id:" + postId;
        return redisCacheUtil.getWithLock(cacheKey, PostVO.class, () -> postMapper.selectByPrimaryKey(postId), CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public List<PostVO> getAllPosts() {
        String cacheKey = CACHE_KEY_PREFIX + "all";
        List<PostVO> posts = redisCacheUtil.get(cacheKey, List.class);
        
        if (posts != null) {
            return posts;
        }
        
        posts = postMapper.selectAll();
        if (posts != null && !posts.isEmpty()) {
            redisCacheUtil.setWithRandomExpire(cacheKey, posts, CACHE_EXPIRE_MINUTES_LIST, TimeUnit.MINUTES);
        }
        
        return posts;
    }

    @Override
    public List<PostVO> searchPosts(Map<String, Object> params) {
        String cacheKey = CACHE_KEY_PREFIX + "search:" + params.hashCode();
        List<PostVO> posts = redisCacheUtil.get(cacheKey, List.class);
        
        if (posts != null) {
            return posts;
        }
        
        posts = postMapper.selectByCondition(params);
        if (posts != null && !posts.isEmpty()) {
            redisCacheUtil.setWithRandomExpire(cacheKey, posts, CACHE_EXPIRE_MINUTES_LIST, TimeUnit.MINUTES);
        }
        
        return posts;
    }

    @Override
    public int countPosts(Map<String, Object> params) {
        return postMapper.countByCondition(params);
    }

    @Override
    public boolean updatePostStatus(Integer postId, Integer status) {
        Date reviewTime = new Date();
        boolean result = postMapper.updateStatus(postId, status, reviewTime) > 0;
        
        if (result) {
            clearPostCache(postId);
        }
        
        return result;
    }

    @Override
    public int countPostsByStatus(Integer status) {
        return postMapper.countByStatus(status);
    }
    
    public void clearPostCache() {
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
    
    public void clearPostCache(Integer postId) {
        redisCacheUtil.clearByPattern(CACHE_KEY_PREFIX + "all*");
        redisCacheUtil.clearByPattern(CACHE_KEY_PREFIX + "search*");
        if (postId != null) {
            redisCacheUtil.delete(CACHE_KEY_PREFIX + "id:" + postId);
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

