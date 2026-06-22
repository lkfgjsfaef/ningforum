package com.app.service;

import java.util.List;
import java.util.Map;

public interface WTagService {
    List<Map<String, Object>> getTagsByCategory(Integer categoryId);
}
