package com.app.service.impl;

import com.app.dao.WCircleMapper;
import com.app.dao.CUserMapper;
import com.app.dao.WErrandMapper;
import com.app.pojo.CUser;
import com.app.service.WCircleService;
import com.app.service.UserPermissionCheckService;
import com.app.utils.RedisCacheUtil;
import com.app.mq.producer.MessageProducer;
import com.app.mq.NotificationMessage;
import com.app.mq.CacheInvalidationMessage;
import com.pc.pojo.PostVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class WCircleServiceImpl implements WCircleService {

    @Autowired
    private WCircleMapper circleMapper;
    
    @Autowired
    private CUserMapper cUserMapper;
    
    @Autowired
    private WErrandMapper wErrandMapper;
    
    @Autowired
    private UserPermissionCheckService permissionCheckService;

    @Autowired
    @Qualifier("appRedisCacheUtil")
    private RedisCacheUtil redisCacheUtil;

    @Autowired
    private MessageProducer messageProducer;

    private static final String CACHE_KEY_PREFIX = "circle:";
    private static final long CACHE_EXPIRE_MINUTES = 10;
    private static final long CACHE_EXPIRE_MINUTES_LIST = 5;

    @Override
    public Map<String, Object> getPosts(Integer categoryId, Integer page, Integer pageSize, Integer tagId, String sortType) {
        return getPosts(categoryId, page, pageSize, tagId, sortType, null);
    }

    @Override
    public Map<String, Object> getPosts(Integer categoryId, Integer page, Integer pageSize, Integer tagId, String sortType, Integer userId) {
        return getPostsWithPostUserId(categoryId, page, pageSize, tagId, sortType, userId, null);
    }
    
    private Map<String, Object> getPostsWithPostUserId(Integer categoryId, Integer page, Integer pageSize, Integer tagId, String sortType, Integer userId, Integer postUserId) {
        Map<String, Object> result = new HashMap<>();
        
        Map<String, Object> params = new HashMap<>();
        if (categoryId != null) {
            params.put("categoryId", categoryId);
        }
        if (tagId != null) {
            params.put("tagId", tagId);
        }
        if (userId != null) {
            params.put("userId", userId);
        }
        if (postUserId != null) {
            params.put("postUserId", postUserId);
        }
        params.put("sortType", sortType);
        
        int start = (page - 1) * pageSize;
        params.put("start", start);
        params.put("size", pageSize);
        
        List<PostVO> posts = circleMapper.selectPostsByCondition(params);
        int total = circleMapper.countPostsByCondition(params);
        int totalPages = (int) Math.ceil((double) total / pageSize);
        
        List<Map<String, Object>> postList = new ArrayList<>();
        for (PostVO post : posts) {
            Map<String, Object> postMap = convertPostToMap(post);
            if (userId != null) {
                Integer likeId = circleMapper.checkLikeExists(post.getPostId(), userId);
                postMap.put("liked", likeId != null);
                Integer favoriteId = circleMapper.checkFavoriteExists(post.getPostId(), userId);
                postMap.put("favorited", favoriteId != null);
            } else {
                postMap.put("liked", false);
                postMap.put("favorited", false);
            }
            postList.add(postMap);
        }
        
        result.put("posts", postList);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);
        
        return result;
    }

    @Override
    public Map<String, Object> getPostDetail(Integer postId, Integer userId) {
        if (postId == null) {
            return null;
        }
        
        circleMapper.incrementViewCount(postId);
        
        String cacheKey = CACHE_KEY_PREFIX + "detail:" + postId + ":" + (userId != null ? userId : "guest");
        Map<String, Object> result = redisCacheUtil.get(cacheKey, Map.class);
        
        if (result != null) {
            return result;
        }
        
        PostVO post = circleMapper.selectPostById(postId);
        if (post == null) {
            return null;
        }
        Map<String, Object> postMap = convertPostToMap(post);
        
        if (userId != null) {
            Integer likeId = circleMapper.checkLikeExists(postId, userId);
            postMap.put("liked", likeId != null);
            Integer favoriteId = circleMapper.checkFavoriteExists(postId, userId);
            postMap.put("favorited", favoriteId != null);
            Integer authorId = post.getUserId();
            if (authorId != null && !authorId.equals(userId)) {
                Integer followId = cUserMapper.checkFollowExists(userId, authorId);
                postMap.put("followed", followId != null);
            } else {
                postMap.put("followed", false);
            }
        } else {
            postMap.put("liked", false);
            postMap.put("favorited", false);
            postMap.put("followed", false);
        }
        
        redisCacheUtil.setWithRandomExpire(cacheKey, postMap, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        return postMap;
    }

    @Override
    public Map<String, Object> getPostComments(Integer postId) {
        Map<String, Object> result = new HashMap<>();
        System.out.println("查询评论: postId=" + postId);
        List<Map<String, Object>> comments = circleMapper.selectCommentsByPostId(postId);
        System.out.println("从数据库查询到 " + comments.size() + " 条评论记录");
        
        List<Map<String, Object>> commentList = new ArrayList<>();
        Map<Integer, Map<String, Object>> commentMap = new HashMap<>();
        
        for (Map<String, Object> comment : comments) {
            try {
                System.out.println("开始处理评论: " + comment);
                System.out.println("createTime类型: " + (comment.get("createTime") != null ? comment.get("createTime").getClass().getName() : "null"));
                
                Map<String, Object> commentData = new HashMap<>();
                commentData.put("id", String.valueOf(comment.get("id")));
                commentData.put("commentId", comment.get("commentId"));
                commentData.put("userId", comment.get("userId"));
                
                String commentStatus = comment.get("commentStatus") != null ? 
                    (String) comment.get("commentStatus") : "正常";
                boolean isDeleted = "已删除".equals(commentStatus);
                
                if (isDeleted) {
                    commentData.put("author", "");
                    commentData.put("avatar", "");
                    commentData.put("content", "该评论已被删除");
                    commentData.put("time", "");
                } else {
                    commentData.put("author", comment.get("author") != null ? comment.get("author") : "匿名");
                    commentData.put("avatar", comment.get("avatar") != null ? comment.get("avatar") : "");
                    commentData.put("content", comment.get("content") != null ? comment.get("content") : "");
                    commentData.put("time", formatTimeFromObject(comment.get("createTime")));
                }
                
                commentData.put("likeCount", comment.get("likeCount") != null ? comment.get("likeCount") : 0);
                Object parentIdObj = comment.get("parentId");
                Integer parentId = parentIdObj != null ? (Integer) parentIdObj : null;
                commentData.put("parentId", parentId);
                commentData.put("replies", new ArrayList<Map<String, Object>>());
                commentData.put("commentStatus", commentStatus);
                
                Integer commentId = (Integer) comment.get("commentId");
                commentMap.put(commentId, commentData);
                System.out.println("成功处理评论: id=" + commentId + ", parentId=" + parentId + ", author=" + comment.get("author"));
            } catch (Exception e) {
                System.err.println("处理评论时发生异常: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        for (Map<String, Object> commentData : commentMap.values()) {
            Integer parentId = (Integer) commentData.get("parentId");
            if (parentId == null) {
                commentList.add(commentData);
                System.out.println("添加顶级评论: " + commentData.get("commentId"));
            } else {
                Map<String, Object> parentComment = commentMap.get(parentId);
                if (parentComment != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> replies = (List<Map<String, Object>>) parentComment.get("replies");
                    replies.add(commentData);
                    System.out.println("添加回复到父评论 " + parentId + ": " + commentData.get("commentId"));
                } else {
                    commentList.add(commentData);
                    System.out.println("父评论不存在，作为顶级评论显示: " + commentData.get("commentId"));
                }
            }
        }
        
        System.out.println("最终返回 " + commentList.size() + " 条顶级评论（包含回复）");
        result.put("comments", commentList);
        result.put("total", comments.size());
        return result;
    }

    @Override
    public Map<String, Object> getHotPosts(Integer limit) {
        Map<String, Object> result = new HashMap<>();
        
        String cacheKey = CACHE_KEY_PREFIX + "hot:" + limit;
        Map<String, Object> cachedResult = redisCacheUtil.get(cacheKey, Map.class);
        
        if (cachedResult != null) {
            return cachedResult;
        }
        
        List<PostVO> posts = circleMapper.selectHotPosts(limit);
        
        List<Map<String, Object>> postList = new ArrayList<>();
        for (PostVO post : posts) {
            Map<String, Object> postMap = convertPostToMap(post);
            postList.add(postMap);
        }
        
        result.put("posts", postList);
        
        redisCacheUtil.setWithRandomExpire(cacheKey, result, CACHE_EXPIRE_MINUTES_LIST, TimeUnit.MINUTES);
        
        return result;
    }

    private Map<String, Object> convertPostToMap(PostVO post) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(post.getPostId()));
        map.put("postId", post.getPostId());
        map.put("author", post.getUsername() != null ? post.getUsername() : "匿名");
        
        String avatar = post.getAvatar();
        System.out.println("转换帖子数据 - postId: " + post.getPostId() + ", username: " + post.getUsername() + ", avatar: " + avatar);
        if (avatar == null || avatar.trim().isEmpty()) {
            avatar = "";
        }
        map.put("avatar", avatar);
        map.put("time", formatTime(post.getCreateTime()));
        map.put("title", post.getTitle() != null ? post.getTitle() : "");
        map.put("content", post.getContent() != null ? post.getContent() : "");
        
        List<String> tagNames = circleMapper.selectTagNamesByPostId(post.getPostId());
        map.put("tag", tagNames.isEmpty() ? "" : tagNames.get(0));
        map.put("tags", tagNames);
        
        map.put("views", post.getViewCount() != null ? post.getViewCount() : 0);
        map.put("comments", post.getCommentCount() != null ? post.getCommentCount() : 0);
        map.put("likes", post.getLikeCount() != null ? post.getLikeCount() : 0);
        map.put("liked", false);
        
        if (post.getPrice() != null) {
            map.put("price", post.getPrice().doubleValue());
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
        map.put("userId", post.getUserId());
        
        map.put("startPoint", post.getStartPoint() != null ? post.getStartPoint() : "");
        map.put("endPoint", post.getEndPoint() != null ? post.getEndPoint() : "");
        map.put("contactInfo", post.getContactInfo() != null ? post.getContactInfo() : "");
        map.put("status", post.getStatus());
        
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
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM-dd HH:mm");
            return sdf.format(date);
        }
    }

    private String formatTimeFromObject(Object timeObj) {
        if (timeObj == null) {
            System.out.println("formatTimeFromObject: timeObj为null，返回'刚刚'");
            return "刚刚";
        }
        
        System.out.println("formatTimeFromObject: 时间类型=" + timeObj.getClass().getName() + ", 值=" + timeObj);
        
        Date date = null;
        
        if (timeObj instanceof java.time.LocalDateTime) {
            java.time.LocalDateTime localDateTime = (java.time.LocalDateTime) timeObj;
            java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
            java.time.ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);
            date = Date.from(zonedDateTime.toInstant());
            System.out.println("formatTimeFromObject: LocalDateTime转换为Date成功");
        } else if (timeObj instanceof Date) {
            date = (Date) timeObj;
            System.out.println("formatTimeFromObject: 直接使用Date类型");
        } else {
            System.err.println("formatTimeFromObject: 未知的时间类型: " + timeObj.getClass().getName() + ", 值: " + timeObj);
            return "刚刚";
        }
        
        String formattedTime = formatTime(date);
        System.out.println("formatTimeFromObject: 格式化后时间=" + formattedTime);
        return formattedTime;
    }

    @Override
    public void incrementViewCount(Integer postId) {
        circleMapper.incrementViewCount(postId);
    }

    @Override
    public Map<String, Object> addComment(Integer postId, Integer userId, String content, Integer parentId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            System.out.println("添加评论: postId=" + postId + ", userId=" + userId + ", content=" + content + ", parentId=" + parentId);
            
            if (userId != null) {
                Map<String, Object> permissionCheck = permissionCheckService.checkCommentPermission(userId);
                if (Boolean.TRUE.equals(permissionCheck.get("banned"))) {
                    result.put("success", false);
                    result.put("error", permissionCheck.get("message"));
                    result.put("banned", true);
                    System.out.println("评论权限检查失败: userId=" + userId + ", message=" + permissionCheck.get("message"));
                    return result;
                }
            }
            
            circleMapper.insertComment(postId, userId, content, parentId);
            int commentId = circleMapper.getLastInsertCommentId();
            System.out.println("评论插入成功，commentId=" + commentId);
            
            circleMapper.incrementCommentCount(postId);
            System.out.println("评论数更新成功");
            
            PostVO post = circleMapper.selectPostById(postId);
            int updatedCommentCount = post != null && post.getCommentCount() != null ? post.getCommentCount() : 0;
            
            try {
                if (post != null && post.getUserId() != null && !post.getUserId().equals(userId)) {
                    NotificationMessage notification = new NotificationMessage();
                    notification.setUserId(post.getUserId());
                    notification.setFromUserId(userId);
                    notification.setType("comment");
                    notification.setContent("有人评论了你的帖子");
                    notification.setPostId(postId);
                    notification.setCommentId(commentId);
                    notification.setCreateTime(new java.util.Date());
                    
                    CUser fromUser = cUserMapper.findById(userId);
                    if (fromUser != null) {
                        notification.setFromUsername(fromUser.getUsername());
                        notification.setFromAvatar(fromUser.getAvatar());
                    }
                    
                    messageProducer.sendNotification(notification);
                }
            } catch (Exception e) {
                System.err.println("发送评论通知失败: " + e.getMessage());
            }
            
            clearPostCache(postId);
            
            result.put("commentId", commentId);
            result.put("commentCount", updatedCommentCount);
            result.put("success", true);
            return result;
        } catch (Exception e) {
            System.err.println("添加评论失败: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> toggleLike(Integer postId, Integer userId) {
        Map<String, Object> result = new HashMap<>();
        
        Integer likeId = circleMapper.checkLikeExists(postId, userId);
        
        if (likeId != null) {
            circleMapper.deleteLike(postId, userId);
            circleMapper.updateLikeCount(postId, -1);
            result.put("liked", false);
        } else {
            Integer recordId = circleMapper.checkLikeRecordExists(postId, userId);
            if (recordId != null) {
                circleMapper.restoreLike(postId, userId);
                circleMapper.updateLikeCount(postId, 1);
            } else {
                circleMapper.insertLike(postId, userId);
                circleMapper.updateLikeCount(postId, 1);
            }
            result.put("liked", true);
            
            try {
                PostVO post = circleMapper.selectPostById(postId);
                if (post != null && post.getUserId() != null && !post.getUserId().equals(userId)) {
                    NotificationMessage notification = new NotificationMessage();
                    notification.setUserId(post.getUserId());
                    notification.setFromUserId(userId);
                    notification.setType("like");
                    notification.setContent("有人点赞了你的帖子");
                    notification.setPostId(postId);
                    notification.setCreateTime(new java.util.Date());
                    
                    CUser fromUser = cUserMapper.findById(userId);
                    if (fromUser != null) {
                        notification.setFromUsername(fromUser.getUsername());
                        notification.setFromAvatar(fromUser.getAvatar());
                    }
                    
                    messageProducer.sendNotification(notification);
                }
            } catch (Exception e) {
                System.err.println("发送点赞通知失败: " + e.getMessage());
            }
        }
        
        clearPostCache(postId);
        
        result.put("success", true);
        return result;
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> result = new HashMap<>();
        
        Map<String, Object> params = new HashMap<>();
        int postCount = circleMapper.countPostsByCondition(params);
        
        int userCount = circleMapper.countUsers();
        
        result.put("postCount", postCount);
        result.put("userCount", userCount);
        
        return result;
    }

    @Override
    public Map<String, Object> addFavorite(Integer postId, Integer userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Integer favoriteId = circleMapper.checkFavoriteExists(postId, userId);
            
            if (favoriteId != null) {
                circleMapper.deleteFavorite(postId, userId);
                circleMapper.updateFavoriteCount(postId, -1);
                result.put("success", true);
                result.put("favorited", false);
                result.put("message", "取消收藏成功");
                return result;
            }
            
            Integer recordId = circleMapper.checkFavoriteRecordExists(postId, userId);
            if (recordId != null) {
                circleMapper.restoreFavorite(postId, userId);
                circleMapper.updateFavoriteCount(postId, 1);
            } else {
                circleMapper.insertFavorite(postId, userId);
                circleMapper.updateFavoriteCount(postId, 1);
            }
            
            result.put("success", true);
            result.put("favorited", true);
            result.put("message", "收藏成功");
            return result;
        } catch (Exception e) {
            System.err.println("收藏操作失败: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> getFavoritePosts(Integer userId, Integer page, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        int start = (page - 1) * pageSize;
        
        List<PostVO> posts = circleMapper.selectFavoritePostsByUserId(userId, start, pageSize);
        int total = circleMapper.countFavoritePostsByUserId(userId);
        int totalPages = (int) Math.ceil((double) total / pageSize);
        
        List<Map<String, Object>> postList = new ArrayList<>();
        for (PostVO post : posts) {
            Map<String, Object> postMap = convertPostToMap(post);
            postMap.put("favorited", true);
            postList.add(postMap);
        }
        
        result.put("posts", postList);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);
        
        return result;
    }
    
    @Override
    public Map<String, Object> deleteComment(Integer commentId, Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer commentUserId = circleMapper.getCommentUserId(commentId);
            if (commentUserId == null) {
                result.put("success", false);
                result.put("message", "评论不存在");
                return result;
            }
            if (!commentUserId.equals(userId)) {
                result.put("success", false);
                result.put("message", "无权删除此评论");
                return result;
            }
            
            Integer postId = circleMapper.getPostIdByCommentId(commentId);
            
            circleMapper.deleteComment(commentId);
            
            if (postId != null) {
                circleMapper.decrementCommentCount(postId);
            }
            
            result.put("success", true);
            result.put("message", "删除成功");
            return result;
        } catch (Exception e) {
            System.err.println("删除评论失败: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }
    
    @Override
    public List<Map<String, Object>> getUserComments(Integer userId) {
        List<Map<String, Object>> comments = circleMapper.selectCommentsByUserId(userId, 0, 100);
        return comments;
    }
    
    @Override
    public List<Map<String, Object>> getUserOrders(Integer userId) {
        List<Map<String, Object>> orders = new ArrayList<>();
        
        List<PostVO> postVOs = wErrandMapper.selectUserErrandPosts(userId, 2);
        
        for (PostVO postVO : postVOs) {
            Map<String, Object> post = convertPostToMap(postVO);
            Map<String, Object> order = new HashMap<>();
            Integer postId = (Integer) post.get("postId");
            order.put("taskId", postId);
            order.put("postId", postId);
            order.put("title", post.get("title"));
            order.put("description", post.get("content"));
            order.put("amount", post.get("price"));
            order.put("creatorId", post.get("userId"));
            order.put("createTime", post.get("time"));
            order.put("status", post.get("status"));
            order.put("contactInfo", post.get("contactInfo"));
            order.put("image1", post.get("image1"));
            order.put("image2", post.get("image2"));
            order.put("image3", post.get("image3"));
            order.put("startLocation", post.get("startPoint"));
            order.put("endLocation", post.get("endPoint"));
            order.put("isFromPost", true);
            
            if (postId != null) {
                try {
                    Map<String, Object> tradeTask = wErrandMapper.selectTradeTaskByPostId(postId);
                    if (tradeTask != null) {
                        order.put("acceptorId", tradeTask.get("acceptorId"));
                        order.put("acceptorUsername", tradeTask.get("acceptorUsername"));
                        order.put("taskStatus", tradeTask.get("taskStatus"));
                        order.put("taskId", tradeTask.get("taskId"));
                    } else {
                        order.put("acceptorId", null);
                        order.put("taskStatus", 0);
                    }
                } catch (Exception e) {
                    order.put("acceptorId", null);
                    order.put("taskStatus", 0);
                }
            } else {
                order.put("acceptorId", null);
                order.put("taskStatus", 0);
            }
            
            orders.add(order);
        }
        
        try {
            List<Map<String, Object>> acceptedTasks = wErrandMapper.selectTradeTasksByAcceptorId(userId);
            if (acceptedTasks != null) {
                for (Map<String, Object> task : acceptedTasks) {
                    Integer taskPostId = (Integer) task.get("postId");
                    boolean exists = false;
                    for (Map<String, Object> existingOrder : orders) {
                        if (taskPostId != null && taskPostId.equals(existingOrder.get("postId"))) {
                            exists = true;
                            break;
                        }
                    }
                    
                    if (!exists) {
                        Map<String, Object> order = new HashMap<>();
                        order.put("taskId", task.get("taskId"));
                        order.put("postId", taskPostId);
                        order.put("title", task.get("title"));
                        order.put("description", task.get("description"));
                        order.put("amount", task.get("amount"));
                        order.put("creatorId", task.get("creatorId"));
                        order.put("creatorUsername", task.get("creatorUsername"));
                        order.put("acceptorId", task.get("acceptorId"));
                        order.put("taskStatus", task.get("taskStatus"));
                        order.put("status", task.get("status"));
                        order.put("createTime", task.get("createTime"));
                        Object contactInfoObj = task.get("contactInfo");
                        String contactInfoStr = (contactInfoObj != null) ? contactInfoObj.toString() : "";
                        System.out.println("[DEBUG] getUserOrders - 我的接单contactInfo: " + contactInfoStr);
                        order.put("contactInfo", contactInfoStr);
                        Object image1Obj = task.get("image1");
                        Object image2Obj = task.get("image2");
                        Object image3Obj = task.get("image3");
                        System.out.println("[DEBUG] getUserOrders - 我的接单图片: image1=" + image1Obj + ", image2=" + image2Obj + ", image3=" + image3Obj);
                        if (image1Obj != null && !image1Obj.toString().isEmpty() && !image1Obj.toString().equals("null")) {
                            order.put("image1", image1Obj);
                        }
                        if (image2Obj != null && !image2Obj.toString().isEmpty() && !image2Obj.toString().equals("null")) {
                            order.put("image2", image2Obj);
                        }
                        if (image3Obj != null && !image3Obj.toString().isEmpty() && !image3Obj.toString().equals("null")) {
                            order.put("image3", image3Obj);
                        }
                        order.put("startLocation", task.get("startLocation"));
                        order.put("endLocation", task.get("endLocation"));
                        order.put("isFromPost", false);
                        orders.add(order);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("查询我接到的订单失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return orders;
    }
    
    @Override
    public List<Map<String, Object>> getUserPosts(Integer userId, Integer categoryId) {
        List<Integer> categoryIds = new ArrayList<>();
        if (categoryId != null) {
            categoryIds.add(categoryId);
        } else {
            categoryIds.add(1);
            categoryIds.add(3);
            categoryIds.add(4);
        }
        
        List<Map<String, Object>> allPosts = new ArrayList<>();
        for (Integer catId : categoryIds) {
            Map<String, Object> result = getPostsWithPostUserId(catId, 1, 100, null, "newest", null, userId);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> posts = (List<Map<String, Object>>) result.get("posts");
            if (posts != null && !posts.isEmpty()) {
                allPosts.addAll(posts);
            }
        }
        
        List<Map<String, Object>> cPostList = new ArrayList<>();
        for (Map<String, Object> post : allPosts) {
            Map<String, Object> cPost = new HashMap<>();
            cPost.put("postId", post.get("postId"));
            cPost.put("userId", post.get("userId"));
            cPost.put("title", post.get("title"));
            cPost.put("content", post.get("content"));
            
            Object imagesObj = post.get("images");
            if (imagesObj instanceof List) {
                cPost.put("images", imagesObj);
                if (((List<?>) imagesObj).size() > 0) {
                    cPost.put("image1", ((List<?>) imagesObj).get(0));
                } else {
                    cPost.put("image1", null);
                }
            } else {
                cPost.put("images", new ArrayList<>());
                cPost.put("image1", null);
            }
            
            cPost.put("createTime", post.get("time"));
            cPost.put("authorName", post.get("author"));
            cPost.put("authorAvatar", post.get("avatar"));
            cPost.put("views", post.get("views"));
            cPost.put("likes", post.get("likes"));
            cPost.put("comments", post.get("comments"));
            
            Object tagObj = post.get("tag");
            if (tagObj != null) {
                cPost.put("tag", tagObj);
            }
            Object tagsObj = post.get("tags");
            if (tagsObj instanceof List) {
                cPost.put("tags", tagsObj);
            }
            
            Object likedObj = post.get("liked");
            if (likedObj != null) {
                cPost.put("liked", likedObj);
            }
            Object favoritedObj = post.get("favorited");
            if (favoritedObj != null) {
                cPost.put("favorited", favoritedObj);
            }
            
            Object categoryIdObj = post.get("categoryId");
            if (categoryIdObj != null) {
                cPost.put("categoryId", categoryIdObj);
            }
            Object categoryNameObj = post.get("categoryName");
            if (categoryNameObj != null) {
                cPost.put("categoryName", categoryNameObj);
            }
            
            Object priceObj = post.get("price");
            if (priceObj != null) {
                cPost.put("price", priceObj);
            }
            
            cPostList.add(cPost);
        }
        
        cPostList.sort((a, b) -> {
            Object timeA = a.get("createTime");
            Object timeB = b.get("createTime");
            if (timeA == null || timeB == null) return 0;
            return timeB.toString().compareTo(timeA.toString());
        });
        
        return cPostList;
    }
    
    @Override
    public Map<String, Object> deletePost(Integer postId, Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            PostVO post = circleMapper.selectPostById(postId);
            if (post == null) {
                result.put("success", false);
                result.put("message", "帖子不存在");
                return result;
            }
            
            if (!post.getUserId().equals(userId)) {
                result.put("success", false);
                result.put("message", "无权删除此帖子");
                return result;
            }
            
            int updateCount = circleMapper.deletePost(postId, userId);
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
    
    private void clearPostCache(Integer postId) {
        if (postId != null) {
            redisCacheUtil.clearByPattern(CACHE_KEY_PREFIX + "detail:" + postId + "*");
            redisCacheUtil.clearByPattern(CACHE_KEY_PREFIX + "hot*");
        }
    }
}
