package com.app.controller;

import com.app.common.Result;
import com.app.service.WTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app/tag")
public class WTagController {

    @Autowired
    private WTagService tagService;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getTags(
            @RequestParam(value = "categoryId", required = false) Integer categoryId) {
        try {
            List<Map<String, Object>> tags = tagService.getTagsByCategory(categoryId);
            return Result.success(tags);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }
}
