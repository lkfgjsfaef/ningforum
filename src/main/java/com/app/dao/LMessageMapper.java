package com.app.dao;

import com.pc.pojo.Message;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

public interface LMessageMapper {
    List<Map<String, Object>> selectMutualFollowMessages(@Param("currentUserId") Integer currentUserId);
    List<Map<String, Object>> selectFanMessages(@Param("currentUserId") Integer currentUserId);
    List<Message> selectMessagesBetweenUsers(@Param("currentUserId") Integer currentUserId, 
                                             @Param("otherUserId") Integer otherUserId);
    int insertPrivateMessage(@Param("senderId") Integer senderId,
                            @Param("receiverId") Integer receiverId,
                            @Param("content") String content,
                            @Param("msgFormat") Integer msgFormat,
                            @Param("imageUrl") String imageUrl);
    int updateMessagesReadBetweenUsers(@Param("currentUserId") Integer currentUserId,
                                      @Param("otherUserId") Integer otherUserId);
    Integer countTotalUnreadMessages(@Param("currentUserId") Integer currentUserId);
    Integer countMutualFollowUnreadMessages(@Param("currentUserId") Integer currentUserId);
    Integer countFanUnreadMessages(@Param("currentUserId") Integer currentUserId);
    List<Map<String, Object>> selectAdminMessages(@Param("currentUserId") Integer currentUserId);
    Integer countAdminUnreadMessages(@Param("currentUserId") Integer currentUserId);
}
