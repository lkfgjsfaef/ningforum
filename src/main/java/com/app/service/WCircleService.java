package com.app.service;

import java.util.List;
import java.util.Map;

public interface WCircleService {
    Map<String, Object> getPosts(Integer categoryId, Integer page, Integer pageSize, Integer tagId, String sortType);

    Map<String, Object> getPosts(Integer categoryId, Integer page, Integer pageSize, Integer tagId, String sortType, Integer userId);

    Map<String, Object> getPostDetail(Integer postId, Integer userId);

    Map<String, Object> getPostComments(Integer postId);

    Map<String, Object> getHotPosts(Integer limit);

    void incrementViewCount(Integer postId);

    Map<String, Object> addComment(Integer postId, Integer userId, String content, Integer parentId);

    Map<String, Object> toggleLike(Integer postId, Integer userId);

    Map<String, Object> getStatistics();

    Map<String, Object> addFavorite(Integer postId, Integer userId);

    Map<String, Object> getFavoritePosts(Integer userId, Integer page, Integer pageSize);
    
    Map<String, Object> deleteComment(Integer commentId, Integer userId);
    
    List<Map<String, Object>> getUserComments(Integer userId);
    
    List<Map<String, Object>> getUserOrders(Integer userId);
    
    List<Map<String, Object>> getUserPosts(Integer userId, Integer categoryId);
    
    Map<String, Object> deletePost(Integer postId, Integer userId);
}
