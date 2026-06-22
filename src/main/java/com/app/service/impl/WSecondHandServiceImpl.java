package com.app.service.impl;

import com.app.dao.WCircleMapper;
import com.app.dao.WSecondHandMapper;
import com.app.service.WSecondHandService;
import com.pc.pojo.PostVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class WSecondHandServiceImpl implements WSecondHandService {

    @Autowired
    private WSecondHandMapper secondHandMapper;

    @Autowired
    private WCircleMapper circleMapper;

    @Override
    public Map<String, Object> getItems(Integer page, Integer pageSize, Integer tagId, String sortType) {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> params = new HashMap<>();
        params.put("categoryId", 3);
        if (tagId != null) {
            params.put("tagId", tagId);
        }
        if ("newest".equals(sortType) || "hottest".equals(sortType)) {
            params.put("sortType", sortType);
        }

        int start = (page - 1) * pageSize;
        params.put("start", start);
        params.put("size", pageSize);

        List<PostVO> posts = secondHandMapper.selectSecondHandPosts(params);
        int total = secondHandMapper.countSecondHandPosts(params);
        int totalPages = (int) Math.ceil((double) total / pageSize);

        List<Map<String, Object>> itemList = new ArrayList<>();
        for (PostVO post : posts) {
            Map<String, Object> itemMap = convertPostToItemMap(post);
            itemList.add(itemMap);
        }

        result.put("items", itemList);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);

        return result;
    }

    @Override
    public Map<String, Object> getItem(Integer itemId) {
        secondHandMapper.incrementViewCount(itemId);

        PostVO post = secondHandMapper.selectSecondHandPostById(itemId);
        if (post == null) {
            return null;
        }
        return convertPostToItemMap(post);
    }

    @Override
    public void incrementViewCount(Integer itemId) {
        secondHandMapper.incrementViewCount(itemId);
    }

    @Override
    public Map<String, Object> deletePost(Integer postId, Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            PostVO post = secondHandMapper.selectSecondHandPostById(postId);
            if (post == null) {
                result.put("success", false);
                result.put("message", "商品不存在");
                return result;
            }
            
            if (!post.getUserId().equals(userId)) {
                result.put("success", false);
                result.put("message", "无权删除此商品");
                return result;
            }
            
            int updateCount = secondHandMapper.deletePost(postId, userId);
            if (updateCount > 0) {
                result.put("success", true);
                result.put("message", "删除成功");
            } else {
                result.put("success", false);
                result.put("message", "删除失败");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除异常: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    private Map<String, Object> convertPostToItemMap(PostVO post) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(post.getPostId()));
        map.put("postId", post.getPostId());
        map.put("title", post.getTitle() != null ? post.getTitle() : "");
        map.put("desc", post.getContent() != null ? post.getContent() : "");
        map.put("price", post.getPrice() != null ? post.getPrice().toString() : "0.00");

        String tag = "二手";
        try {
            List<String> tagNames = circleMapper.selectTagNamesByPostId(post.getPostId());
            System.out.println("帖子 " + post.getPostId() + " 的标签: " + tagNames);
            if (tagNames != null && !tagNames.isEmpty()) {
                tag = tagNames.get(0);
            }
        } catch (Exception e) {
            System.err.println("查询标签失败: postId=" + post.getPostId() + ", error=" + e.getMessage());
            e.printStackTrace();
        }
        map.put("tag", tag);
        System.out.println("最终标签: " + tag);

        map.put("publisher", post.getUsername() != null ? post.getUsername() : "匿名");
        map.put("sellerId", post.getUserId());
        String avatar = post.getAvatar() != null ? post.getAvatar() : "";
        System.out.println("二手集市转换: postId=" + post.getPostId() + ", username=" + post.getUsername() + ", avatar=" + avatar + ", userId=" + post.getUserId());
        map.put("avatar", avatar);
        map.put("time", formatTime(post.getCreateTime()));
        map.put("views", post.getViewCount() != null ? post.getViewCount() : 0);

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
