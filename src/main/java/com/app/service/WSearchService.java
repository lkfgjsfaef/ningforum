package com.app.service;

import java.util.Map;

public interface WSearchService {
    Map<String, Object> searchPosts(String keyword, Integer categoryId, Integer page, Integer pageSize);

    Map<String, Object> searchUsers(String keyword, Integer page, Integer pageSize);
}
