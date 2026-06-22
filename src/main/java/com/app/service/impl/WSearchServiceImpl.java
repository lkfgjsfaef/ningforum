package com.app.service.impl;

import com.app.dao.WSearchMapper;
import com.app.service.WSearchService;
import com.app.utils.RedisCacheUtil;
import com.pc.pojo.PostVO;
import com.pc.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class WSearchServiceImpl implements WSearchService {

    @Autowired
    private WSearchMapper searchMapper;

    @Autowired
    @Qualifier("appRedisCacheUtil")
    private RedisCacheUtil redisCacheUtil;

    private static final String CACHE_KEY_PREFIX = "search:";
    private static final long CACHE_EXPIRE_MINUTES = 5;

    @Override
    public Map<String, Object> searchPosts(String keyword, Integer categoryId, Integer page, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();

        String cacheKey = CACHE_KEY_PREFIX + "posts:" + keyword + ":" + categoryId + ":" + page + ":" + pageSize;
        Map<String, Object> cachedResult = redisCacheUtil.get(cacheKey, Map.class);
        
        if (cachedResult != null) {
            return cachedResult;
        }

        int start = (page - 1) * pageSize;
        List<PostVO> posts = searchMapper.searchPosts(keyword, categoryId, start, pageSize);
        int total = searchMapper.countSearchPosts(keyword, categoryId);
        int totalPages = (int) Math.ceil((double) total / pageSize);

        List<Map<String, Object>> postList = new ArrayList<>();
        for (PostVO post : posts) {
            Map<String, Object> postMap = convertPostToMap(post);
            postList.add(postMap);
        }

        result.put("posts", postList);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);

        redisCacheUtil.setWithRandomExpire(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        return result;
    }

    @Override
    public Map<String, Object> searchUsers(String keyword, Integer page, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();

        String cacheKey = CACHE_KEY_PREFIX + "users:" + keyword + ":" + page + ":" + pageSize;
        Map<String, Object> cachedResult = redisCacheUtil.get(cacheKey, Map.class);
        
        if (cachedResult != null) {
            return cachedResult;
        }

        int start = (page - 1) * pageSize;
        List<User> users = searchMapper.searchUsers(keyword, start, pageSize);
        int total = searchMapper.countSearchUsers(keyword);
        int totalPages = (int) Math.ceil((double) total / pageSize);

        List<Map<String, Object>> userList = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("userId", user.getUserId());
            userMap.put("username", user.getUsername());
            userMap.put("avatar", user.getAvatar());
            userMap.put("signature", user.getSignature());
            userList.add(userMap);
        }

        result.put("users", userList);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);

        redisCacheUtil.setWithRandomExpire(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        return result;
    }

    private Map<String, Object> convertPostToMap(PostVO post) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(post.getPostId()));
        map.put("postId", post.getPostId());
        map.put("author", post.getUsername() != null ? post.getUsername() : "匿名");
        map.put("avatar", post.getAvatar() != null ? post.getAvatar() : "");
        map.put("time", formatTime(post.getCreateTime()));
        map.put("title", post.getTitle() != null ? post.getTitle() : "");
        map.put("content", post.getContent() != null ? post.getContent() : "");
        map.put("views", post.getViewCount() != null ? post.getViewCount() : 0);
        map.put("comments", post.getCommentCount() != null ? post.getCommentCount() : 0);
        map.put("likes", post.getLikeCount() != null ? post.getLikeCount() : 0);

        if (post.getPrice() != null) {
            map.put("price", post.getPrice());
        } else {
            map.put("price", 0.00);
        }

        List<String> images = new ArrayList<>();
        if (post.getImage1() != null && !post.getImage1().isEmpty()) {
            images.add(post.getImage1());
        }
        if (post.getImage2() != null && !post.getImage2().isEmpty()) {
            images.add(post.getImage2());
        }
        if (post.getImage3() != null && !post.getImage3().isEmpty()) {
            images.add(post.getImage3());
        }
        map.put("images", images);

        map.put("categoryId", post.getCategoryId());
        map.put("categoryName", post.getCategoryName());

        return map;
    }

    private String formatTime(Date date) {
        if (date == null) {
            return "刚刚";
        }

        long now = System.currentTimeMillis();
        long time = date.getTime();
        long diff = now - time;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "刚刚";
        } else if (minutes < 60) {
            return minutes + "分钟前";
        } else if (hours < 24) {
            return hours + "小时前";
        } else if (days < 7) {
            return days + "天前";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
            return sdf.format(date);
        }
    }
}
