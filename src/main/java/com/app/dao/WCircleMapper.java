package com.app.dao;

import com.pc.pojo.PostVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface WCircleMapper {
    List<PostVO> selectPostsByCondition(Map<String, Object> params);
    int countPostsByCondition(Map<String, Object> params);
    PostVO selectPostById(@Param("postId") Integer postId);
    List<String> selectTagNamesByPostId(@Param("postId") Integer postId);
    List<Map<String, Object>> selectCommentsByPostId(@Param("postId") Integer postId);
    List<PostVO> selectHotPosts(@Param("limit") Integer limit);
    void incrementViewCount(@Param("postId") Integer postId);
    int insertComment(@Param("postId") Integer postId, 
                      @Param("userId") Integer userId, 
                      @Param("content") String content, 
                      @Param("parentId") Integer parentId);
    int getLastInsertCommentId();
    Integer getCommentIdByPostAndUser(@Param("postId") Integer postId, @Param("userId") Integer userId);
    void incrementCommentCount(@Param("postId") Integer postId);
    Integer checkLikeExists(@Param("postId") Integer postId, @Param("userId") Integer userId);
    Integer checkLikeRecordExists(@Param("postId") Integer postId, @Param("userId") Integer userId);
    void insertLike(@Param("postId") Integer postId, @Param("userId") Integer userId);
    void restoreLike(@Param("postId") Integer postId, @Param("userId") Integer userId);
    void deleteLike(@Param("postId") Integer postId, @Param("userId") Integer userId);
    void updateLikeCount(@Param("postId") Integer postId, @Param("increment") int increment);
    int countUsers();
    void insertFavorite(@Param("postId") Integer postId, @Param("userId") Integer userId);
    Integer checkFavoriteExists(@Param("postId") Integer postId, @Param("userId") Integer userId);
    void updateFavoriteCount(@Param("postId") Integer postId, @Param("increment") int increment);
    Integer checkFavoriteRecordExists(@Param("postId") Integer postId, @Param("userId") Integer userId);
    void deleteFavorite(@Param("postId") Integer postId, @Param("userId") Integer userId);
    void restoreFavorite(@Param("postId") Integer postId, @Param("userId") Integer userId);
    List<PostVO> selectFavoritePostsByUserId(@Param("userId") Integer userId, @Param("start") Integer start, @Param("size") Integer size);
    int countFavoritePostsByUserId(@Param("userId") Integer userId);
    Integer getCommentUserId(@Param("commentId") Integer commentId);
    Integer getPostIdByCommentId(@Param("commentId") Integer commentId);
    void deleteComment(@Param("commentId") Integer commentId);
    void decrementCommentCount(@Param("postId") Integer postId);
    List<Map<String, Object>> selectCommentsByUserId(@Param("userId") Integer userId, 
                                                      @Param("start") Integer start, 
                                                      @Param("size") Integer size);
    int deletePost(@Param("postId") Integer postId, @Param("userId") Integer userId);
}
