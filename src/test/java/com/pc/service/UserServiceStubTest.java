package com.pc.service;

import com.app.mq.producer.MessageProducer;
import com.pc.dao.UserDao;
import com.pc.pojo.User;
import com.pc.service.impl.UserServiceImpl;
import com.pc.utils.RedisCacheUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceStubTest {

    private UserServiceImpl userService;
    private UserDaoStub userDaoStub;
    private RedisCacheUtil redisCacheUtil;
    private MessageProducer messageProducer;

    @BeforeEach
    void setUp() {
        userDaoStub = new UserDaoStub();
        redisCacheUtil = new RedisCacheUtilStub();
        messageProducer = new MessageProducerStub();
        
        userService = new UserServiceImpl();
        userService.setUserDao(userDaoStub);
        userService.setRedisCacheUtil(redisCacheUtil);
        userService.setMessageProducer(messageProducer);
    }

    @Test
    @DisplayName("测试用例1：根据ID查询用户 - 正常返回用户对象")
    void testFindUserById_Success() {
        User expectedUser = new User();
        expectedUser.setUserId(1);
        expectedUser.setUsername("testuser");
        expectedUser.setEmail("test@example.com");

        userDaoStub.setStubUser(expectedUser);

        User result = userService.findUserById(1);

        assertNotNull(result);
        assertEquals(1, result.getUserId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    @DisplayName("测试用例2：根据ID查询用户 - 返回null")
    void testFindUserById_ReturnsNull() {
        userDaoStub.setStubUser(null);

        User result = userService.findUserById(999);

        assertNull(result);
    }

    @Test
    @DisplayName("测试用例3：根据ID查询用户 - 参数为null")
    void testFindUserById_NullId() {
        User result = userService.findUserById(null);

        assertNull(result);
    }

    @Test
    @DisplayName("测试用例4：更新用户信息 - 更新成功")
    void testUpdateUser_Success() {
        userDaoStub.setUpdateResult(1);

        User user = new User();
        user.setUserId(1);
        user.setUsername("updateduser");

        int result = userService.updateUser(user);

        assertEquals(1, result);
    }

    @Test
    @DisplayName("测试用例5：更新用户信息 - 更新失败")
    void testUpdateUser_Failure() {
        userDaoStub.setUpdateResult(0);

        User user = new User();
        user.setUserId(1);

        int result = userService.updateUser(user);

        assertEquals(0, result);
    }

    @Test
    @DisplayName("测试用例6：根据用户名查询用户 - 正常返回")
    void testFindUserByUsername_Success() {
        User expectedUser = new User();
        expectedUser.setUserId(1);
        expectedUser.setUsername("testuser");

        userDaoStub.setStubUser(expectedUser);

        User result = userService.findUserByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    @DisplayName("测试用例7：根据用户名查询用户 - 用户名为空")
    void testFindUserByUsername_EmptyUsername() {
        User result = userService.findUserByUsername("");

        assertNull(result);
    }

    @Test
    @DisplayName("测试用例8：根据用户名查询用户 - 用户不存在")
    void testFindUserByUsername_UserNotFound() {
        userDaoStub.setStubUser(null);

        User result = userService.findUserByUsername("nonexistent");

        assertNull(result);
    }

    @Test
    @DisplayName("测试用例9：数据库异常模拟")
    void testFindUserById_DatabaseException() {
        userDaoStub.setThrowException(true);

        assertThrows(RuntimeException.class, () -> userService.findUserById(1));
    }

    public static class UserDaoStub implements UserDao {
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
        public User findUserById(Integer userId) {
            if (throwException) {
                throw new RuntimeException("数据库连接失败");
            }
            return stubUser;
        }

        @Override
        public User findUserByUsername(String username) {
            if (throwException) {
                throw new RuntimeException("数据库查询失败");
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
        public List<User> findAllUsers() { return null; }
        @Override
        public int insertUser(User user) { return 0; }
        @Override
        public int deleteUser(Integer userId) { return 0; }
        @Override
        public List<User> findUsersByStatus(Integer status) { return null; }
        @Override
        public int updateUserStatus(Integer userId, Integer status) { return 0; }
        @Override
        public int resetPassword(Integer userId, String password) { return 0; }
        @Override
        public com.pc.pojo.UserPermission findUserPermissionByUserId(Integer userId) { return null; }
        @Override
        public int updateUserPostPermission(Integer userId, Integer canPost) { return 0; }
        @Override
        public int updateUserCommentPermission(Integer userId, Integer canComment) { return 0; }
        @Override
        public int updateUserMessagePermission(Integer userId, Integer canMessage) { return 0; }
        @Override
        public int insertBanHistory(com.pc.pojo.UserBanHistory banHistory) { return 0; }
        @Override
        public List<com.pc.pojo.UserBanHistory> findBanHistoryByUserId(Integer userId) { return null; }
        @Override
        public int deactivateActiveBanHistory(Integer userId, String permissionType) { return 0; }
    }

    public static class RedisCacheUtilStub extends RedisCacheUtil {
        @Override
        public <T> T getWithLock(String key, Class<T> clazz, CacheLoader<T> loader, long timeout, java.util.concurrent.TimeUnit unit) {
            return loader.load();
        }

        @Override
        public boolean delete(String key) {
            return true;
        }

        @Override
        public void clearByPattern(String pattern) {
        }
    }

    public static class MessageProducerStub extends MessageProducer {
        @Override
        public void sendCacheInvalidation(com.app.mq.CacheInvalidationMessage message) {
        }
    }
}
