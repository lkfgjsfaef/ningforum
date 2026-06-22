package com.app.dao;

import com.app.pojo.SPostVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SPostMapper {
    int insertPost(Map<String, Object> params);
    int insertPostTag(@Param("postId") Integer postId, @Param("tagId") Integer tagId);
    Integer selectTagIdByName(@Param("tagName") String tagName);
}
