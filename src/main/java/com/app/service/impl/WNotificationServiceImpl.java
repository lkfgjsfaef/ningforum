package com.app.service.impl;

import com.app.dao.WNotificationMapper;
import com.app.mq.NotificationMessage;
import com.app.service.WNotificationService;
import com.pc.pojo.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class WNotificationServiceImpl implements WNotificationService {

    @Autowired
    private WNotificationMapper notificationMapper;

    @Override
    public Map<String, Object> getLatestNotification() {
        System.out.println("========== 开始查询最新系统通知 ==========");
        Message message = notificationMapper.selectLatestSystemNotification();
        System.out.println("查询结果: " + (message != null ? "找到通知, messageId=" + message.getMessageId() + ", title=" + message.getTitle() : "没有找到通知"));
        if (message == null) {
            return null;
        }
        Map<String, Object> result = convertMessageToMap(message);
        System.out.println("转换后的通知数据: " + result);
        System.out.println("========== 查询完成 ==========");
        return result;
    }

    @Override
    public Map<String, Object> getLatestUserNotification(Integer userId) {
        System.out.println("========== 开始查询用户最新系统通知 ==========");
        System.out.println("用户ID: " + userId);
        if (userId == null) {
            System.out.println("用户ID为null，返回null");
            return null;
        }
        Message message = notificationMapper.selectLatestUserSystemNotification(userId);
        System.out.println("查询结果: " + (message != null ? "找到通知, messageId=" + message.getMessageId() + ", title=" + message.getTitle() : "没有找到通知"));
        if (message == null) {
            System.out.println("用户没有专属通知，查询全局最新通知");
            return getLatestNotification();
        }
        Map<String, Object> result = convertMessageToMap(message);
        System.out.println("转换后的通知数据: " + result);
        System.out.println("========== 查询完成 ==========");
        return result;
    }

    @Override
    public Map<String, Object> getNotificationById(Integer messageId) {
        Message message = notificationMapper.selectNotificationById(messageId);
        if (message == null) {
            return null;
        }
        return convertMessageToMap(message);
    }

    @Override
    public Map<String, Object> getNotificationList(Integer page, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        int start = (page - 1) * pageSize;
        List<Message> messages = notificationMapper.selectSystemNotifications(start, pageSize);
        int total = notificationMapper.countSystemNotifications();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        
        List<Map<String, Object>> notificationList = new ArrayList<>();
        for (Message message : messages) {
            notificationList.add(convertMessageToMap(message));
        }
        
        result.put("notifications", notificationList);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);
        
        return result;
    }

    private Map<String, Object> convertMessageToMap(Message message) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(message.getMessageId()));
        map.put("messageId", message.getMessageId());
        map.put("title", message.getTitle() != null ? message.getTitle() : "");
        map.put("content", message.getContent() != null ? message.getContent() : "");
        map.put("time", formatTime(message.getCreateTime()));
        
        List<String> images = new ArrayList<>();
        if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
            images.add(message.getImageUrl());
        }
        map.put("images", images);
        map.put("imageUrl", message.getImageUrl() != null ? message.getImageUrl() : "");
        
        return map;
    }

    @Override
    public List<Map<String, Object>> getUnreadUserNotifications(Integer userId) {
        System.out.println("========== 开始查询用户未读特定通知 ==========");
        System.out.println("用户ID: " + userId);
        if (userId == null) {
            System.out.println("用户ID为null，返回空列表");
            return new ArrayList<>();
        }
        
        List<Message> messages = new ArrayList<>();
        
        List<Message> receiverMessages = notificationMapper.selectUnreadUserNotifications(userId);
        if (receiverMessages != null) {
            messages.addAll(receiverMessages);
        }
        
        List<Message> directMessages = notificationMapper.selectUnreadDirectUserNotifications(userId);
        if (directMessages != null) {
            messages.addAll(directMessages);
        }
        
        Map<Integer, Message> messageMap = new HashMap<>();
        for (Message message : messages) {
            messageMap.put(message.getMessageId(), message);
        }
        
        List<Message> sortedMessages = new ArrayList<>(messageMap.values());
        sortedMessages.sort((m1, m2) -> {
            if (m1.getCreateTime() == null && m2.getCreateTime() == null) return 0;
            if (m1.getCreateTime() == null) return 1;
            if (m2.getCreateTime() == null) return -1;
            return m2.getCreateTime().compareTo(m1.getCreateTime());
        });
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Message message : sortedMessages) {
            result.add(convertMessageToMap(message));
        }
        
        System.out.println("查询到 " + result.size() + " 条未读特定通知");
        System.out.println("========== 查询完成 ==========");
        return result;
    }

    private String formatTime(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(date);
    }

    @Override
    public void createNotification(NotificationMessage message) {
        if (message.getUserId() == null) {
            return;
        }
        
        try {
            Message notification = new Message();
            notification.setReceiverId(message.getUserId());
            notification.setTitle(message.getType());
            notification.setContent(message.getContent());
            notification.setCreateTime(new java.util.Date());
            notification.setIsRead(0);
            notification.setMessageType(1); // 系统通知类型
            
            notificationMapper.insertNotification(notification);
            System.out.println("创建通知成功: userId=" + message.getUserId() + ", type=" + message.getType());
        } catch (Exception e) {
            System.err.println("创建通知失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
