package com.app.dao;

import com.app.pojo.CUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CUserMapper {
    CUser findByUsername(@Param("username") String username);
    CUser findByPhone(@Param("phone") String phone);
    CUser findById(@Param("userId") Integer userId);
    int insertCUser(CUser user);
    int updateCUser(CUser user);
    List<CUser> selectRelationList(@Param("userId") Integer userId, @Param("type") Integer type);
    int insertFollow(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);
    int deleteFollow(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);
    Integer checkFollowExists(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);
    Integer countFollowing(@Param("userId") Integer userId);
    Integer countFans(@Param("userId") Integer userId);
    Integer countLikes(@Param("userId") Integer userId);
    int blockUser(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);
    int unblockUser(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);
    List<CUser> selectBlacklist(@Param("userId") Integer userId);
    Integer checkBlockExists(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);
}
