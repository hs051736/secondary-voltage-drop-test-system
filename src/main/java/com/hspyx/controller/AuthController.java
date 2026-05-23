package com.hspyx.controller;

import com.hspyx.dto.AuthResponse;
import com.hspyx.dto.LoginRequest;
import com.hspyx.dto.RegisterRequest;
import com.hspyx.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户登录注册接口")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    /**
     * 用户登录
     * POST /api/auth/login
     * @return
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回JWT Token")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        if (response.getToken() != null) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).body(response);
    }


    /**
     * 用户注册
     * @param request
     * POST /api/auth/register
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户，自动登录并返回JWT Token")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        if (response.getToken() != null) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(400).body(response);
    }
}
