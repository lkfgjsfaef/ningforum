package com.app.service.impl;

import com.app.dao.SPostMapper;
import com.app.service.SPostService;
import com.app.service.UserPermissionCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class SPostServiceImpl implements SPostService {

    @Autowired
    private SPostMapper sPostMapper;
    
    @Autowired
    private UserPermissionCheckService permissionCheckService;

    @Override
    public Integer publishPost(Map<String, Object> params) {
        try {
            System.out.println("SPostServiceImpl.publishPost - 开始发布帖子，参数: " + params);
            
            Integer userId = (Integer) params.get("userId");
            if (userId != null) {
                Map<String, Object> permissionCheck = permissionCheckService.checkPostPermission(userId);
                if (Boolean.TRUE.equals(permissionCheck.get("banned"))) {
                    String message = (String) permissionCheck.get("message");
                    throw new RuntimeException("BANNED_POST:" + message);
                }
            }
            
            int result = sPostMapper.insertPost(params);
            System.out.println("SPostServiceImpl.publishPost - 插入帖子结果: " + result);
            
            if (result > 0) {
                Object postIdObj = params.get("postId");
                Integer postId = null;
                if (postIdObj != null) {
                    if (postIdObj instanceof Integer) {
                        postId = (Integer) postIdObj;
                    } else if (postIdObj instanceof java.math.BigInteger) {
                        postId = ((java.math.BigInteger) postIdObj).intValue();
                    } else if (postIdObj instanceof Number) {
                        postId = ((Number) postIdObj).intValue();
                    }
                }
                
                System.out.println("SPostServiceImpl.publishPost - 获取到的postId: " + postId);
                
                if (postId != null) {
                    String tagName = (String) params.get("tagName");
                    if (tagName != null && !tagName.trim().isEmpty()) {
                        System.out.println("SPostServiceImpl.publishPost - 处理标签: " + tagName);
                        Integer tagId = sPostMapper.selectTagIdByName(tagName);
                        System.out.println("SPostServiceImpl.publishPost - 查询到的tagId: " + tagId);
                        if (tagId != null) {
                            int tagResult = sPostMapper.insertPostTag(postId, tagId);
                            System.out.println("SPostServiceImpl.publishPost - 插入标签关联结果: " + tagResult);
                        } else {
                            System.out.println("SPostServiceImpl.publishPost - 警告: 标签不存在: " + tagName);
                        }
                    }
                    
                    System.out.println("SPostServiceImpl.publishPost - 发布成功，postId: " + postId);
                    return postId;
                } else {
                    System.err.println("SPostServiceImpl.publishPost - 错误: 无法获取postId");
                }
            } else {
                System.err.println("SPostServiceImpl.publishPost - 错误: 插入帖子失败，result=" + result);
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("BANNED_POST:")) {
                throw e;
            }
            System.err.println("SPostServiceImpl.publishPost - 异常: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("SPostServiceImpl.publishPost - 异常: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
