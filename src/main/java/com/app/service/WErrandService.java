package com.app.service;

import java.util.Map;

public interface WErrandService {
    Map<String, Object> getOrders(String status, Integer page, Integer pageSize);
    
    boolean isErrandRunner(Integer userId);
    
    Map<String, Object> getOrderDetail(Integer orderId);
    
    boolean acceptOrder(Integer postId, Integer acceptorId);
    
    Integer getSuperAdminId();
    
    boolean cancelPublish(Integer postId, Integer userId);
    
    boolean completeOrder(Integer postId, Integer acceptorId);
}
