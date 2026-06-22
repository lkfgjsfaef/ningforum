package com.app.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WReportMapper {
    int insertReport(@Param("reporterId") Integer reporterId,
                     @Param("targetType") String targetType,
                     @Param("targetId") Integer targetId,
                     @Param("postId") Integer postId,
                     @Param("interaction") Integer interaction,
                     @Param("reportType") String reportType,
                     @Param("description") String description,
                     @Param("reportImage") String reportImage,
                     @Param("status") Integer status);
    Integer checkReportExists(@Param("reporterId") Integer reporterId,
                             @Param("targetType") String targetType,
                             @Param("targetId") Integer targetId);
    Integer checkReportExistsByTargetId(@Param("reporterId") Integer reporterId,
                                        @Param("targetType") String targetType,
                                        @Param("targetId") Integer targetId);
    Integer checkReportExistsByPostId(@Param("reporterId") Integer reporterId,
                                     @Param("targetType") String targetType,
                                     @Param("postId") Integer postId);
    Integer checkReportExistsByInteraction(@Param("reporterId") Integer reporterId,
                                          @Param("targetType") String targetType,
                                          @Param("interaction") Integer interaction);
}
