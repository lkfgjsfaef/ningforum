package com.app.controller;

import com.app.common.Result;
import com.app.service.WCircleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/app/circle")
public class WCircleController {

    @Autowired
    private WCircleService circleService;

    @GetMapping("/posts")
    public Result<Map<String, Object>> getPosts(
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "tagId", required = false) Integer tagId,
            @RequestParam(value = "sortType", defaultValue = "newest") String sortType,
            @RequestParam(value = "userId", required = false) Integer userId) {
        try {
            Map<String, Object> result = circleService.getPosts(categoryId, page, pageSize, tagId, sortType, userId);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/post/{postId}")
    public Result<Map<String, Object>> getPostDetail(
            @PathVariable("postId") Integer postId,
            @RequestParam(value = "userId", required = false) Integer userId) {
        try {
            Map<String, Object> result = circleService.getPostDetail(postId, userId);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/post/{postId}/comments")
    public Result<Map<String, Object>> getPostComments(@PathVariable("postId") Integer postId) {
        try {
            Map<String, Object> result = circleService.getPostComments(postId);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/hot")
    public Result<Map<String, Object>> getHotPosts(
            @RequestParam(value = "limit", defaultValue = "1") Integer limit) {
        try {
            Map<String, Object> result = circleService.getHotPosts(limit);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/post/{postId}/view")
    public Result<Void> incrementViewCount(@PathVariable("postId") Integer postId) {
        try {
            circleService.incrementViewCount(postId);
            return Result.success(null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("操作失败：" + e.getMessage());
        }
    }

    @PostMapping("/post/{postId}/comment")
    public Result<Map<String, Object>> addComment(
            @PathVariable("postId") Integer postId,
            @RequestParam("userId") Integer userId,
            @RequestParam("content") String content,
            @RequestParam(value = "parentId", required = false) Integer parentId) {
        try {
            Map<String, Object> result = circleService.addComment(postId, userId, content, parentId);
            if (result.get("banned") != null && Boolean.TRUE.equals(result.get("banned"))) {
                return Result.error(result.get("error") != null ? (String) result.get("error") : "你已被禁止评论");
            }
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("发布评论失败：" + e.getMessage());
        }
    }

    @PostMapping("/post/{postId}/like")
    public Result<Map<String, Object>> toggleLike(
            @PathVariable("postId") Integer postId,
            @RequestParam("userId") Integer userId) {
        try {
            Map<String, Object> result = circleService.toggleLike(postId, userId);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("操作失败：" + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> result = circleService.getStatistics();
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/post/{postId}/favorite")
    public Result<Map<String, Object>> addFavorite(
            @PathVariable("postId") Integer postId,
            @RequestParam("userId") Integer userId) {
        try {
            Map<String, Object> result = circleService.addFavorite(postId, userId);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("收藏失败：" + e.getMessage());
        }
    }

    @GetMapping("/favorites")
    public Result<Map<String, Object>> getFavoritePosts(
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            Map<String, Object> result = circleService.getFavoritePosts(userId, page, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }
    
    @PostMapping("/comment/{commentId}/delete")
    public Result<Map<String, Object>> deleteComment(
            @PathVariable("commentId") Integer commentId,
            @RequestParam("userId") Integer userId) {
        try {
            Map<String, Object> result = circleService.deleteComment(commentId, userId);
            if (result.get("success") != null && (Boolean) result.get("success")) {
                return Result.success(result);
            } else {
                return Result.error(result.get("message") != null ? (String) result.get("message") : "删除失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败：" + e.getMessage());
        }
    }
}
