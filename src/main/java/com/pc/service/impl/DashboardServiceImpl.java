package com.pc.service.impl;

import com.pc.dao.DashboardPostDao;
import com.pc.dao.UserDao;
import com.pc.pojo.User;
import com.pc.pojo.UserBanHistory;
import com.pc.service.DashboardService;
import com.pc.utils.RedisCacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {
    @Autowired
    private DashboardPostDao dashboardPostDao;

    @Autowired
    private UserDao userDao;
    
    @Autowired
    private RedisCacheUtil redisCacheUtil;

    private ObjectMapper mapper = new ObjectMapper();

    private static final String CACHE_KEY_PREFIX = "dashboard:";
    private static final long CACHE_EXPIRE_MINUTES = 2;

    @Override
    public int countTodayPosts() {
        String cacheKey = CACHE_KEY_PREFIX + "today_posts";
        Integer count = redisCacheUtil.get(cacheKey, Integer.class);
        
        if (count != null) {
            return count;
        }
        
        count = dashboardPostDao.countTodayPosts();
        redisCacheUtil.setWithRandomExpire(cacheKey, count, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        return count;
    }

    @Override
    public int countPendingReports() {
        String cacheKey = CACHE_KEY_PREFIX + "pending_reports";
        Integer count = redisCacheUtil.get(cacheKey, Integer.class);
        
        if (count != null) {
            return count;
        }
        
        count = dashboardPostDao.countPendingReports();
        redisCacheUtil.setWithRandomExpire(cacheKey, count, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        return count;
    }

    @Override
    public int countBannedUsers() {
        String cacheKey = CACHE_KEY_PREFIX + "banned_users";
        Integer count = redisCacheUtil.get(cacheKey, Integer.class);
        
        if (count != null) {
            return count;
        }
        
        count = dashboardPostDao.countBannedUsers();
        redisCacheUtil.setWithRandomExpire(cacheKey, count, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        return count;
    }

    @Override
    public List<Map<String, Object>> getLast7DaysNewUsers() {
        String cacheKey = CACHE_KEY_PREFIX + "last7days_users";
        List<Map<String, Object>> result = redisCacheUtil.get(cacheKey, List.class);
        
        if (result != null) {
            return result;
        }
        
        result = dashboardPostDao.getLast7DaysNewUsers();
        if (result != null && !result.isEmpty()) {
            redisCacheUtil.setWithRandomExpire(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getLast7DaysInteractions() {
        String cacheKey = CACHE_KEY_PREFIX + "last7days_interactions";
        List<Map<String, Object>> result = redisCacheUtil.get(cacheKey, List.class);
        
        if (result != null) {
            return result;
        }
        
        result = dashboardPostDao.getLast7DaysInteractions();
        if (result != null && !result.isEmpty()) {
            redisCacheUtil.setWithRandomExpire(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getLast7DaysNewReports() {
        String cacheKey = CACHE_KEY_PREFIX + "last7days_reports";
        List<Map<String, Object>> result = redisCacheUtil.get(cacheKey, List.class);
        
        if (result != null) {
            return result;
        }
        
        result = dashboardPostDao.getLast7DaysNewReports();
        if (result != null && !result.isEmpty()) {
            redisCacheUtil.setWithRandomExpire(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getReportTypeCounts() {
        String cacheKey = CACHE_KEY_PREFIX + "report_type_counts";
        List<Map<String, Object>> result = redisCacheUtil.get(cacheKey, List.class);
        
        if (result != null) {
            return result;
        }
        
        result = dashboardPostDao.getReportTypeCounts();
        if (result != null && !result.isEmpty()) {
            redisCacheUtil.setWithRandomExpire(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getCategoryPostCounts() {
        String cacheKey = CACHE_KEY_PREFIX + "category_post_counts";
        List<Map<String, Object>> result = redisCacheUtil.get(cacheKey, List.class);
        
        if (result != null) {
            return result;
        }
        
        result = dashboardPostDao.getCategoryPostCounts();
        if (result != null && !result.isEmpty()) {
            redisCacheUtil.setWithRandomExpire(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getUserGenderCounts() {
        String cacheKey = CACHE_KEY_PREFIX + "user_gender_counts";
        List<Map<String, Object>> result = redisCacheUtil.get(cacheKey, List.class);
        
        if (result != null) {
            return result;
        }
        
        result = dashboardPostDao.getUserGenderCounts();
        if (result != null && !result.isEmpty()) {
            redisCacheUtil.setWithRandomExpire(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getPostStatusCounts() {
        String cacheKey = CACHE_KEY_PREFIX + "post_status_counts";
        List<Map<String, Object>> result = redisCacheUtil.get(cacheKey, List.class);
        
        if (result != null) {
            return result;
        }
        
        result = dashboardPostDao.getPostStatusCounts();
        if (result != null && !result.isEmpty()) {
            redisCacheUtil.setWithRandomExpire(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getLatestUsers() {
        String cacheKey = CACHE_KEY_PREFIX + "latest_users";
        List<Map<String, Object>> result = redisCacheUtil.get(cacheKey, List.class);
        
        if (result != null) {
            return result;
        }
        
        List<User> allUsers = userDao.findAllUsers();
        if (allUsers == null || allUsers.isEmpty()) {
            return new ArrayList<>();
        }

        result = allUsers.stream()
                .sorted((u1, u2) -> {
                    if (u1.getCreateTime() == null && u2.getCreateTime() == null) return 0;
                    if (u1.getCreateTime() == null) return 1;
                    if (u2.getCreateTime() == null) return -1;
                    return u2.getCreateTime().compareTo(u1.getCreateTime());
                })
                .limit(50)
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", u.getUserId());
                    map.put("avatar", u.getAvatar());
                    map.put("username", u.getUsername());
                    map.put("gender", u.getGender());
                    map.put("phone", u.getPhone());
                    map.put("email", u.getEmail());
                    map.put("createTime", u.getCreateTime());
                    map.put("status", u.getStatus());
                    return map;
                })
                .collect(Collectors.toList());
        
        if (result != null && !result.isEmpty()) {
            redisCacheUtil.setWithRandomExpire(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getBanRecords() {
        String cacheKey = CACHE_KEY_PREFIX + "ban_records";
        List<Map<String, Object>> result = redisCacheUtil.get(cacheKey, List.class);
        
        if (result != null) {
            return result;
        }
        
        List<User> users = userDao.findAllUsers();
        if (users == null || users.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Integer, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));

        List<Map<String, Object>> records = new ArrayList<>();

        for (User user : users) {
            List<UserBanHistory> historyList = userDao.findBanHistoryByUserId(user.getUserId());
            if (historyList == null || historyList.isEmpty()) {
                continue;
            }

            for (UserBanHistory h : historyList) {
                Map<String, Object> map = new HashMap<>();
                map.put("historyId", h.getHistoryId());

                User admin = userMap.get(h.getAdminId());
                map.put("adminName", admin != null ? admin.getUsername() : ("管理员ID:" + h.getAdminId()));

                map.put("bannedUserName", user.getUsername());
                map.put("reason", h.getReason());
                map.put("startTime", h.getStartTime());
                map.put("createTime", h.getCreateTime());

                map.put("durationDays", h.getDurationDays());

                String banPermissions = "";
                try {
                    if (h.getRestrictionsAfter() != null && !h.getRestrictionsAfter().isEmpty()) {
                        Map<String, Object> afterMap = mapper.readValue(
                                h.getRestrictionsAfter(),
                                new TypeReference<Map<String, Object>>() {});
                        List<String> denied = new ArrayList<>();
                        addIfDenied(afterMap, denied, "can_post", "发帖");
                        addIfDenied(afterMap, denied, "can_comment", "评论");
                        addIfDenied(afterMap, denied, "can_like", "点赞");
                        addIfDenied(afterMap, denied, "can_follow", "关注");
                        addIfDenied(afterMap, denied, "can_message", "私信");
                        addIfDenied(afterMap, denied, "can_buy", "购买");
                        addIfDenied(afterMap, denied, "can_sell", "出售");
                        addIfDenied(afterMap, denied, "can_run_errand", "跑腿");
                        banPermissions = denied.isEmpty() ? "未解析" : String.join("、", denied);
                    }
                } catch (Exception ignore) {
                    banPermissions = "未解析";
                }
                map.put("banPermissions", banPermissions);

                records.add(map);
            }
        }

        result = records.stream()
                .sorted((m1, m2) -> {
                    Object t1 = m1.get("createTime");
                    Object t2 = m2.get("createTime");
                    if (t1 == null && t2 == null) return 0;
                    if (t1 == null) return 1;
                    if (t2 == null) return -1;
                    return ((java.util.Date) t2).compareTo((java.util.Date) t1);
                })
                .limit(50)
                .collect(Collectors.toList());
        
        if (result != null && !result.isEmpty()) {
            redisCacheUtil.setWithRandomExpire(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        return result;
    }

    private void addIfDenied(Map<String, Object> map, List<String> denied, String key, String label) {
        if (map == null) return;
        Object v = map.get(key);
        if (v == null) {
            // 再试驼峰
            String camel = toCamelCase(key);
            v = map.get(camel);
        }
        if (v != null) {
            try {
                int iv = Integer.parseInt(v.toString());
                if (iv == 0) {
                    denied.add(label);
                }
            } catch (Exception ignored) {}
        }
    }

    private String toCamelCase(String snake) {
        if (snake == null || snake.isEmpty()) return snake;
        StringBuilder sb = new StringBuilder();
        boolean upperNext = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') {
                upperNext = true;
                continue;
            }
            if (upperNext) {
                sb.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    public void clearDashboardCache() {
        redisCacheUtil.clearByPattern(CACHE_KEY_PREFIX + "*");
    }
}

