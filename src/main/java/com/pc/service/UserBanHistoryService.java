package com.pc.service;

public interface UserBanHistoryService {
    // 处理过期封禁的权限恢复
    void recoverExpiredBanPermission();
}