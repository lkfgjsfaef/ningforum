package com.app.controller;

import com.app.common.Result;
import com.app.service.WNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app/notification")
public class WNotificationController {

    @Autowired
    private WNotificationService notificationService;

    @GetMapping("/latest")
    public Result<Map<String, Object>> getLatestNotification(
            @RequestParam(value = "userId", required = false) Integer userId) {
        try {
            System.out.println("========== 收到获取最新通知请求 ==========");
            System.out.println("userId: " + userId);
            Map<String, Object> result;
            if (userId != null) {
                System.out.println("查询用户专属通知");
                result = notificationService.getLatestUserNotification(userId);
            } else {
                System.out.println("查询全局最新通知");
                result = notificationService.getLatestNotification();
            }
            System.out.println("查询结果: " + (result != null ? "有通知" : "无通知"));
            if (result == null) {
                System.out.println("返回null（没有通知）");
                return Result.success(null);
            }
            System.out.println("返回通知数据: " + result);
            return Result.success(result);
        } catch (Exception e) {
            System.err.println("查询通知失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/{messageId}")
    public Result<Map<String, Object>> getNotificationById(@PathVariable("messageId") Integer messageId) {
        try {
            Map<String, Object> result = notificationService.getNotificationById(messageId);
            if (result == null) {
                return Result.error("通知不存在");
            }
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> getNotificationList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            Map<String, Object> result = notificationService.getNotificationList(page, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/unread")
    public Result<List<Map<String, Object>>> getUnreadUserNotifications(
            @RequestParam("userId") Integer userId) {
        try {
            List<Map<String, Object>> result = notificationService.getUnreadUserNotifications(userId);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }
}
