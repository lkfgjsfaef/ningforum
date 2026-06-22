package com.app.service;

import com.app.mq.NotificationMessage;
import java.util.List;
import java.util.Map;

public interface WNotificationService {
    Map<String, Object> getLatestNotification();

    Map<String, Object> getLatestUserNotification(Integer userId);

    Map<String, Object> getNotificationById(Integer messageId);

    Map<String, Object> getNotificationList(Integer page, Integer pageSize);

    List<Map<String, Object>> getUnreadUserNotifications(Integer userId);

    void createNotification(NotificationMessage message);
}
