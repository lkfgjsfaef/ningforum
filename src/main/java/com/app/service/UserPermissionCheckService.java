package com.app.service;

import java.util.Map;

public interface UserPermissionCheckService {
    Map<String, Object> checkLoginPermission(Integer userId);
    
    Map<String, Object> checkPostPermission(Integer userId);
    
    Map<String, Object> checkCommentPermission(Integer userId);
    
    Map<String, Object> checkMessagePermission(Integer userId);
}
