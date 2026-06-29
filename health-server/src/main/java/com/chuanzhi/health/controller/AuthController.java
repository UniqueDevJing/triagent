package com.chuanzhi.health.controller;

import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.dto.LoginRequest;
import com.chuanzhi.health.dto.LoginResponse;
import com.chuanzhi.health.service.AuthService;
import io.jsonwebtoken.Claims;
import com.chuanzhi.health.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证", description = "用户登录、Token 验证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request.getUsername(), request.getPassword()));
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<?> me(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Claims claims = jwtUtil.parseToken(token);
        return Result.ok(Map.of(
            "userId", claims.get("userId"),
            "username", claims.getSubject(),
            "role", claims.get("role")
        ));
    }
}
