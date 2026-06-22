package com.app.dao;

import com.pc.pojo.PostVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface WLostFoundMapper {
    List<PostVO> selectLostFoundPosts(Map<String, Object> params);
    int countLostFoundPosts(Map<String, Object> params);
    PostVO selectLostFoundPostById(@Param("postId") Integer postId);
    void incrementViewCount(@Param("postId") Integer postId);
    int deletePost(@Param("postId") Integer postId, @Param("userId") Integer userId);
}
