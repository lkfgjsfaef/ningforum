package com.app.service;

import java.util.Map;

public interface WSecondHandService {
    Map<String, Object> getItems(Integer page, Integer pageSize, Integer tagId, String sortType);
    
    Map<String, Object> getItem(Integer itemId);
    
    void incrementViewCount(Integer itemId);
    
    Map<String, Object> deletePost(Integer postId, Integer userId);
}
