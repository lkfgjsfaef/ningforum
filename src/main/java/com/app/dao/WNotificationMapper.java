package com.app.dao;

import com.pc.pojo.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WNotificationMapper {
    Message selectLatestSystemNotification();
    Message selectLatestUserSystemNotification(@Param("userId") Integer userId);
    Message selectNotificationById(@Param("messageId") Integer messageId);
    List<Message> selectSystemNotifications(@Param("start") Integer start, @Param("size") Integer size);
    int countSystemNotifications();
    List<Message> selectUnreadUserNotifications(@Param("userId") Integer userId);
    List<Message> selectUnreadDirectUserNotifications(@Param("userId") Integer userId);
    int insertNotification(Message message);
}
