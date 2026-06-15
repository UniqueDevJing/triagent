package com.chuanzhi.health.service;

import com.chuanzhi.health.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(String username, String password);
}
