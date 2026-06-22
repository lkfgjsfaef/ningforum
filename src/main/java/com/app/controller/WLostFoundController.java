package com.app.controller;

import com.app.common.Result;
import com.app.service.WLostFoundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/app/lostfound")
public class WLostFoundController {

    @Autowired
    private WLostFoundService lostFoundService;

    @GetMapping("/items")
    public Result<Map<String, Object>> getItems(
            @RequestParam(value = "type", defaultValue = "lost") String type,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            Map<String, Object> result = lostFoundService.getItems(type, page, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/item/{itemId}")
    public Result<Map<String, Object>> getItemDetail(@PathVariable("itemId") Integer itemId) {
        try {
            Map<String, Object> result = lostFoundService.getItemDetail(itemId);
            if (result == null) {
                return Result.error("物品不存在");
            }
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/item/{itemId}/view")
    public Result<Void> incrementViewCount(@PathVariable("itemId") Integer itemId) {
        try {
            lostFoundService.incrementViewCount(itemId);
            return Result.success(null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("操作失败：" + e.getMessage());
        }
    }

    @PostMapping("/delete")
    public Result<Map<String, Object>> deletePost(@RequestParam("postId") Integer postId, 
                                                   @RequestParam("userId") Integer userId) {
        try {
            if (postId == null) {
                return Result.error("物品ID不能为空");
            }
            if (userId == null) {
                return Result.error("用户ID不能为空");
            }
            
            Map<String, Object> result = lostFoundService.deletePost(postId, userId);
            Boolean success = (Boolean) result.get("success");
            String message = (String) result.get("message");
            
            if (success != null && success) {
                return Result.success(message, result);
            } else {
                return Result.error(message != null ? message : "删除失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
