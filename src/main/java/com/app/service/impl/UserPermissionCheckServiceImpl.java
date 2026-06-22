package com.app.service.impl;

import com.app.service.UserPermissionCheckService;
import com.pc.dao.UserBanHistoryMapper;
import com.pc.dao.UserPermissionMapper;
import com.pc.pojo.UserBanHistory;
import com.pc.pojo.UserPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserPermissionCheckServiceImpl implements UserPermissionCheckService {
    
    @Autowired
    private UserBanHistoryMapper userBanHistoryMapper;
    
    @Autowired
    private UserPermissionMapper userPermissionMapper;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public Map<String, Object> checkLoginPermission(Integer userId) {
        Map<String, Object> result = new HashMap<>();
        
        if (userId == null) {
            result.put("banned", false);
            return result;
        }
        
        List<UserBanHistory> activeBans = userBanHistoryMapper.selectActiveBanHistoriesByUserId(userId);
        
        for (UserBanHistory ban : activeBans) {
            if (ban.getEndTime() == null || ban.getEndTime().after(new Date())) {
                String restrictionsAfter = ban.getRestrictionsAfter();
                if (restrictionsAfter != null) {
                    try {
                        JsonNode afterNode = objectMapper.readTree(restrictionsAfter);
                        boolean allBanned = true;
                        
                        String[] permissions = {"can_post", "can_comment", "can_like", "can_follow", 
                                               "can_message", "can_buy", "can_sell", "can_run_errand"};
                        for (String perm : permissions) {
                            JsonNode node = afterNode.has(perm) ? afterNode.get(perm) : 
                                          afterNode.has(perm.replace("_", "")) ? afterNode.get(perm.replace("_", "")) : null;
                            if (node != null && node.asInt() == 1) {
                                allBanned = false;
                                break;
                            }
                        }
                        
                        if (allBanned) {
                            int remainingDays = calculateRemainingDays(ban.getEndTime(), ban.getDurationDays());
                            result.put("banned", true);
                            result.put("message", "你已被系统封禁，当前禁止登录小宁论坛");
                            if (remainingDays > 0) {
                                result.put("remainingDays", remainingDays);
                            } else {
                                result.put("permanent", true);
                            }
                            return result;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        result.put("banned", false);
        return result;
    }
    
    @Override
    public Map<String, Object> checkPostPermission(Integer userId) {
        return checkPermission(userId, "can_post", "发布");
    }
    
    @Override
    public Map<String, Object> checkCommentPermission(Integer userId) {
        return checkPermission(userId, "can_comment", "评论");
    }
    
    @Override
    public Map<String, Object> checkMessagePermission(Integer userId) {
        return checkPermission(userId, "can_message", "私信");
    }
    
    private Map<String, Object> checkPermission(Integer userId, String permissionKey, String permissionName) {
        Map<String, Object> result = new HashMap<>();
        
        if (userId == null) {
            result.put("banned", false);
            return result;
        }
        
        UserPermission userPermission = userPermissionMapper.selectByUserId(userId);
        
        if (userPermission == null) {
            result.put("banned", false);
            return result;
        }
        
        Integer permissionValue = null;
        switch (permissionKey) {
            case "can_post":
                permissionValue = userPermission.getCanPost();
                break;
            case "can_comment":
                permissionValue = userPermission.getCanComment();
                break;
            case "can_message":
                permissionValue = userPermission.getCanMessage();
                break;
            case "can_like":
                permissionValue = userPermission.getCanLike();
                break;
            case "can_follow":
                permissionValue = userPermission.getCanFollow();
                break;
            case "can_buy":
                permissionValue = userPermission.getCanBuy();
                break;
            case "can_sell":
                permissionValue = userPermission.getCanSell();
                break;
            case "can_run_errand":
                permissionValue = userPermission.getCanRunErrand();
                break;
        }
        
        if (permissionValue != null && permissionValue == 0) {
            List<UserBanHistory> activeBans = userBanHistoryMapper.selectActiveBanHistoriesByUserId(userId);
            int remainingDays = -1;
            
            for (UserBanHistory ban : activeBans) {
                if (ban.getEndTime() == null || ban.getEndTime().after(new Date())) {
                    remainingDays = calculateRemainingDays(ban.getEndTime(), ban.getDurationDays());
                    break;
                }
            }
            
            result.put("banned", true);
            if (remainingDays > 0) {
                result.put("message", String.format("你已被禁止%s%d天", permissionName, remainingDays));
                result.put("remainingDays", remainingDays);
            } else if (remainingDays == -1) {
                result.put("message", String.format("你已被禁止%s（永久）", permissionName));
                result.put("permanent", true);
            } else {
                result.put("message", String.format("你已被禁止%s", permissionName));
            }
            return result;
        }
        
        result.put("banned", false);
        return result;
    }
    
    private int calculateRemainingDays(Date endTime, Integer durationDays) {
        if (endTime == null) {
            return -1;
        }
        
        if (durationDays != null && durationDays == 0) {
            return -1;
        }
        
        Date now = new Date();
        if (endTime.before(now)) {
            return 0;
        }
        
        long diff = endTime.getTime() - now.getTime();
        long days = diff / (1000 * 60 * 60 * 24);
        
        if (days == 0 && diff > 0) {
            return 1;
        }
        
        return (int) days;
    }
}
