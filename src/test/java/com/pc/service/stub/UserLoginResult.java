package com.pc.service.impl;

import com.pc.pojo.User;

public class UserLoginResult {
    private boolean success;
    private String message;
    private User user;

    public UserLoginResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public UserLoginResult(boolean success, String message, User user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
