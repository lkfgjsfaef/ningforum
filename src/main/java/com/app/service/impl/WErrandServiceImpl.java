package com.app.service.impl;

import com.app.dao.WErrandMapper;
import com.app.service.WErrandService;
import com.app.utils.RedisCacheUtil;
import com.pc.dao.UserMapper;
import com.pc.dao.MessageMapper;
import com.pc.pojo.PostVO;
import com.pc.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class WErrandServiceImpl implements WErrandService {

    @Autowired
    private WErrandMapper errandMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    @Qualifier("appRedisCacheUtil")
    private RedisCacheUtil redisCacheUtil;

    private static final String CACHE_KEY_PREFIX = "errand:";
    private static final long CACHE_EXPIRE_MINUTES = 5;

    @Override
    public Map<String, Object> getOrders(String status, Integer page, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        String cacheKey = CACHE_KEY_PREFIX + "orders:" + status + ":" + page + ":" + pageSize;
        Map<String, Object> cachedResult = redisCacheUtil.get(cacheKey, Map.class);
        
        if (cachedResult != null) {
            return cachedResult;
        }
        
        List<Integer> statusList = new ArrayList<>();
        if ("unaccepted".equals(status)) {
            statusList.add(0);
            statusList.add(1);
        } else if ("completed".equals(status)) {
            statusList.add(3);
        } else {
            statusList.add(0);
            statusList.add(1);
        }
        
        int start = (page - 1) * pageSize;
        System.out.println("========== 跑腿查询开始 ==========");
        System.out.println("查询参数: status=" + status + ", statusList=" + statusList + ", start=" + start + ", size=" + pageSize);
        
        List<PostVO> posts = errandMapper.selectErrandOrders(statusList, start, pageSize);
        int total = errandMapper.countErrandOrders(statusList);
        int totalPages = (int) Math.ceil((double) total / pageSize);
        
        System.out.println("数据库查询结果: 找到 " + posts.size() + " 条记录, 总数=" + total);
        
        List<Map<String, Object>> orderList = new ArrayList<>();
        for (int i = 0; i < posts.size(); i++) {
            PostVO post = posts.get(i);
            try {
                System.out.println("开始转换第" + (i+1) + "条记录: postId=" + post.getPostId() + ", title=" + post.getTitle());
                Map<String, Object> orderMap = convertPostToOrderMap(post);
                orderList.add(orderMap);
                System.out.println("成功转换第" + (i+1) + "条记录: id=" + orderMap.get("id") + ", title=" + orderMap.get("title"));
            } catch (Exception e) {
                System.err.println("转换第" + (i+1) + "条记录失败: postId=" + post.getPostId() + ", error=" + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("转换完成，共转换 " + orderList.size() + " 条记录");
        System.out.println("返回结果: orders.size()=" + orderList.size() + ", total=" + total);
        System.out.println("========== 跑腿查询结束 ==========");
        
        result.put("orders", orderList);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);
        
        redisCacheUtil.setWithRandomExpire(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        return result;
    }

    private Map<String, Object> convertPostToOrderMap(PostVO post) {
        System.out.println("转换订单: postId=" + post.getPostId() + ", title=" + post.getTitle());
        
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(post.getPostId()));
        map.put("postId", post.getPostId());
        map.put("title", post.getTitle() != null ? post.getTitle() : "");
        map.put("desc", post.getContent() != null ? post.getContent() : "");
        map.put("from", post.getStartPoint() != null ? post.getStartPoint() : "");
        map.put("to", post.getEndPoint() != null ? post.getEndPoint() : "");
        map.put("price", post.getPrice() != null ? post.getPrice().toString() : "0.00");
        map.put("status", convertStatus(post.getStatus()));
        map.put("tag", post.getUsername() != null ? post.getUsername() : "匿名");
        map.put("publisher", post.getUsername() != null ? post.getUsername() : "匿名");
        map.put("publisherId", post.getUserId());
        map.put("time", formatTime(post.getCreateTime()));
        map.put("remark", post.getItemInfo() != null ? post.getItemInfo() : "");
        
        List<String> images = new ArrayList<>();
        if (post.getImage1() != null && !post.getImage1().isEmpty()) {
            images.add(post.getImage1());
            System.out.println("添加图片1: " + post.getImage1());
        }
        if (post.getImage2() != null && !post.getImage2().isEmpty()) {
            images.add(post.getImage2());
            System.out.println("添加图片2: " + post.getImage2());
        }
        if (post.getImage3() != null && !post.getImage3().isEmpty()) {
            images.add(post.getImage3());
            System.out.println("添加图片3: " + post.getImage3());
        }
        map.put("images", images);
        System.out.println("订单转换完成，图片数量: " + images.size());
        
        return map;
    }

    private String convertStatus(Integer status) {
        if (status == null) {
            return "waiting";
        }
        if (status == 0 || status == 1) {
            return "waiting";
        } else if (status == 3) {
            return "completed";
        } else {
            return "delivering";
        }
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

    @Override
    public boolean isErrandRunner(Integer userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectUserById(userId);
        if (user == null) {
            return false;
        }
        Integer role = user.getRole();
        return role != null && role == 2;
    }

    @Override
    public Map<String, Object> getOrderDetail(Integer orderId) {
        if (orderId == null) {
            return null;
        }
        
        String cacheKey = CACHE_KEY_PREFIX + "detail:" + orderId;
        Map<String, Object> cachedResult = redisCacheUtil.get(cacheKey, Map.class);
        
        if (cachedResult != null) {
            return cachedResult;
        }
        
        PostVO post = errandMapper.selectErrandOrderById(orderId);
        if (post == null) {
            return null;
        }
        Map<String, Object> orderMap = convertPostToOrderMap(post);
        orderMap.put("contactInfo", post.getContactInfo() != null ? post.getContactInfo() : "");
        orderMap.put("itemInfo", post.getItemInfo() != null ? post.getItemInfo() : "");
        orderMap.put("status", post.getStatus());
        orderMap.put("from", post.getStartPoint() != null ? post.getStartPoint() : "暂无地点");
        orderMap.put("to", post.getEndPoint() != null ? post.getEndPoint() : "暂无地点");
        orderMap.put("price", post.getPrice() != null ? post.getPrice().toString() : "0.00");
        boolean showContactAdmin = (post.getStatus() != null && (post.getStatus() == 0 || post.getStatus() == 1));
        orderMap.put("showContactAdmin", showContactAdmin);
        
        try {
            Map<String, Object> tradeTask = errandMapper.selectTradeTaskByPostId(orderId);
            if (tradeTask != null) {
                orderMap.put("acceptorId", tradeTask.get("acceptorId"));
                orderMap.put("acceptorUsername", tradeTask.get("acceptorUsername"));
                orderMap.put("acceptorAvatar", tradeTask.get("acceptorAvatar"));
                orderMap.put("taskStatus", tradeTask.get("taskStatus"));
                orderMap.put("isAccepted", true);
                String taskStartLocation = (String) tradeTask.get("startLocation");
                String taskEndLocation = (String) tradeTask.get("endLocation");
                if (taskStartLocation != null && !taskStartLocation.isEmpty()) {
                    orderMap.put("from", taskStartLocation);
                }
                if (taskEndLocation != null && !taskEndLocation.isEmpty()) {
                    orderMap.put("to", taskEndLocation);
                }
                String contactInfoFromPost = (String) tradeTask.get("contactInfo");
                System.out.println("[DEBUG] getOrderDetail - 从tradeTask获取contactInfo: " + contactInfoFromPost);
                if (contactInfoFromPost != null && !contactInfoFromPost.isEmpty()) {
                    orderMap.put("contactInfo", contactInfoFromPost);
                } else {
                    String contactInfoFromPostObj = post.getContactInfo();
                    System.out.println("[DEBUG] getOrderDetail - tradeTask中contactInfo为空，使用post对象中的contactInfo: " + contactInfoFromPostObj);
                    if (contactInfoFromPostObj != null && !contactInfoFromPostObj.isEmpty()) {
                        orderMap.put("contactInfo", contactInfoFromPostObj);
                    }
                }
                List<String> images = new ArrayList<>();
                Object image1Obj = tradeTask.get("image1");
                Object image2Obj = tradeTask.get("image2");
                Object image3Obj = tradeTask.get("image3");
                System.out.println("[DEBUG] getOrderDetail - 从tradeTask获取图片: image1=" + image1Obj + ", image2=" + image2Obj + ", image3=" + image3Obj);
                if (image1Obj != null && !image1Obj.toString().isEmpty() && !image1Obj.toString().equals("null")) {
                    images.add(image1Obj.toString());
                    orderMap.put("image1", image1Obj);
                }
                if (image2Obj != null && !image2Obj.toString().isEmpty() && !image2Obj.toString().equals("null")) {
                    images.add(image2Obj.toString());
                    orderMap.put("image2", image2Obj);
                }
                if (image3Obj != null && !image3Obj.toString().isEmpty() && !image3Obj.toString().equals("null")) {
                    images.add(image3Obj.toString());
                    orderMap.put("image3", image3Obj);
                }
                System.out.println("[DEBUG] getOrderDetail - 最终图片列表: " + images);
                if (!images.isEmpty()) {
                    orderMap.put("images", images);
                }
            } else {
                orderMap.put("isAccepted", false);
                orderMap.put("acceptorId", null);
                orderMap.put("acceptorUsername", null);
            }
        } catch (Exception e) {
            orderMap.put("isAccepted", false);
            orderMap.put("acceptorId", null);
            orderMap.put("acceptorUsername", null);
        }
        
        redisCacheUtil.setWithRandomExpire(cacheKey, orderMap, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        return orderMap;
    }

    @Override
    public boolean acceptOrder(Integer postId, Integer acceptorId) {
        try {
            PostVO post = errandMapper.selectErrandOrderById(postId);
            if (post == null) {
                System.err.println("接单失败：帖子不存在，postId=" + postId);
                return false;
            }
            
            if (post.getStatus() == null || (post.getStatus() != 0 && post.getStatus() != 1)) {
                System.err.println("接单失败：帖子状态不允许接单，status=" + post.getStatus());
                return false;
            }
            
            int updateResult = errandMapper.updatePostStatusToInProgress(postId);
            if (updateResult <= 0) {
                System.err.println("接单失败：更新帖子状态失败，postId=" + postId);
                return false;
            }
            
            String taskNo = "ER" + System.currentTimeMillis();
            
            int insertResult = errandMapper.insertTradeTask(
                taskNo,
                postId,
                post.getUserId(),
                acceptorId,
                post.getTitle() != null ? post.getTitle() : "跑腿任务",
                post.getContent() != null ? post.getContent() : "",
                post.getPrice() != null ? post.getPrice() : java.math.BigDecimal.ZERO,
                post.getStartPoint(),
                post.getEndPoint(),
                post.getContactInfo(),
                post.getImage1(),
                post.getImage2(),
                post.getImage3()
            );
            
            if (insertResult <= 0) {
                System.err.println("接单失败：创建trade_task失败，postId=" + postId);
                return false;
            }
            
            clearOrderCache(postId);
            
            System.out.println("接单成功：postId=" + postId + ", acceptorId=" + acceptorId + ", taskNo=" + taskNo);
            return true;
        } catch (Exception e) {
            System.err.println("接单异常：postId=" + postId + ", acceptorId=" + acceptorId);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Integer getSuperAdminId() {
        try {
            Integer adminId = messageMapper.selectDefaultAdminId();
            return adminId;
        } catch (Exception e) {
            System.err.println("获取超级管理员ID失败");
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean cancelPublish(Integer postId, Integer userId) {
        try {
            PostVO post = errandMapper.selectErrandOrderById(postId);
            if (post == null) {
                System.err.println("取消发布失败：帖子不存在，postId=" + postId);
                return false;
            }
            if (!post.getUserId().equals(userId)) {
                System.err.println("取消发布失败：不是发布者，postId=" + postId + ", userId=" + userId);
                return false;
            }
            if (post.getStatus() != null && post.getStatus() == 3) {
                System.err.println("取消发布失败：订单已结束，不能取消，postId=" + postId);
                return false;
            }
            int updateResult = errandMapper.updatePostStatusToDeleted(postId, userId);
            if (updateResult <= 0) {
                System.err.println("取消发布失败：更新状态失败，postId=" + postId);
                return false;
            }
            clearOrderCache(postId);
            System.out.println("取消发布成功：postId=" + postId + ", userId=" + userId);
            return true;
        } catch (Exception e) {
            System.err.println("取消发布异常：postId=" + postId + ", userId=" + userId);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean completeOrder(Integer postId, Integer acceptorId) {
        try {
            Map<String, Object> tradeTask = errandMapper.selectTradeTaskByPostId(postId);
            if (tradeTask == null) {
                System.err.println("完成订单失败：订单不存在，postId=" + postId);
                return false;
            }
            Object acceptorIdObj = tradeTask.get("acceptorId");
            if (acceptorIdObj == null || !acceptorIdObj.equals(acceptorId)) {
                System.err.println("完成订单失败：不是接单者，postId=" + postId + ", acceptorId=" + acceptorId);
                return false;
            }
            int updateResult = errandMapper.updateTradeTaskStatusToCompleted(postId, acceptorId);
            if (updateResult <= 0) {
                System.err.println("完成订单失败：更新状态失败，postId=" + postId);
                return false;
            }
            errandMapper.updatePostStatusToCompleted(postId);
            clearOrderCache(postId);
            System.out.println("完成订单成功：postId=" + postId + ", acceptorId=" + acceptorId);
            return true;
        } catch (Exception e) {
            System.err.println("完成订单异常：postId=" + postId + ", acceptorId=" + acceptorId);
            e.printStackTrace();
            return false;
        }
    }
    
    private void clearOrderCache(Integer postId) {
        if (postId != null) {
            redisCacheUtil.clearByPattern(CACHE_KEY_PREFIX + "detail:" + postId);
            redisCacheUtil.clearByPattern(CACHE_KEY_PREFIX + "orders*");
        }
    }
}
