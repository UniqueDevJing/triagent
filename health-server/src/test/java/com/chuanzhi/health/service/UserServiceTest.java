package com.chuanzhi.health.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chuanzhi.health.entity.User;
import com.chuanzhi.health.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        userMapper.delete(null);
        testUser = new User();
        testUser.setName("测试用户");
        testUser.setPhone("13800138000");
        testUser.setEmail("test@example.com");
        testUser.setGender(1);
        testUser.setAge(30);
        testUser.setAddress("测试地址");
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        userMapper.insert(testUser);
    }

    @Test
    void shouldReturnPagedUsers() {
        IPage<User> result = userService.pageUsers(1, 10, null);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("测试用户", result.getRecords().get(0).getName());
    }

    @Test
    void shouldFilterByKeyword() {
        IPage<User> result = userService.pageUsers(1, 10, "13800138000");
        assertEquals(1, result.getTotal());

        IPage<User> noMatch = userService.pageUsers(1, 10, "不存在的用户");
        assertEquals(0, noMatch.getTotal());
    }

    @Test
    void shouldHandleEmptyTable() {
        userMapper.delete(null);
        IPage<User> result = userService.pageUsers(1, 10, null);
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void shouldPaginateCorrectly() {
        for (int i = 0; i < 5; i++) {
            User u = new User();
            u.setName("用户" + i);
            u.setPhone("1380013800" + i);
            u.setUsername("user" + i);
            u.setPassword("pass" + i);
            userMapper.insert(u);
        }

        IPage<User> page1 = userService.pageUsers(1, 2, null);
        assertEquals(6, page1.getTotal());
        assertEquals(2, page1.getRecords().size());
        assertEquals(3, page1.getPages());
    }
}
