package com.app.service;

import com.app.mq.ChatMessage;
import com.pc.pojo.Message;
import java.util.List;
import java.util.Map;

public interface LMessageService {
    
    List<Map<String, Object>> getMutualFollowMessages(Integer currentUserId);
    
    List<Map<String, Object>> getFanMessages(Integer currentUserId);
    
    List<Message> getMessagesBetweenUsers(Integer currentUserId, Integer otherUserId);
    
    boolean sendMessage(Integer senderId, Integer receiverId, String content, Integer msgFormat, String imageUrl);
    
    void markMessagesAsRead(Integer currentUserId, Integer otherUserId);
    
    Integer getTotalUnreadCount(Integer currentUserId);
    
    Integer getMutualFollowUnreadCount(Integer currentUserId);
    
    Integer getFanUnreadCount(Integer currentUserId);
    
    List<Map<String, Object>> getAdminMessages(Integer currentUserId);
    
    Integer getAdminUnreadCount(Integer currentUserId);
    
    void pushMessageToUser(ChatMessage message);
}
