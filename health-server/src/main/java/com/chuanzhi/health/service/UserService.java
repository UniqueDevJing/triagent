package com.chuanzhi.health.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chuanzhi.health.entity.User;

public interface UserService extends IService<User> {
    IPage<User> pageUsers(int page, int size, String keyword);
}
