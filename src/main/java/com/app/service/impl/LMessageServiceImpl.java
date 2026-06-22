package com.app.service.impl;

import com.app.dao.CUserMapper;
import com.app.dao.LMessageMapper;
import com.app.mq.ChatMessage;
import com.app.mq.producer.MessageProducer;
import com.app.pojo.CUser;
import com.app.service.LMessageService;
import com.app.service.UserPermissionCheckService;
import com.pc.pojo.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class LMessageServiceImpl implements LMessageService {
    
    @Autowired
    private LMessageMapper lMessageMapper;
    
    @Autowired
    private UserPermissionCheckService permissionCheckService;
    
    @Autowired
    private CUserMapper cUserMapper;
    
    @Autowired
    private MessageProducer messageProducer;
    
    @Override
    public List<Map<String, Object>> getMutualFollowMessages(Integer currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        try {
            return lMessageMapper.selectMutualFollowMessages(currentUserId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public List<Map<String, Object>> getFanMessages(Integer currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        try {
            return lMessageMapper.selectFanMessages(currentUserId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public List<Message> getMessagesBetweenUsers(Integer currentUserId, Integer otherUserId) {
        if (currentUserId == null || otherUserId == null) {
            return null;
        }
        try {
            return lMessageMapper.selectMessagesBetweenUsers(currentUserId, otherUserId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public boolean sendMessage(Integer senderId, Integer receiverId, String content, Integer msgFormat, String imageUrl) {
        if (senderId == null || receiverId == null) {
            return false;
        }
        try {
            Map<String, Object> permissionCheck = permissionCheckService.checkMessagePermission(senderId);
            if (Boolean.TRUE.equals(permissionCheck.get("banned"))) {
                throw new RuntimeException("BANNED_MESSAGE:" + permissionCheck.get("message"));
            }
            
            Integer blockId = cUserMapper.checkBlockExists(receiverId, senderId);
            if (blockId != null) {
                throw new RuntimeException("你已被对方拉黑，无法发送消息");
            }
            
            if (msgFormat == null) {
                msgFormat = 0;
            }
            
            boolean isImageMessage = (msgFormat != null && msgFormat == 1);
            if (isImageMessage) {
                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    return false;
                }
            } else {
                if (content == null || content.trim().isEmpty()) {
                    return false;
                }
            }
            
            int result = lMessageMapper.insertPrivateMessage(
                senderId, 
                receiverId, 
                content != null ? content.trim() : "", 
                msgFormat, 
                imageUrl != null ? imageUrl.trim() : null
            );
            
            if (result > 0) {
                // 发送消息到队列，用于实时推送
                try {
                    CUser senderUser = cUserMapper.findById(senderId);
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.setFromUserId(senderId);
                    chatMessage.setFromUsername(senderUser != null ? senderUser.getUsername() : "未知用户");
                    chatMessage.setFromAvatar(senderUser != null ? senderUser.getAvatar() : "");
                    chatMessage.setToUserId(receiverId);
                    chatMessage.setContent(content != null ? content.trim() : "");
                    chatMessage.setCreateTime(new java.util.Date());
                    
                    messageProducer.sendChatMessage(chatMessage);
                } catch (Exception e) {
                    // 消息队列发送失败不影响主流程
                    e.printStackTrace();
                }
            }
            
            return result > 0;
        } catch (RuntimeException e) {
            if (e.getMessage() != null && (e.getMessage().startsWith("BANNED_MESSAGE:") || e.getMessage().contains("拉黑"))) {
                throw e;
            }
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public void markMessagesAsRead(Integer currentUserId, Integer otherUserId) {
        if (currentUserId == null || otherUserId == null) {
            return;
        }
        try {
            lMessageMapper.updateMessagesReadBetweenUsers(currentUserId, otherUserId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public Integer getTotalUnreadCount(Integer currentUserId) {
        if (currentUserId == null) {
            return 0;
        }
        try {
            Integer count = lMessageMapper.countTotalUnreadMessages(currentUserId);
            return count != null ? count : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    @Override
    public Integer getMutualFollowUnreadCount(Integer currentUserId) {
        if (currentUserId == null) {
            return 0;
        }
        try {
            Integer count = lMessageMapper.countMutualFollowUnreadMessages(currentUserId);
            return count != null ? count : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    @Override
    public Integer getFanUnreadCount(Integer currentUserId) {
        if (currentUserId == null) {
            return 0;
        }
        try {
            Integer count = lMessageMapper.countFanUnreadMessages(currentUserId);
            return count != null ? count : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    @Override
    public List<Map<String, Object>> getAdminMessages(Integer currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        try {
            return lMessageMapper.selectAdminMessages(currentUserId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public Integer getAdminUnreadCount(Integer currentUserId) {
        if (currentUserId == null) {
            return 0;
        }
        try {
            Integer count = lMessageMapper.countAdminUnreadMessages(currentUserId);
            return count != null ? count : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    @Override
    public void pushMessageToUser(ChatMessage message) {
        if (message.getToUserId() == null) {
            return;
        }
        
        try {
            System.out.println("推送消息给用户: toUserId=" + message.getToUserId() + ", content=" + message.getContent());
            
            // 使用insertPrivateMessage方法插入消息
            int result = lMessageMapper.insertPrivateMessage(
                message.getFromUserId(),
                message.getToUserId(),
                message.getContent(),
                0, // msgFormat: 0表示文本消息
                null // imageUrl: 文本消息没有图片
            );
            
            if (result > 0) {
                System.out.println("消息推送成功");
            } else {
                System.err.println("消息推送失败：插入数据库失败");
            }
        } catch (Exception e) {
            System.err.println("推送消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
