package com.app.controller;

import com.app.common.Result;
import com.app.service.WSecondHandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/app/secondhand")
public class WSecondHandController {

    @Autowired
    private WSecondHandService secondHandService;

    @GetMapping("/items")
    public Result<Map<String, Object>> getItems(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "tagId", required = false) Integer tagId,
            @RequestParam(value = "sortType", required = false) String sortType) {
        try {
            Map<String, Object> result = secondHandService.getItems(page, pageSize, tagId, sortType);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/item/{itemId}")
    public Result<Map<String, Object>> getItem(@PathVariable("itemId") Integer itemId) {
        try {
            Map<String, Object> result = secondHandService.getItem(itemId);
            if (result == null) {
                return Result.error("商品不存在");
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
            secondHandService.incrementViewCount(itemId);
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
                return Result.error("商品ID不能为空");
            }
            if (userId == null) {
                return Result.error("用户ID不能为空");
            }
            
            Map<String, Object> result = secondHandService.deletePost(postId, userId);
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
