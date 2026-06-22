package com.pc.service.impl;

import com.app.mq.CacheInvalidationMessage;
import com.app.mq.producer.MessageProducer;
import com.pc.dao.UserDao;
import com.pc.pojo.User;
import com.pc.pojo.UserBanHistory;
import com.pc.pojo.UserPermission;
import com.pc.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pc.utils.RedisCacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserDao userDao;

    @Autowired
    private RedisCacheUtil redisCacheUtil;

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public void setRedisCacheUtil(RedisCacheUtil redisCacheUtil) {
        this.redisCacheUtil = redisCacheUtil;
    }

    public void setMessageProducer(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }
    
    @Autowired
    private MessageProducer messageProducer;

    private ObjectMapper objectMapper = new ObjectMapper();

    private static final String CACHE_KEY_PREFIX = "user:";
    private static final String PERMISSION_CACHE_PREFIX = "user:permission:";
    private static final String BAN_HISTORY_CACHE_PREFIX = "user:ban_history:";
    private static final long CACHE_EXPIRE_MINUTES = 10;

    @Override
    public List<User> findAllUsers() {
        return userDao.findAllUsers();
    }

    @Override
    public User findUserById(Integer userId) {
        if (userId == null) {
            return null;
        }
        
        String cacheKey = CACHE_KEY_PREFIX + "id:" + userId;
        return redisCacheUtil.getWithLock(cacheKey, User.class, () -> userDao.findUserById(userId), CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public User findUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        
        String cacheKey = CACHE_KEY_PREFIX + "username:" + username.trim();
        return redisCacheUtil.getWithLock(cacheKey, User.class, () -> userDao.findUserByUsername(username.trim()), CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public int addUser(User user) {
        int result = userDao.insertUser(user);
        if (result > 0) {
            clearUserCache();
        }
        return result;
    }

    @Override
    public int updateUser(User user) {
        int result = userDao.updateUser(user);
        if (result > 0 && user.getUserId() != null) {
            clearUserCache(user.getUserId());
        }
        return result;
    }

    @Override
    public int deleteUser(Integer userId) {
        int result = userDao.deleteUser(userId);
        if (result > 0) {
            clearUserCache(userId);
        }
        return result;
    }

    @Override
    public List<User> findUsersByStatus(Integer status) {
        return userDao.findUsersByStatus(status);
    }

    @Override
    public int banUser(Integer userId) {
        // 直接调用updateUserStatus方法，只更新状态字段
        return userDao.updateUserStatus(userId, 1); // 1表示已封禁
    }

    @Override
    public int unbanUser(Integer userId) {
        // 直接调用updateUserStatus方法，只更新状态字段
        return userDao.updateUserStatus(userId, 0); // 0表示正常
    }

    @Override
    public int resetPassword(Integer userId) {
        // 重置密码为123456
        return userDao.resetPassword(userId, "123456");
    }

    @Override
    public UserPermission findUserPermissionByUserId(Integer userId) {
        if (userId == null) {
            return null;
        }
        
        String cacheKey = PERMISSION_CACHE_PREFIX + userId;
        return redisCacheUtil.getWithLock(cacheKey, UserPermission.class, () -> userDao.findUserPermissionByUserId(userId), CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public int updateUserPostPermission(Integer userId, Integer canPost, Integer adminId, String reason, Integer durationDays) {
        UserPermission currentPermission = userDao.findUserPermissionByUserId(userId);
        if (currentPermission == null) {
            return 0;
        }

        String restrictionsBefore = generatePermissionJson(currentPermission);

        int result = userDao.updateUserPostPermission(userId, canPost);

        UserPermission updatedPermission = userDao.findUserPermissionByUserId(userId);
        String restrictionsAfter = generatePermissionJson(updatedPermission);

        if (canPost == 0) {
            userDao.deactivateActiveBanHistory(userId, "post");
            insertBanHistory(userId, adminId, "封禁", restrictionsBefore, restrictionsAfter, reason, durationDays);
            syncPermissionFromActiveBanHistory(userId);
        }
        
        clearPermissionCache(userId);

        return result;
    }

    @Override
    public int updateUserCommentPermission(Integer userId, Integer canComment, Integer adminId, String reason, Integer durationDays) {
        UserPermission currentPermission = userDao.findUserPermissionByUserId(userId);
        if (currentPermission == null) {
            return 0;
        }

        String restrictionsBefore = generatePermissionJson(currentPermission);

        int result = userDao.updateUserCommentPermission(userId, canComment);

        UserPermission updatedPermission = userDao.findUserPermissionByUserId(userId);
        String restrictionsAfter = generatePermissionJson(updatedPermission);

        if (canComment == 0) {
            userDao.deactivateActiveBanHistory(userId, "comment");
            insertBanHistory(userId, adminId, "封禁", restrictionsBefore, restrictionsAfter, reason, durationDays);
            syncPermissionFromActiveBanHistory(userId);
        }
        
        clearPermissionCache(userId);

        return result;
    }

    @Override
    public int updateUserMessagePermission(Integer userId, Integer canMessage, Integer adminId, String reason, Integer durationDays) {
        UserPermission currentPermission = userDao.findUserPermissionByUserId(userId);
        if (currentPermission == null) {
            return 0;
        }

        String restrictionsBefore = generatePermissionJson(currentPermission);

        int result = userDao.updateUserMessagePermission(userId, canMessage);

        UserPermission updatedPermission = userDao.findUserPermissionByUserId(userId);
        String restrictionsAfter = generatePermissionJson(updatedPermission);

        if (canMessage == 0) {
            userDao.deactivateActiveBanHistory(userId, "message");
            insertBanHistory(userId, adminId, "封禁", restrictionsBefore, restrictionsAfter, reason, durationDays);
            syncPermissionFromActiveBanHistory(userId);
        }
        
        clearPermissionCache(userId);

        return result;
    }

    @Override
    public List<UserBanHistory> findBanHistoryByUserId(Integer userId) {
        return userDao.findBanHistoryByUserId(userId);
    }

    /**
     * 生成权限JSON字符串
     * @param permission 用户权限对象
     * @return JSON格式的权限字符串
     */
    private String generatePermissionJson(UserPermission permission) {
        try {
            return objectMapper.writeValueAsString(permission);
        } catch (Exception e) {
            // 如果JSON序列化失败，返回简单的权限字符串
            return String.format("{\"can_post\":%d,\"can_comment\":%d,\"can_message\":%d}",
                    permission.getCanPost(), permission.getCanComment(), permission.getCanMessage());
        }
    }

    /**
     * 插入封禁记录
     * @param userId 用户ID
     * @param adminId 管理员ID
     * @param actionType 操作类型：封禁或恢复
     * @param restrictionsBefore 封禁前权限
     * @param restrictionsAfter 封禁后权限
     * @param reason 封禁原因
     * @param durationDays 封禁天数
     */
    private void insertBanHistory(Integer userId, Integer adminId, String actionType,
                                  String restrictionsBefore, String restrictionsAfter,
                                  String reason, Integer durationDays) {
        UserBanHistory banHistory = new UserBanHistory();
        banHistory.setUserId(userId);
        banHistory.setAdminId(adminId);
        banHistory.setActionType(actionType);
        banHistory.setRestrictionsBefore(restrictionsBefore);
        banHistory.setRestrictionsAfter(restrictionsAfter);
        banHistory.setReason(reason);
        banHistory.setDurationDays(durationDays);
        banHistory.setStartTime(new Date());
        banHistory.setIsActive(1);
        banHistory.setCreateTime(new Date());

        // 计算结束时间
        if (durationDays > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_YEAR, durationDays);
            banHistory.setEndTime(calendar.getTime());
        }

        // 插入封禁记录
        userDao.insertBanHistory(banHistory);
    }

    /**
     * 根据is_active=1的封禁记录的restrictions_after同步user_permission表的权限状态
     * 确保user_permission表的权限状态与is_active=1的最新记录的restrictions_after保持一致
     * @param userId 用户ID
     */
    private void syncPermissionFromActiveBanHistory(Integer userId) {
        try {
            // 查询所有is_active=1的封禁记录，按时间倒序排列（SQL已过滤）
            List<UserBanHistory> banHistories = userDao.findBanHistoryByUserId(userId);

            // 找到每个权限对应的最新的is_active=1的记录
            // 由于查询结果已经按start_time DESC排序，第一个匹配的记录就是最新的
            UserBanHistory latestPostBan = null;
            UserBanHistory latestCommentBan = null;
            UserBanHistory latestMessageBan = null;

            for (UserBanHistory banHistory : banHistories) {
                // 查询已过滤is_active=1，这里只需要判断action_type
                if ("封禁".equals(banHistory.getActionType())) {
                    String restrictionsAfter = banHistory.getRestrictionsAfter();
                    if (restrictionsAfter != null) {
                        try {
                            JsonNode afterNode = objectMapper.readTree(restrictionsAfter);

                            // 查找发帖权限的最新封禁记录
                            if (latestPostBan == null) {
                                JsonNode canPostNode = afterNode.has("can_post") ? afterNode.get("can_post") : afterNode.get("canPost");
                                if (canPostNode != null && canPostNode.asInt() == 0) {
                                    latestPostBan = banHistory;
                                }
                            }

                            // 查找评论权限的最新封禁记录
                            if (latestCommentBan == null) {
                                JsonNode canCommentNode = afterNode.has("can_comment") ? afterNode.get("can_comment") : afterNode.get("canComment");
                                if (canCommentNode != null && canCommentNode.asInt() == 0) {
                                    latestCommentBan = banHistory;
                                }
                            }

                            // 查找私信权限的最新封禁记录
                            if (latestMessageBan == null) {
                                JsonNode canMessageNode = afterNode.has("can_message") ? afterNode.get("can_message") : afterNode.get("canMessage");
                                if (canMessageNode != null && canMessageNode.asInt() == 0) {
                                    latestMessageBan = banHistory;
                                }
                            }
                        } catch (Exception e) {
                            // JSON解析失败时，使用字符串匹配
                            if (latestPostBan == null &&
                                    (restrictionsAfter.contains("\"can_post\":0") || restrictionsAfter.contains("\"canPost\":0"))) {
                                latestPostBan = banHistory;
                            }
                            if (latestCommentBan == null &&
                                    (restrictionsAfter.contains("\"can_comment\":0") || restrictionsAfter.contains("\"canComment\":0"))) {
                                latestCommentBan = banHistory;
                            }
                            if (latestMessageBan == null &&
                                    (restrictionsAfter.contains("\"can_message\":0") || restrictionsAfter.contains("\"canMessage\":0"))) {
                                latestMessageBan = banHistory;
                            }
                        }
                    }
                }
            }

            // 根据找到的最新封禁记录的restrictions_after来更新user_permission表
            UserPermission currentPermission = userDao.findUserPermissionByUserId(userId);
            if (currentPermission != null) {
                // 更新发帖权限
                if (latestPostBan != null) {
                    try {
                        JsonNode afterNode = objectMapper.readTree(latestPostBan.getRestrictionsAfter());
                        JsonNode canPostNode = afterNode.has("can_post") ? afterNode.get("can_post") : afterNode.get("canPost");
                        if (canPostNode != null && canPostNode.asInt() != currentPermission.getCanPost()) {
                            userDao.updateUserPostPermission(userId, canPostNode.asInt());
                        }
                    } catch (Exception e) {
                        // 解析失败时不更新
                    }
                }

                // 更新评论权限
                if (latestCommentBan != null) {
                    try {
                        JsonNode afterNode = objectMapper.readTree(latestCommentBan.getRestrictionsAfter());
                        JsonNode canCommentNode = afterNode.has("can_comment") ? afterNode.get("can_comment") : afterNode.get("canComment");
                        if (canCommentNode != null && canCommentNode.asInt() != currentPermission.getCanComment()) {
                            userDao.updateUserCommentPermission(userId, canCommentNode.asInt());
                        }
                    } catch (Exception e) {
                        // 解析失败时不更新
                    }
                }

                // 更新私信权限
                if (latestMessageBan != null) {
                    try {
                        JsonNode afterNode = objectMapper.readTree(latestMessageBan.getRestrictionsAfter());
                        JsonNode canMessageNode = afterNode.has("can_message") ? afterNode.get("can_message") : afterNode.get("canMessage");
                        if (canMessageNode != null && canMessageNode.asInt() != currentPermission.getCanMessage()) {
                            userDao.updateUserMessagePermission(userId, canMessageNode.asInt());
                        }
                    } catch (Exception e) {
                        // 解析失败时不更新
                    }
                }
            }
        } catch (Exception e) {
            // 同步失败时不影响主流程，记录错误即可
            e.printStackTrace();
        }
    }
    
    public void clearUserCache() {
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
    
    public void clearUserCache(Integer userId) {
        if (userId == null) {
            return;
        }
        redisCacheUtil.delete(CACHE_KEY_PREFIX + "id:" + userId);
        redisCacheUtil.delete(CACHE_KEY_PREFIX + "username:" + userId);
        redisCacheUtil.delete(PERMISSION_CACHE_PREFIX + userId);
        redisCacheUtil.delete(BAN_HISTORY_CACHE_PREFIX + userId);
        
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
    
    public void clearPermissionCache(Integer userId) {
        if (userId == null) {
            return;
        }
        redisCacheUtil.delete(PERMISSION_CACHE_PREFIX + userId);
    }
    
    public void clearBanHistoryCache(Integer userId) {
        if (userId == null) {
            return;
        }
        redisCacheUtil.delete(BAN_HISTORY_CACHE_PREFIX + userId);
    }
}
