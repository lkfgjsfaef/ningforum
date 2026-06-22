package com.pc.service;

import com.app.mq.CacheInvalidationMessage;
import com.app.mq.producer.MessageProducer;
import com.pc.dao.UserDao;
import com.pc.pojo.User;
import com.pc.service.impl.UserServiceImpl;
import com.pc.utils.RedisCacheUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceMockitoTest {

    @Mock
    private UserDao mockUserDao;

    private RedisCacheUtilStub redisCacheUtilStub;
    private MessageProducerStub messageProducerStub;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        redisCacheUtilStub = new RedisCacheUtilStub();
        messageProducerStub = new MessageProducerStub();
        
        userService = new UserServiceImpl();
        userService.setUserDao(mockUserDao);
        userService.setRedisCacheUtil(redisCacheUtilStub);
        userService.setMessageProducer(messageProducerStub);
    }

    @Test
    @DisplayName("Mockito测试：根据ID查询用户 - 正常返回")
    void testFindUserById_Success() {
        User expectedUser = new User();
        expectedUser.setUserId(1);
        expectedUser.setUsername("testuser");
        expectedUser.setEmail("test@example.com");

        when(mockUserDao.findUserById(1)).thenReturn(expectedUser);

        User result = userService.findUserById(1);

        verify(mockUserDao, times(1)).findUserById(1);
        assertNotNull(result);
        assertEquals(1, result.getUserId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    @DisplayName("Mockito测试：根据ID查询用户 - 返回null")
    void testFindUserById_ReturnsNull() {
        when(mockUserDao.findUserById(999)).thenReturn(null);

        User result = userService.findUserById(999);

        verify(mockUserDao).findUserById(999);
        assertNull(result);
    }

    @Test
    @DisplayName("Mockito测试：更新用户 - 更新成功")
    void testUpdateUser_Success() {
        User user = new User();
        user.setUserId(1);
        user.setUsername("updated");

        when(mockUserDao.updateUser(user)).thenReturn(1);

        int result = userService.updateUser(user);

        verify(mockUserDao).updateUser(user);
        assertEquals(1, result);
    }

    @Test
    @DisplayName("Mockito测试：更新用户 - 更新失败")
    void testUpdateUser_Failure() {
        User user = new User();
        user.setUserId(1);

        when(mockUserDao.updateUser(user)).thenReturn(0);

        int result = userService.updateUser(user);

        verify(mockUserDao).updateUser(user);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("Mockito测试：根据用户名查询用户")
    void testFindUserByUsername_Success() {
        User expectedUser = new User();
        expectedUser.setUserId(1);
        expectedUser.setUsername("testuser");

        when(mockUserDao.findUserByUsername("testuser")).thenReturn(expectedUser);

        User result = userService.findUserByUsername("testuser");

        verify(mockUserDao).findUserByUsername("testuser");
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    @DisplayName("Mockito测试：模拟数据库异常")
    void testFindUserById_Exception() {
        when(mockUserDao.findUserById(1)).thenThrow(new RuntimeException("数据库连接失败"));

        RuntimeException caught = assertThrows(RuntimeException.class, () -> userService.findUserById(1));
        assertEquals("数据库连接失败", caught.getMessage());

        verify(mockUserDao).findUserById(1);
    }

    @Test
    @DisplayName("Mockito测试：添加用户 - 成功")
    void testAddUser_Success() {
        User user = new User();
        user.setUsername("newuser");

        when(mockUserDao.insertUser(user)).thenReturn(1);

        int result = userService.addUser(user);

        verify(mockUserDao).insertUser(user);
        assertEquals(1, result);
    }

    @Test
    @DisplayName("Mockito测试：删除用户 - 成功")
    void testDeleteUser_Success() {
        when(mockUserDao.deleteUser(1)).thenReturn(1);

        int result = userService.deleteUser(1);

        verify(mockUserDao).deleteUser(1);
        assertEquals(1, result);
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
