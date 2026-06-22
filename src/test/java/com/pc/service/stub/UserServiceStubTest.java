package com.pc.service.stub;

import com.pc.pojo.User;
import com.pc.service.impl.UserServiceChangImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceStubTest {

    private UserServiceChangImpl userService;
    private UserMapperStub userMapperStub;

    @BeforeEach
    public void setUp() {
        userService = new UserServiceChangImpl();
        userMapperStub = new UserMapperStub();
        userService.setUserMapper(userMapperStub);
    }

    @Test
    @DisplayName("测试桩模块：用户登录成功")
    public void testLoginSuccess() {
        User stubUser = new User();
        stubUser.setUserId(1);
        stubUser.setUsername("admin");
        stubUser.setPassword("123456");
        stubUser.setStatus(0);
        stubUser.setRole(0);

        userMapperStub.setStubUser(stubUser);

        User result = userService.login("admin", "123456");

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        assertEquals(0, result.getStatus());
        assertEquals(0, result.getRole());
    }

    @Test
    @DisplayName("测试桩模块：用户不存在")
    public void testLoginUserNotFound() {
        userMapperStub.setStubUser(null);

        User result = userService.login("nonexistent", "123456");

        assertNull(result);
    }

    @Test
    @DisplayName("测试桩模块：密码错误")
    public void testLoginWrongPassword() {
        User stubUser = new User();
        stubUser.setUserId(1);
        stubUser.setUsername("admin");
        stubUser.setPassword("123456");
        stubUser.setStatus(0);
        stubUser.setRole(0);

        userMapperStub.setStubUser(stubUser);

        User result = userService.login("admin", "wrongpassword");

        assertNull(result);
    }

    @Test
    @DisplayName("测试桩模块：账号被冻结")
    public void testLoginAccountFrozen() {
        User stubUser = new User();
        stubUser.setUserId(1);
        stubUser.setUsername("admin");
        stubUser.setPassword("123456");
        stubUser.setStatus(1);
        stubUser.setRole(0);

        userMapperStub.setStubUser(stubUser);

        assertThrows(RuntimeException.class, () -> {
            userService.login("admin", "123456");
        });
    }

    @Test
    @DisplayName("测试桩模块：非管理员禁止登录")
    public void testLoginNonAdminForbidden() {
        User stubUser = new User();
        stubUser.setUserId(2);
        stubUser.setUsername("normaluser");
        stubUser.setPassword("123456");
        stubUser.setStatus(0);
        stubUser.setRole(2);

        userMapperStub.setStubUser(stubUser);

        assertThrows(RuntimeException.class, () -> {
            userService.login("normaluser", "123456");
        });
    }

    @Test
    @DisplayName("测试桩模块：根据ID获取用户")
    public void testGetUserById() {
        User stubUser = new User();
        stubUser.setUserId(1);
        stubUser.setUsername("testuser");
        stubUser.setEmail("test@example.com");

        userMapperStub.setStubUser(stubUser);

        User result = userService.getUserById(1);

        assertNotNull(result);
        assertEquals(1, result.getUserId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    @DisplayName("测试桩模块：更新用户信息成功")
    public void testUpdateUserInfoSuccess() {
        User user = new User();
        user.setUserId(1);
        user.setUsername("updateduser");

        userMapperStub.setUpdateResult(1);

        boolean result = userService.updateUserInfo(user);

        assertTrue(result);
    }

    @Test
    @DisplayName("测试桩模块：更新用户信息失败")
    public void testUpdateUserInfoFailure() {
        User user = new User();
        user.setUserId(1);
        user.setUsername("updateduser");

        userMapperStub.setUpdateResult(0);

        boolean result = userService.updateUserInfo(user);

        assertFalse(result);
    }

    @Test
    @DisplayName("测试桩模块：更新头像成功")
    public void testUpdateAvatarSuccess() {
        userMapperStub.setUpdateResult(1);

        boolean result = userService.updateUserAvatar(1, "/path/to/avatar.jpg");

        assertTrue(result);
    }

    @Test
    @DisplayName("测试桩模块：数据库异常模拟")
    public void testDatabaseException() {
        userMapperStub.setThrowException(true);

        assertThrows(RuntimeException.class, () -> {
            userService.getUserById(1);
        });
    }

    @Test
    @DisplayName("测试桩模块：根据用户名获取用户")
    public void testGetUserByUsername() {
        User stubUser = new User();
        stubUser.setUserId(1);
        stubUser.setUsername("testuser");
        stubUser.setEmail("test@example.com");

        userMapperStub.setStubUser(stubUser);

        User result = userService.getUserByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
    }
}
