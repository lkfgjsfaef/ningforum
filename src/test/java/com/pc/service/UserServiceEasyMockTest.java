package com.pc.service;

import com.app.mq.CacheInvalidationMessage;
import com.app.mq.producer.MessageProducer;
import com.pc.dao.UserDao;
import com.pc.pojo.User;
import com.pc.service.impl.UserServiceImpl;
import com.pc.utils.RedisCacheUtil;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;

public class UserServiceEasyMockTest {

    private UserDao mockUserDao;
    private RedisCacheUtilStub redisCacheUtilStub;
    private MessageProducerStub messageProducerStub;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        mockUserDao = createMock(UserDao.class);
        redisCacheUtilStub = new RedisCacheUtilStub();
        messageProducerStub = new MessageProducerStub();
        
        userService = new UserServiceImpl();
        userService.setUserDao(mockUserDao);
        userService.setRedisCacheUtil(redisCacheUtilStub);
        userService.setMessageProducer(messageProducerStub);
    }

    @Test
    @DisplayName("EasyMock测试：根据ID查询用户 - 正常返回")
    void testFindUserById_Success() {
        User expectedUser = new User();
        expectedUser.setUserId(1);
        expectedUser.setUsername("testuser");
        expectedUser.setEmail("test@example.com");

        expect(mockUserDao.findUserById(1)).andReturn(expectedUser);
        replay(mockUserDao);

        User result = userService.findUserById(1);

        verify(mockUserDao);
        assertNotNull(result);
        assertEquals(1, result.getUserId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    @DisplayName("EasyMock测试：根据ID查询用户 - 返回null")
    void testFindUserById_ReturnsNull() {
        expect(mockUserDao.findUserById(999)).andReturn(null);
        replay(mockUserDao);

        User result = userService.findUserById(999);

        verify(mockUserDao);
        assertNull(result);
    }

    @Test
    @DisplayName("EasyMock测试：更新用户 - 更新成功")
    void testUpdateUser_Success() {
        User user = new User();
        user.setUserId(1);
        user.setUsername("updated");

        expect(mockUserDao.updateUser(user)).andReturn(1);
        replay(mockUserDao);

        int result = userService.updateUser(user);

        verify(mockUserDao);
        assertEquals(1, result);
    }

    @Test
    @DisplayName("EasyMock测试：更新用户 - 更新失败")
    void testUpdateUser_Failure() {
        User user = new User();
        user.setUserId(1);

        expect(mockUserDao.updateUser(user)).andReturn(0);
        replay(mockUserDao);

        int result = userService.updateUser(user);

        verify(mockUserDao);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("EasyMock测试：根据用户名查询用户")
    void testFindUserByUsername_Success() {
        User expectedUser = new User();
        expectedUser.setUserId(1);
        expectedUser.setUsername("testuser");

        expect(mockUserDao.findUserByUsername("testuser")).andReturn(expectedUser);
        replay(mockUserDao);

        User result = userService.findUserByUsername("testuser");

        verify(mockUserDao);
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    @DisplayName("EasyMock测试：模拟数据库异常")
    void testFindUserById_Exception() {
        expect(mockUserDao.findUserById(1)).andThrow(new RuntimeException("数据库连接失败"));
        replay(mockUserDao);

        RuntimeException caught = assertThrows(RuntimeException.class, () -> userService.findUserById(1));
        assertEquals("数据库连接失败", caught.getMessage());

        verify(mockUserDao);
    }

    public static class RedisCacheUtilStub extends RedisCacheUtil {
        @Override
        public <T> T getWithLock(String key, Class<T> clazz, CacheLoader<T> loader, long timeout, TimeUnit unit) {
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
        public void sendCacheInvalidation(CacheInvalidationMessage message) {
        }
    }
}
