package com.app.service;

import java.util.Map;

public interface WLostFoundService {
    Map<String, Object> getItems(String type, Integer page, Integer pageSize);
    
    Map<String, Object> getItemDetail(Integer itemId);
    
    void incrementViewCount(Integer itemId);
    
    Map<String, Object> deletePost(Integer postId, Integer userId);
}
