package com.app.controller;

import com.app.common.CResult;
import com.app.service.LMessageService;
import com.pc.pojo.Message;
import com.pc.utils.OssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cuser/message")
public class LMessageController {
    
    @Autowired
    private LMessageService lMessageService;
    
    @GetMapping("/mutualFollow")
    public CResult<List<Map<String, Object>>> getMutualFollowMessages(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            List<Map<String, Object>> messages = lMessageService.getMutualFollowMessages(userId);
            if (messages != null) {
                return CResult.success("查询成功", messages);
            } else {
                return CResult.error("查询失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return CResult.error("查询互关私信时发生异常: " + e.getMessage());
        }
    }
    
    @GetMapping("/fan")
    public CResult<List<Map<String, Object>>> getFanMessages(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            List<Map<String, Object>> messages = lMessageService.getFanMessages(userId);
            if (messages != null) {
                return CResult.success("查询成功", messages);
            } else {
                return CResult.error("查询失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return CResult.error("查询粉丝来信时发生异常: " + e.getMessage());
        }
    }
    
    @GetMapping("/conversation")
    public CResult<List<Message>> getConversation(@RequestParam("userId") Integer userId,
                                                   @RequestParam("otherUserId") Integer otherUserId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            if (otherUserId == null) {
                return CResult.error("对方用户ID不能为空");
            }
            
            List<Message> messages = lMessageService.getMessagesBetweenUsers(userId, otherUserId);
            if (messages != null) {
                lMessageService.markMessagesAsRead(userId, otherUserId);
                return CResult.success("查询成功", messages);
            } else {
                return CResult.error("查询失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return CResult.error("查询会话记录时发生异常: " + e.getMessage());
        }
    }
    
    @PostMapping("/send")
    public CResult<Object> sendMessage(@RequestParam("userId") Integer userId,
                                       @RequestParam("receiverId") Integer receiverId,
                                       @RequestParam("content") String content) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            if (receiverId == null) {
                return CResult.error("接收者ID不能为空");
            }
            if (content == null || content.trim().isEmpty()) {
                return CResult.error("消息内容不能为空");
            }
            
            boolean success = lMessageService.sendMessage(userId, receiverId, content.trim(), 0, null);
            if (success) {
                return CResult.success("发送成功", null);
            } else {
                return CResult.error("发送失败");
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("BANNED_MESSAGE:")) {
                String message = e.getMessage().substring("BANNED_MESSAGE:".length());
                return CResult.error(message);
            }
            if (e.getMessage() != null && e.getMessage().contains("拉黑")) {
                return CResult.error(e.getMessage());
            }
            e.printStackTrace();
            return CResult.error("发送消息时发生异常: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return CResult.error("发送消息时发生异常: " + e.getMessage());
        }
    }
    
    @PostMapping("/uploadImage")
    public CResult<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return CResult.error("请选择要上传的图片");
            }
            
            long maxSize = 10 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return CResult.error("图片大小不能超过10MB");
            }
            
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                return CResult.error("文件名无效");
            }
            
            String extension = "";
            int lastDotIndex = originalFilename.lastIndexOf('.');
            if (lastDotIndex > 0) {
                extension = originalFilename.substring(lastDotIndex).toLowerCase();
            }
            
            String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};
            boolean isValidExtension = false;
            for (String ext : allowedExtensions) {
                if (extension.equals(ext)) {
                    isValidExtension = true;
                    break;
                }
            }
            
            if (!isValidExtension) {
                return CResult.error("只支持JPG、PNG、GIF、BMP、WEBP格式的图片");
            }
            
            String imageUrl = null;
            InputStream inputStream = null;
            try {
                inputStream = file.getInputStream();
                imageUrl = OssUtil.uploadFile(inputStream, originalFilename);
            } catch (Exception uploadException) {
                uploadException.printStackTrace();
                return CResult.error("图片上传失败: " + (uploadException.getMessage() != null ? uploadException.getMessage() : "未知错误"));
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                return CResult.success("图片上传成功", imageUrl);
            } else {
                return CResult.error("图片上传失败：未获取到图片URL");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return CResult.error("上传图片时发生异常: " + e.getMessage());
        }
    }
    
    @PostMapping("/sendImage")
    public CResult<Object> sendImageMessage(@RequestParam("userId") Integer userId,
                                            @RequestParam("receiverId") Integer receiverId,
                                            @RequestParam("imageUrl") String imageUrl,
                                            @RequestParam(value = "content", required = false) String content) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            if (receiverId == null) {
                return CResult.error("接收者ID不能为空");
            }
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                return CResult.error("图片URL不能为空");
            }
            
            boolean success = lMessageService.sendMessage(
                userId, 
                receiverId, 
                content != null ? content.trim() : "", 
                1, 
                imageUrl.trim()
            );
            
            if (success) {
                return CResult.success("发送成功", null);
            } else {
                return CResult.error("发送失败");
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("BANNED_MESSAGE:")) {
                String message = e.getMessage().substring("BANNED_MESSAGE:".length());
                return CResult.error(message);
            }
            if (e.getMessage() != null && e.getMessage().contains("拉黑")) {
                return CResult.error(e.getMessage());
            }
            e.printStackTrace();
            return CResult.error("发送图片消息时发生异常: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return CResult.error("发送图片消息时发生异常: " + e.getMessage());
        }
    }
    
    @PostMapping("/markRead")
    public CResult<Object> markAsRead(@RequestParam("userId") Integer userId,
                                      @RequestParam("otherUserId") Integer otherUserId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            if (otherUserId == null) {
                return CResult.error("对方用户ID不能为空");
            }
            
            lMessageService.markMessagesAsRead(userId, otherUserId);
            return CResult.success("标记成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return CResult.error("标记已读时发生异常: " + e.getMessage());
        }
    }
    
    @GetMapping("/totalUnreadCount")
    public CResult<Integer> getTotalUnreadCount(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            Integer count = lMessageService.getTotalUnreadCount(userId);
            return CResult.success("查询成功", count);
        } catch (Exception e) {
            e.printStackTrace();
            return CResult.error("查询总未读数时发生异常: " + e.getMessage());
        }
    }
    
    @GetMapping("/mutualFollowUnreadCount")
    public CResult<Integer> getMutualFollowUnreadCount(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            Integer count = lMessageService.getMutualFollowUnreadCount(userId);
            return CResult.success("查询成功", count);
        } catch (Exception e) {
            e.printStackTrace();
            return CResult.error("查询互关私信未读数时发生异常: " + e.getMessage());
        }
    }
    
    @GetMapping("/fanUnreadCount")
    public CResult<Integer> getFanUnreadCount(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            Integer count = lMessageService.getFanUnreadCount(userId);
            return CResult.success("查询成功", count);
        } catch (Exception e) {
            e.printStackTrace();
            return CResult.error("查询粉丝来信未读数时发生异常: " + e.getMessage());
        }
    }
    
    @GetMapping("/allUnreadCounts")
    public CResult<Map<String, Integer>> getAllUnreadCounts(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            Map<String, Integer> counts = new java.util.HashMap<>();
            counts.put("total", lMessageService.getTotalUnreadCount(userId));
            counts.put("mutualFollow", lMessageService.getMutualFollowUnreadCount(userId));
            counts.put("fan", lMessageService.getFanUnreadCount(userId));
            counts.put("admin", lMessageService.getAdminUnreadCount(userId));
            
            return CResult.success("查询成功", counts);
        } catch (Exception e) {
            e.printStackTrace();
            return CResult.error("查询未读数时发生异常: " + e.getMessage());
        }
    }
    
    @GetMapping("/admin")
    public CResult<List<Map<String, Object>>> getAdminMessages(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            List<Map<String, Object>> messages = lMessageService.getAdminMessages(userId);
            if (messages != null) {
                return CResult.success("查询成功", messages);
            } else {
                return CResult.error("查询失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return CResult.error("查询管理员列表时发生异常: " + e.getMessage());
        }
    }
    
    @GetMapping("/adminUnreadCount")
    public CResult<Integer> getAdminUnreadCount(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            Integer count = lMessageService.getAdminUnreadCount(userId);
            return CResult.success("查询成功", count);
        } catch (Exception e) {
            e.printStackTrace();
            return CResult.error("查询联系管理员未读数时发生异常: " + e.getMessage());
        }
    }
}
