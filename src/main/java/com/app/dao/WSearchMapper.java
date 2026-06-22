package com.app.dao;

import com.pc.pojo.PostVO;
import com.pc.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WSearchMapper {
    List<PostVO> searchPosts(@Param("keyword") String keyword,
                             @Param("categoryId") Integer categoryId,
                             @Param("start") Integer start,
                             @Param("size") Integer size);
    int countSearchPosts(@Param("keyword") String keyword,
                        @Param("categoryId") Integer categoryId);
    List<User> searchUsers(@Param("keyword") String keyword,
                           @Param("start") Integer start,
                           @Param("size") Integer size);
    int countSearchUsers(@Param("keyword") String keyword);
}
