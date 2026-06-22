package com.pc.service;

import com.pc.pojo.User;

public interface UserServiceChang {

    User getUserById(Integer userId);

    User getUserByUsername(String username);

    boolean updateUserInfo(User user);

    boolean updateUserAvatar(Integer userId, String avatarPath);

    User login(String username, String password);
}
