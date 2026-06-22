package com.app.service;

import com.app.pojo.SPostVO;
import java.util.Map;

public interface SPostService {
    Integer publishPost(Map<String, Object> params);
}
