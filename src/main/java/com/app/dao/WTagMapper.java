package com.app.dao;

import com.pc.pojo.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WTagMapper {
    List<Tag> selectTagsByCategory(@Param("categoryId") Integer categoryId);
}
