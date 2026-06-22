package com.pc.service.impl;

import com.pc.dao.SystemSettingMapper;
import com.pc.dao.UserMapper;
import com.pc.pojo.SystemSetting;
import com.pc.pojo.User;
import com.pc.service.SystemManageService;
import com.pc.utils.RedisCacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class SystemManageServiceImpl implements SystemManageService {

    @Autowired
    private SystemSettingMapper settingMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private RedisCacheUtil redisCacheUtil;

    private static final String CACHE_KEY_PREFIX = "system:";
    private static final long CACHE_EXPIRE_MINUTES = 60;

    @Override
    public SystemSetting getSetting() {
        String cacheKey = CACHE_KEY_PREFIX + "setting";
        SystemSetting setting = redisCacheUtil.get(cacheKey, SystemSetting.class);
        
        if (setting != null) {
            return setting;
        }
        
        setting = settingMapper.selectSetting();
        if (setting == null) {
            setting = new SystemSetting();
            setting.setId(1);
        }
        
        redisCacheUtil.setWithRandomExpire(cacheKey, setting, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        return setting;
    }

    @Override
    public boolean updateSetting(SystemSetting setting) {
        boolean result = settingMapper.updateSetting(setting) > 0;
        
        if (result) {
            clearSystemCache();
        }
        
        return result;
    }

    @Override
    public List<User> getAdminList(String keyword) {
        return userMapper.selectAdminList(keyword);
    }

    @Override
    public boolean addAdmin(User user) {
        if (userMapper.selectUserByUsername(user.getUsername()) != null) {
            return false;
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword("123456");
        }
        if (user.getCreateTime() == null) {
            user.setCreateTime(new Date());
        }
        return userMapper.insertUser(user) > 0;
    }

    @Override
    public boolean deleteAdmin(Integer userId) {
        return userMapper.deleteUserById(userId) > 0;
    }

    @Override
    public boolean updateAdmin(User user) {
        if (user.getPassword() != null && user.getPassword().trim().isEmpty()) {
            user.setPassword(null);
        }
        return userMapper.updateUser(user) > 0;
    }
    
    public void clearSystemCache() {
        redisCacheUtil.clearByPattern(CACHE_KEY_PREFIX + "*");
    }
}