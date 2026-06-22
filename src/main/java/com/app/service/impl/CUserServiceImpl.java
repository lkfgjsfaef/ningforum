package com.app.service.impl;

import com.app.dao.CUserMapper;
import com.app.pojo.CUser;
import com.app.service.CUserService;
import com.app.utils.RedisCacheUtil;
import com.app.mq.producer.MessageProducer;
import com.app.mq.NotificationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CUserServiceImpl implements CUserService {

    @Autowired
    private CUserMapper cUserMapper;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("appRedisCacheUtil")
    private RedisCacheUtil redisCacheUtil;

    @Autowired
    private MessageProducer messageProducer;

    private static final String CACHE_KEY_PREFIX = "user:";
    private static final long CACHE_EXPIRE_MINUTES = 30;

    @Override
    public CUser login(String username, String password) {
        CUser user = cUserMapper.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            if (user.getStatus() == null || user.getStatus() != 0) {
                throw new RuntimeException("BANNED_USER");
            }
            user.setPassword(null);
            return user;
        }
        return null;
    }

    @Override
    public boolean register(CUser user) {
        if (cUserMapper.findByPhone(user.getPhone()) != null) return false;
        if (cUserMapper.findByUsername(user.getUsername()) != null) return false;
        return cUserMapper.insertCUser(user) > 0;
    }

    @Override
    public boolean updateInfo(CUser user) {
        boolean result = cUserMapper.updateCUser(user) > 0;
        
        if (result) {
            clearUserCache(user.getUserId());
        }
        
        return result;
    }
    
    @Override
    public CUser findByPhone(String phone) {
        CUser user = cUserMapper.findByPhone(phone);
        if (user != null) {
            if (user.getStatus() == null || user.getStatus() != 0) {
                throw new RuntimeException("BANNED_USER");
            }
            user.setPassword(null);
        }
        return user;
    }
    
    @Override
    public CUser findByUsername(String username) {
        CUser user = cUserMapper.findByUsername(username);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
    
    @Override
    public CUser getUserInfo(Integer userId) {
        if (userId == null) {
            return null;
        }
        
        String cacheKey = CACHE_KEY_PREFIX + "id:" + userId;
        CUser user = redisCacheUtil.get(cacheKey, CUser.class);
        
        if (user != null) {
            return user;
        }
        
        user = cUserMapper.findById(userId);
        if (user != null) {
            user.setPassword(null);
            redisCacheUtil.setWithRandomExpire(cacheKey, user, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        return user;
    }
    
    @Override
    public List<CUser> getRelationList(Integer userId, Integer type) {
        List<CUser> users = cUserMapper.selectRelationList(userId, type);
        for (CUser user : users) {
            user.setPassword(null);
        }
        return users;
    }
    
    @Override
    public boolean followAction(Integer userId, Integer targetUserId, Integer actionType) {
        try {
            boolean result = false;
            if (actionType == 0) {
                result = cUserMapper.insertFollow(userId, targetUserId) > 0;
                if (result) {
                    try {
                        NotificationMessage notification = new NotificationMessage();
                        notification.setUserId(targetUserId);
                        notification.setFromUserId(userId);
                        notification.setType("follow");
                        notification.setContent("有人关注了你");
                        notification.setCreateTime(new java.util.Date());
                        
                        CUser fromUser = cUserMapper.findById(userId);
                        if (fromUser != null) {
                            notification.setFromUsername(fromUser.getUsername());
                            notification.setFromAvatar(fromUser.getAvatar());
                        }
                        
                        messageProducer.sendNotification(notification);
                    } catch (Exception e) {
                        System.err.println("发送关注通知失败: " + e.getMessage());
                    }
                }
            } else if (actionType == 1) {
                result = cUserMapper.deleteFollow(userId, targetUserId) > 0;
            } else if (actionType == 2) {
                result = cUserMapper.deleteFollow(targetUserId, userId) > 0;
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public Map<String, Integer> getUserStats(Integer userId) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("followingCount", cUserMapper.countFollowing(userId));
        stats.put("fansCount", cUserMapper.countFans(userId));
        stats.put("likeCount", cUserMapper.countLikes(userId));
        return stats;
    }
    
    @Override
    public boolean checkFollowExists(Integer followerId, Integer followingId) {
        Integer followId = cUserMapper.checkFollowExists(followerId, followingId);
        return followId != null;
    }
    
    @Override
    public boolean blockUser(Integer userId, Integer targetUserId) {
        try {
            cUserMapper.deleteFollow(userId, targetUserId);
            return cUserMapper.blockUser(userId, targetUserId) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean unblockUser(Integer userId, Integer targetUserId) {
        try {
            return cUserMapper.unblockUser(userId, targetUserId) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public List<CUser> getBlacklist(Integer userId) {
        List<CUser> users = cUserMapper.selectBlacklist(userId);
        for (CUser user : users) {
            user.setPassword(null);
        }
        return users;
    }
    
    @Override
    public boolean checkBlockExists(Integer userId, Integer targetUserId) {
        Integer blockId = cUserMapper.checkBlockExists(userId, targetUserId);
        return blockId != null;
    }
    
    private void clearUserCache(Integer userId) {
        if (userId != null) {
            redisCacheUtil.delete(CACHE_KEY_PREFIX + "id:" + userId);
        }
    }
}
