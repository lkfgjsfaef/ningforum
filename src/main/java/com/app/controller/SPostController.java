package com.app.controller;

import com.app.pojo.SPostVO;
import com.app.service.SPostService;
import com.pc.pojo.Tag;
import com.pc.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/android/post")
public class SPostController {

    @Autowired
    private SPostService sPostService;
    
    @Autowired
    private TagService tagService;

    @PostMapping("/publish/circle")
    public Map<String, Object> publishCircle(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Integer userId = getInteger(request, "userId");
            String title = getString(request, "title");
            String content = getString(request, "content");
            String tagName = getString(request, "tagName");
            String image1 = getString(request, "image1");
            String image2 = getString(request, "image2");
            String image3 = getString(request, "image3");
            
            if (userId == null) {
                response.put("success", false);
                response.put("message", "用户ID不能为空");
                return response;
            }
            if (title == null || title.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "标题不能为空");
                return response;
            }
            if (content == null || content.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "内容不能为空");
                return response;
            }
            
            Map<String, Object> params = new HashMap<>();
            params.put("userId", userId);
            params.put("categoryId", 1);
            params.put("title", title);
            params.put("content", content);
            params.put("tagName", tagName);
            params.put("image1", image1);
            params.put("image2", image2);
            params.put("image3", image3);
            
            Integer postId = sPostService.publishPost(params);
            
            if (postId != null) {
                response.put("success", true);
                response.put("message", "发布成功");
                response.put("postId", postId);
            } else {
                response.put("success", false);
                response.put("message", "发布失败");
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("BANNED_POST:")) {
                String message = e.getMessage().substring("BANNED_POST:".length());
                response.put("success", false);
                response.put("message", message);
                response.put("banned", true);
                return response;
            }
            e.printStackTrace();
            System.err.println("SPostController.publishCircle - 异常: " + e.getMessage());
            response.put("success", false);
            response.put("message", "发布失败：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("SPostController.publishCircle - 异常: " + e.getMessage());
            response.put("success", false);
            response.put("message", "发布失败：" + e.getMessage());
        }
        
        return response;
    }

    @PostMapping("/publish/errand")
    public Map<String, Object> publishErrand(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Integer userId = getInteger(request, "userId");
            String title = getString(request, "title");
            String description = getString(request, "description");
            String amount = getString(request, "amount");
            String remark = getString(request, "remark");
            String startPoint = getString(request, "startPoint");
            String endPoint = getString(request, "endPoint");
            String image1 = getString(request, "image1");
            String image2 = getString(request, "image2");
            String image3 = getString(request, "image3");
            
            if (userId == null) {
                response.put("success", false);
                response.put("message", "用户ID不能为空");
                return response;
            }
            if (title == null || title.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "标题不能为空");
                return response;
            }
            if (description == null || description.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "描述不能为空");
                return response;
            }
            
            Map<String, Object> params = new HashMap<>();
            params.put("userId", userId);
            params.put("categoryId", 2);
            params.put("title", title);
            params.put("content", description);
            params.put("startPoint", startPoint);
            params.put("endPoint", endPoint);
            params.put("image1", image1);
            params.put("image2", image2);
            params.put("image3", image3);
            
            if (amount != null && !amount.trim().isEmpty()) {
                try {
                    params.put("price", new BigDecimal(amount));
                } catch (Exception e) {
                }
            }
            
            if (remark != null && !remark.trim().isEmpty()) {
                params.put("contactInfo", remark);
            }
            
            Integer postId = sPostService.publishPost(params);
            
            if (postId != null) {
                response.put("success", true);
                response.put("message", "发布成功");
                response.put("postId", postId);
            } else {
                response.put("success", false);
                response.put("message", "发布失败");
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("BANNED_POST:")) {
                String message = e.getMessage().substring("BANNED_POST:".length());
                response.put("success", false);
                response.put("message", message);
                response.put("banned", true);
                return response;
            }
            e.printStackTrace();
            System.err.println("SPostController.publishErrand - 异常: " + e.getMessage());
            response.put("success", false);
            response.put("message", "发布失败：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("SPostController.publishErrand - 异常: " + e.getMessage());
            response.put("success", false);
            response.put("message", "发布失败：" + e.getMessage());
        }
        
        return response;
    }

    @PostMapping("/publish/lostfound")
    public Map<String, Object> publishLostFound(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Integer userId = getInteger(request, "userId");
            String title = getString(request, "title");
            String desc = getString(request, "desc");
            String tagName = getString(request, "tagName");
            String contact = getString(request, "contact");
            String location = getString(request, "location");
            String image1 = getString(request, "image1");
            String image2 = getString(request, "image2");
            String image3 = getString(request, "image3");
            
            if (userId == null) {
                response.put("success", false);
                response.put("message", "用户ID不能为空");
                return response;
            }
            if (title == null || title.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "标题不能为空");
                return response;
            }
            if (desc == null || desc.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "描述不能为空");
                return response;
            }
            if (tagName == null || tagName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "请选择分类标签");
                return response;
            }
            
            Map<String, Object> params = new HashMap<>();
            params.put("userId", userId);
            params.put("categoryId", 4);
            params.put("title", title);
            params.put("content", desc);
            params.put("tagName", tagName);
            params.put("contactInfo", contact);
            params.put("itemInfo", location);
            params.put("image1", image1);
            params.put("image2", image2);
            params.put("image3", image3);
            
            Integer postId = sPostService.publishPost(params);
            
            if (postId != null) {
                response.put("success", true);
                response.put("message", "发布成功");
                response.put("postId", postId);
            } else {
                response.put("success", false);
                response.put("message", "发布失败");
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("BANNED_POST:")) {
                String message = e.getMessage().substring("BANNED_POST:".length());
                response.put("success", false);
                response.put("message", message);
                response.put("banned", true);
                return response;
            }
            e.printStackTrace();
            System.err.println("SPostController.publishLostFound - 异常: " + e.getMessage());
            response.put("success", false);
            response.put("message", "发布失败：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("SPostController.publishLostFound - 异常: " + e.getMessage());
            response.put("success", false);
            response.put("message", "发布失败：" + e.getMessage());
        }
        
        return response;
    }

    @PostMapping("/publish/secondhand")
    public Map<String, Object> publishSecondHand(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Integer userId = getInteger(request, "userId");
            String title = getString(request, "title");
            String desc = getString(request, "desc");
            String price = getString(request, "price");
            String tagName = getString(request, "tagName");
            String image1 = getString(request, "image1");
            String image2 = getString(request, "image2");
            String image3 = getString(request, "image3");
            
            if (userId == null) {
                response.put("success", false);
                response.put("message", "用户ID不能为空");
                return response;
            }
            if (title == null || title.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "标题不能为空");
                return response;
            }
            if (desc == null || desc.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "描述不能为空");
                return response;
            }
            if (tagName == null || tagName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "请选择分类标签");
                return response;
            }
            
            Map<String, Object> params = new HashMap<>();
            params.put("userId", userId);
            params.put("categoryId", 3);
            params.put("title", title);
            params.put("content", desc);
            params.put("tagName", tagName);
            params.put("image1", image1);
            params.put("image2", image2);
            params.put("image3", image3);
            
            if (price != null && !price.trim().isEmpty()) {
                try {
                    params.put("price", new BigDecimal(price));
                } catch (Exception e) {
                    params.put("price", BigDecimal.ZERO);
                }
            } else {
                params.put("price", BigDecimal.ZERO);
            }
            
            Integer postId = sPostService.publishPost(params);
            
            if (postId != null) {
                response.put("success", true);
                response.put("message", "发布成功");
                response.put("postId", postId);
            } else {
                response.put("success", false);
                response.put("message", "发布失败");
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("BANNED_POST:")) {
                String message = e.getMessage().substring("BANNED_POST:".length());
                response.put("success", false);
                response.put("message", message);
                response.put("banned", true);
                return response;
            }
            e.printStackTrace();
            System.err.println("SPostController.publishSecondHand - 异常: " + e.getMessage());
            response.put("success", false);
            response.put("message", "发布失败：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("SPostController.publishSecondHand - 异常: " + e.getMessage());
            response.put("success", false);
            response.put("message", "发布失败：" + e.getMessage());
        }
        
        return response;
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }
    
    @GetMapping("/tags")
    public Map<String, Object> getTags(@RequestParam(value = "categoryId", required = false) Integer categoryId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (categoryId == null) {
                categoryId = 1;
            }
            
            List<Tag> tags = tagService.getAllTags(null, categoryId);
            
            response.put("code", 200);
            response.put("message", "获取成功");
            response.put("success", true);
            response.put("data", tags);
            response.put("total", tags != null ? tags.size() : 0);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("SPostController.getTags - 异常: " + e.getMessage());
            response.put("code", 500);
            response.put("message", "获取标签失败：" + e.getMessage());
            response.put("success", false);
            response.put("data", null);
        }
        
        return response;
    }
}
