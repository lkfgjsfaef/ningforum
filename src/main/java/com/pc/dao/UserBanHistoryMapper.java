package com.pc.dao;

import com.pc.pojo.UserBanHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户封禁历史DAO接口
 */
@Mapper
public interface UserBanHistoryMapper {
    /**
     * 插入封禁历史记录
     * @param banHistory 封禁历史对象
     * @return 插入的行数
     */
    int insert(UserBanHistory banHistory);

    /**
     * 查询用户的所有活跃封禁记录
     * @param userId 用户ID
     * @return 封禁记录列表
     */
    List<UserBanHistory> selectActiveBanHistoriesByUserId(@Param("userId") Integer userId);
}