package com.app.controller;

import com.app.common.Result;
import com.app.service.WSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/app/search")
public class WSearchController {

    @Autowired
    private WSearchService searchService;

    @GetMapping("/posts")
    public Result<Map<String, Object>> searchPosts(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            Map<String, Object> result = searchService.searchPosts(keyword, categoryId, page, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

    @GetMapping("/users")
    public Result<Map<String, Object>> searchUsers(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        try {
            Map<String, Object> result = searchService.searchUsers(keyword, page, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("搜索失败：" + e.getMessage());
        }
    }
}
