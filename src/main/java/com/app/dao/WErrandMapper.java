package com.app.dao;

import com.pc.pojo.PostVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WErrandMapper {
    List<PostVO> selectErrandOrders(@Param("statusList") List<Integer> statusList,
                                    @Param("start") Integer start,
                                    @Param("size") Integer size);
    int countErrandOrders(@Param("statusList") List<Integer> statusList);
    PostVO selectErrandOrderById(@Param("postId") Integer postId);
    int updatePostStatusToInProgress(@Param("postId") Integer postId);
    int insertTradeTask(@Param("taskNo") String taskNo,
                       @Param("postId") Integer postId,
                       @Param("creatorId") Integer creatorId,
                       @Param("acceptorId") Integer acceptorId,
                       @Param("title") String title,
                       @Param("description") String description,
                       @Param("amount") java.math.BigDecimal amount,
                       @Param("startLocation") String startLocation,
                       @Param("endLocation") String endLocation,
                       @Param("contactInfo") String contactInfo,
                       @Param("image1") String image1,
                       @Param("image2") String image2,
                       @Param("image3") String image3);
    java.util.Map<String, Object> selectTradeTaskByPostId(@Param("postId") Integer postId);
    List<java.util.Map<String, Object>> selectTradeTasksByAcceptorId(@Param("acceptorId") Integer acceptorId);
    int updatePostStatusToDeleted(@Param("postId") Integer postId, @Param("userId") Integer userId);
    int updateTradeTaskStatusToCompleted(@Param("postId") Integer postId, @Param("acceptorId") Integer acceptorId);
    int updatePostStatusToCompleted(@Param("postId") Integer postId);
    List<PostVO> selectUserErrandPosts(@Param("userId") Integer userId, @Param("categoryId") Integer categoryId);
}
