package com.pc.service.stub;

import com.pc.dao.UserMapper;
import com.pc.pojo.User;

import java.util.List;

public class UserMapperStub implements UserMapper {

    private User stubUser;
    private int updateResult = 0;
    private boolean throwException = false;

    public void setStubUser(User stubUser) {
        this.stubUser = stubUser;
    }

    public void setUpdateResult(int updateResult) {
        this.updateResult = updateResult;
    }

    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }

    @Override
    public User selectUserById(Integer userId) {
        if (throwException) {
            throw new RuntimeException("数据库连接失败");
        }
        return stubUser;
    }

    @Override
    public int updateUser(User user) {
        if (throwException) {
            throw new RuntimeException("数据库更新失败");
        }
        return updateResult;
    }

    @Override
    public List<User> selectAdminList(String keyword) {
        return null;
    }

    @Override
    public int insertUser(User user) {
        return 1;
    }

    @Override
    public int deleteUserById(Integer userId) {
        return 1;
    }

    @Override
    public User selectUserByUsername(String username) {
        if (throwException) {
            throw new RuntimeException("数据库查询失败");
        }
        return stubUser;
    }
}
