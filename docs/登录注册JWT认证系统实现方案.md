# 登录注册和JWT认证系统实现方案

## 目录

- [1. 需求概述](#1-需求概述)
- [2. 技术架构](#2-技术架构)
- [3. 核心组件实现](#3-核心组件实现)
- [4. 前端实现](#4-前端实现)
- [5. 数据库设计](#5-数据库设计)
- [6. 安全机制详解](#6-安全机制详解)
- [7. 配置文件说明](#7-配置文件说明)

***

## 1. 需求概述

### 1.1 功能需求

- 支持账号密码登录
- 支持用户注册
- 前后端分离架构
- 无状态的Token鉴权机制
- JWT (JSON Web Token) 实现身份认证
- Swagger API 文档展示

### 1.2 技术栈

| 组件      | 技术方案                        |
| ------- | --------------------------- |
| 后端框架    | Spring Boot 3.4.12          |
| 安全框架    | Spring Security             |
| Token方案 | JWT (jjwt 0.12.6)           |
| 密码加密    | BCrypt                      |
| 数据库     | MySQL                       |
| ORM     | MyBatis                     |
| API文档   | SpringDoc OpenAPI (Swagger) |

***

## 2. 技术架构

### 2.1 系统架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                              前端 (HTML/JavaScript)                      │
│                                                                          │
│   ┌──────────────┐         ┌──────────────┐         ┌──────────────┐  │
│   │  login.html  │────────▶│  index.html │────────▶│ API请求     │  │
│   │   登录页面   │         │   主页面     │         │ 携带Token   │  │
│   └──────────────┘         └──────────────┘         └──────┬───────┘  │
│          │                        │                        │            │
│          ▼                        ▼                        ▼            │
│   ┌─────────────────────────────────────────────────────────────┐      │
│   │              localStorage 存储 Token                         │      │
│   │              - token: JWT令牌                               │      │
│   │              - username: 用户名                              │      │
│   │              - nickname: 昵称                              │      │
│   │              - role: 角色                                   │      │
│   └─────────────────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────────────┘
                                    │ HTTP + Bearer Token
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          Spring Boot Backend                             │
│                                                                          │
│   ┌─────────────────────────────────────────────────────────────────┐    │
│   │                    Authentication Flow                          │    │
│   │                                                                   │    │
│   │   ┌────────────────┐    ┌────────────────┐    ┌──────────────┐  │    │
│   │   │ AuthController │───▶│  AuthService  │───▶│ UserMapper  │  │    │
│   │   │ /api/auth/*   │    │ 业务逻辑处理   │    │ 数据库操作   │  │    │
│   │   └────────────────┘    └───────┬────────┘    └──────────────┘  │    │
│   │                                  │                                  │    │
│   │                                  ▼                                  │    │
│   │                    ┌────────────────────┐                          │    │
│   │                    │     JwtUtils      │                          │    │
│   │                    │  - 生成Token      │                          │    │
│   │                    │  - 验证Token      │                          │    │
│   │                    │  - BCrypt加密     │                          │    │
│   │                    └────────────────────┘                          │    │
│   └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│   ┌─────────────────────────────────────────────────────────────────┐    │
│   │                 JwtAuthenticationFilter                          │    │
│   │   ┌─────────────────────────────────────────────────────────┐    │    │
│   │   │ 1. 拦截每个HTTP请求                                      │    │    │
│   │   │ 2. 从请求头提取 Token (Authorization: Bearer xxx)        │    │    │
│   │   │ 3. 验证 Token 有效性                                     │    │    │
│   │   │ 4. 查询用户信息                                            │    │    │
│   │   │ 5. 设置 Security Context                                  │    │    │
│   │   └─────────────────────────────────────────────────────────┘    │    │
│   └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│   ┌─────────────────────────────────────────────────────────────────┐    │
│   │                    SecurityConfig                                │    │
│   │   ┌─────────────────────────────────────────────────────────┐    │    │
│   │   │ - 配置哪些路径需要认证                                    │    │    │
│   │   │ - 配置 CORS 跨域策略                                     │    │    │
│   │   │ - 配置无状态会话 (STATELESS)                            │    │    │
│   │   │ - 添加 JWT 过滤器                                        │    │    │
│   │   └─────────────────────────────────────────────────────────┘    │    │
│   └─────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                            MySQL Database                              │
│                                                                          │
│   ┌─────────────────────────────────────────────────────────────┐        │
│   │                          users 表                            │        │
│   │   ┌─────────────────────────────────────────────────────┐    │        │
│   │   │ id | username | password | role | status | ...    │    │        │
│   │   └─────────────────────────────────────────────────────┘    │        │
│   └─────────────────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 Token认证流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Token 认证完整流程                               │
└─────────────────────────────────────────────────────────────────────┘

【登录流程】
┌──────────┐    POST /api/auth/login     ┌──────────────┐
│ 前端     │ ─────────────────────────▶ │ AuthController│
│ login    │   {username, password}      └──────┬───────┘
└──────────┘                                 │
                                              ▼
                                     ┌──────────────┐
                                     │ AuthService  │
                                     │ 1.查询用户   │
                                     │ 2.验证密码   │
                                     └──────┬───────┘
                                           │密码正确
                                           ▼
                                    ┌──────────────┐
                                    │  JwtUtils    │
                                    │ generateToken│
                                    └──────┬───────┘
                                           │Token生成
                                           ▼
┌──────────┐    {token, userInfo}      ┌──────────────┐
│ 前端     │ ◀─────────────────────────│ AuthController│
│          │                           └──────────────┘
└────┬─────┘
     │存储Token到localStorage
     │window.location.href = '/index.html'
     ▼
┌──────────┐
│ 主页面   │
└──────────┘

【后续请求流程】
┌──────────┐    GET /api/records     ┌──────────────┐
│ 前端     │ ─────────────────────────▶ │ JwtAuth      │
│ index    │   Header: Authorization    │ Filter       │
│          │   Bearer eyJhbGciOi...      └──────┬───────┘
└──────────┘                                  │
                                              ▼
                                    ┌──────────────┐
                                    │  JwtUtils    │
                                    │ validateToken│
                                    │ getUsername  │
                                    └──────┬───────┘
                                           │有效
                                           ▼
                                    ┌──────────────┐
                                    │ Security     │
                                    │ Context      │
                                    │ 设置认证信息  │
                                    └──────┬───────┘
                                           │
                                           ▼
                                    ┌──────────────┐
                                    │ Controller   │
                                    │ 处理请求     │
                                    └──────┬───────┘
                                           │
                                           ▼
                                    ┌──────────────┐
                                    │ 返回数据     │
                                    └──────────────┘
```

***

## 3. 核心组件实现

### 3.1 用户实体类 (User.java)

```java
package com.hspyx.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;              // 用户ID，主键
    private String username;      // 用户名，唯一标识
    private String password;       // 密码，BCrypt加密存储
    private String nickname;      // 昵称
    private String email;         // 邮箱
    private String phone;          // 手机号
    private String role;          // 角色：USER（普通用户）/ ADMIN（管理员）
    private Integer status;       // 状态：1（正常）/ 0（禁用）
    private LocalDateTime createTime;  // 创建时间
    private LocalDateTime updateTime;   // 更新时间
}
```

**字段说明：**

- `username`：用户登录账号，唯一约束
- `password`：使用BCrypt加密存储，永不明文保存
- `role`：区分用户权限，ADMIN可访问管理功能
- `status`：可禁用账号而不删除数据

***

### 3.2 请求响应DTO

#### LoginRequest.java - 登录请求

```java
package com.hspyx.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;  // 用户名
    private String password;   // 密码
}
```

#### RegisterRequest.java - 注册请求

```java
package com.hspyx.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;  // 用户名（必填，唯一）
    private String password;  // 密码（必填）
    private String nickname; // 昵称（可选）
    private String email;    // 邮箱（可选）
    private String phone;    // 手机号（可选）
}
```

#### AuthResponse.java - 认证响应

```java
package com.hspyx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;       // JWT Token
    private String username;     // 用户名
    private String nickname;     // 昵称
    private String role;         // 角色
    private Long expiresIn;      // 过期时间（毫秒）
    private Integer code;        // 状态码：200成功，401失败
    private String message;       // 消息

    // 成功响应工厂方法
    public static AuthResponse success(String token, String username,
                                       String nickname, String role) {
        return new AuthResponse(token, username, nickname, role,
                               86400000L, 200, "登录成功");
    }

    // 失败响应工厂方法
    public static AuthResponse error(String message) {
        return new AuthResponse(null, null, null, null, null, 401, message);
    }
}
```

***

### 3.3 JWT工具类 (JwtUtils.java)

```java
package com.hspyx.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    // JWT密钥（从配置文件读取，默认值保证至少32字节）
    @Value("${jwt.secret:hspyx-secret-key-for-jwt-token-generation-must-be-long-enough}")
    private String secret;

    // Token过期时间（24小时）
    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    /**
     * 生成JWT Token
     * @param username 用户名
     * @param role 用户角色
     * @return JWT字符串
     */
    public String generateToken(String username, String role) {
        // 创建声明信息
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);  // 存储用户名
        claims.put("role", role);        // 存储角色

        // 构建Token
        return Jwts.builder()
                .claims(claims)                    // 自定义声明
                .subject(username)                  // 主题（用户名）
                .issuedAt(new Date())             // 签发时间
                .expiration(new Date(System.currentTimeMillis() + expiration))  // 过期时间
                .signWith(getSigningKey())        // 签名
                .compact();                       // 生成字符串
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getPayload().getSubject();
    }

    /**
     * 从Token中获取角色
     */
    public String getRoleFromToken(String token) {
        return parseToken(token).getPayload().get("role", String.class);
    }

    /**
     * 验证Token有效性
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 检查Token是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = parseToken(token).getPayload().getExpiration();
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    /**
     * 解析Token
     */
    private Jws<Claims> parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())  // 使用密钥验证
                .build()
                .parseSignedClaims(token);  // 解析并验证签名
    }

    /**
     * 获取签名密钥（确保至少32字节用于HS256）
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            keyBytes = paddedKey;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

**JWT Token 结构：**

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTcwOTQ4MjAwMCwiZXhwIjoxNzA5NTY4NDAwfQ.
                   │                    │                  │               │
        Header (Base64)    Payload/Claims (Base64)   Signature (Base64)
                   │                    │                  │
        {"alg":"HS256",     {"sub":"admin",          HMACSHA256(
         "typ":"JWT"}        "role":"ADMIN",         header+payload,
                             "iat":1709482000,       secret)
                             "exp":1709568400}
```

***

### 3.4 JWT认证过滤器 (JwtAuthenticationFilter.java)

```java
package com.hspyx.filter;

import com.hspyx.entity.User;
import com.hspyx.mapper.UserMapper;
import com.hspyx.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserMapper userMapper) {
        this.jwtUtils = jwtUtils;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 从请求头获取 Authorization
        String authHeader = request.getHeader("Authorization");

        // 2. 检查是否包含 Bearer Token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // 提取Token（去掉 "Bearer " 前缀）
            String token = authHeader.substring(7);

            // 3. 验证Token有效性
            if (jwtUtils.validateToken(token)) {
                // 提取用户信息
                String username = jwtUtils.getUsernameFromToken(token);
                String role = jwtUtils.getRoleFromToken(token);

                // 4. 检查是否已认证且用户存在
                if (username != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                    // 从数据库查询用户
                    User user = userMapper.findByUsername(username);

                    // 5. 验证用户存在且状态正常
                    if (user != null && user.getStatus() == 1) {
                        // 6. 创建认证令牌
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user,           // 认证主体（用户对象）
                                        null,           // 凭证（密码已验证，不需要）
                                        Collections.singletonList(
                                            new SimpleGrantedAuthority("ROLE_" + role)
                                        )               // 权限列表
                                );

                        // 7. 设置到安全上下文
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        }

        // 8. 继续过滤器链
        filterChain.doFilter(request, response);
    }
}
```

**过滤器执行时机：**

```
请求进入 → Filter → Controller → Filter继续 → 响应返回
                │
                ▼
         JwtAuthenticationFilter
         1. 检查Token
         2. 验证Token
         3. 设置SecurityContext
```

***

### 3.5 用户数据访问层 (UserMapper.java)

```java
package com.hspyx.mapper;

import com.hspyx.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);

    /**
     * 根据ID查询用户
     */
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);

    /**
     * 插入用户
     * useGeneratedKeys: 使用自增主键
     * keyProperty: 将主键值映射到对象的id属性
     */
    @Insert("INSERT INTO users(username, password, nickname, email, phone, role, status, create_time) " +
            "VALUES(#{username}, #{password}, #{nickname}, #{email}, #{phone}, #{role}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /**
     * 更新用户信息
     */
    @Update("UPDATE users SET nickname = #{nickname}, email = #{email}, " +
            "phone = #{phone}, update_time = NOW() WHERE id = #{id}")
    int update(User user);

    /**
     * 更新密码
     */
    @Update("UPDATE users SET password = #{password}, update_time = NOW() WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /**
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(*) FROM users WHERE username = #{username}")
    int countByUsername(String username);
}
```

***

### 3.6 认证服务层 (AuthService.java)

```java
package com.hspyx.service;

import com.hspyx.dto.AuthResponse;
import com.hspyx.dto.LoginRequest;
import com.hspyx.dto.RegisterRequest;
import com.hspyx.entity.User;
import com.hspyx.mapper.UserMapper;
import com.hspyx.utils.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 用户登录
     * 流程：查询用户 → 验证密码 → 生成Token → 返回响应
     */
    public AuthResponse login(LoginRequest request) {
        // 1. 查询用户
        User user = userMapper.findByUsername(request.getUsername());

        // 2. 检查用户是否存在
        if (user == null) {
            return AuthResponse.error("用户不存在");
        }

        // 3. 检查账号状态
        if (user.getStatus() != 1) {
            return AuthResponse.error("账号已被禁用");
        }

        // 4. 验证密码（BCrypt自动比对）
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return AuthResponse.error("密码错误");
        }

        // 5. 生成Token
        String token = jwtUtils.generateToken(user.getUsername(), user.getRole());

        // 6. 返回成功响应
        return AuthResponse.success(token, user.getUsername(),
                user.getNickname() != null ? user.getNickname() : user.getUsername(),
                user.getRole());
    }

    /**
     * 用户注册
     * 流程：检查用户名 → 加密密码 → 保存用户 → 生成Token → 返回响应
     */
    public AuthResponse register(RegisterRequest request) {
        // 1. 检查用户名是否已存在
        if (userMapper.countByUsername(request.getUsername()) > 0) {
            return AuthResponse.error("用户名已存在");
        }

        // 2. 创建用户对象
        User user = new User();
        user.setUsername(request.getUsername());
        // BCrypt加密密码
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ?
                        request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole("USER");      // 默认普通用户
        user.setStatus(1);          // 状态正常

        // 3. 保存到数据库
        userMapper.insert(user);

        // 4. 生成Token（注册后自动登录）
        String token = jwtUtils.generateToken(user.getUsername(), user.getRole());

        // 5. 返回成功响应
        return AuthResponse.success(token, user.getUsername(),
                                  user.getNickname(), user.getRole());
    }

    /**
     * 获取当前用户信息
     */
    public User getCurrentUser(String username) {
        return userMapper.findByUsername(username);
    }
}
```

***

### 3.7 认证控制器 (AuthController.java)

```java
package com.hspyx.controller;

import com.hspyx.dto.AuthResponse;
import com.hspyx.dto.LoginRequest;
import com.hspyx.dto.RegisterRequest;
import com.hspyx.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录",
               description = "使用用户名和密码登录，返回JWT Token")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);

        // 登录成功返回200，失败返回401
        if (response.getToken() != null) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).body(response);
    }

    /**
     * 用户注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册",
               description = "注册新用户，自动登录并返回JWT Token")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);

        // 注册成功返回200，失败返回400
        if (response.getToken() != null) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(400).body(response);
    }
}
```

***

## 4. 前端实现

### 4.1 登录页面 (login.html)

**登录功能实现：**

```javascript
// 1. 发送登录请求
async function handleLogin() {
    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value;

    // 前端验证
    if (!username || !password) {
        showAlert('login', '请输入用户名和密码');
        return;
    }

    // 显示加载状态
    setLoading('loginBtn', true);

    try {
        // 2. 调用登录API
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();

        // 3. 处理响应
        if (data.token) {
            // 4. 存储Token到localStorage
            localStorage.setItem('token', data.token);
            localStorage.setItem('username', data.username);
            localStorage.setItem('nickname', data.nickname);
            localStorage.setItem('role', data.role);

            showAlert('login', '登录成功！正在跳转...');

            // 5. 跳转到主页
            setTimeout(() => {
                window.location.href = '/index.html';
            }, 500);
        } else {
            showAlert('login', data.message || '登录失败');
        }
    } catch (error) {
        showAlert('login', '网络错误，请稍后重试');
    } finally {
        setLoading('loginBtn', false);
    }
}
```

**注册功能实现：**

```javascript
async function handleRegister() {
    const username = document.getElementById('regUsername').value.trim();
    const nickname = document.getElementById('regNickname').value.trim();
    const password = document.getElementById('regPassword').value;
    const password2 = document.getElementById('regPassword2').value;
    const email = document.getElementById('regEmail').value.trim();

    // 前端验证
    if (!username || !password) {
        showAlert('register', '用户名和密码不能为空');
        return;
    }

    if (password !== password2) {
        showAlert('register', '两次输入的密码不一致');
        return;
    }

    if (password.length < 6) {
        showAlert('register', '密码长度至少6位');
        return;
    }

    try {
        // 调用注册API
        const response = await fetch(`${API_BASE}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, nickname, password, email })
        });

        const data = await response.json();

        if (data.token) {
            // 存储Token并跳转
            localStorage.setItem('token', data.token);
            localStorage.setItem('username', data.username);
            localStorage.setItem('nickname', data.nickname);
            localStorage.setItem('role', data.role);

            window.location.href = '/index.html';
        } else {
            showAlert('register', data.message || '注册失败');
        }
    } catch (error) {
        showAlert('register', '网络错误，请稍后重试');
    }
}
```

### 4.2 主页面 Token 处理 (index.html)

**页面访问控制：**

```javascript
// 检查是否已登录
function checkAuth() {
    const token = localStorage.getItem('token');
    const username = localStorage.getItem('username');
    const nickname = localStorage.getItem('nickname');
    const role = localStorage.getItem('role');

    // 未登录则跳转到登录页
    if (!token) {
        window.location.href = '/login.html';
        return false;
    }

    // 显示用户信息
    document.getElementById('currentUserName').textContent = nickname || username || '未知用户';
    document.getElementById('currentUserRole').textContent = role === 'ADMIN' ? '管理员' : '普通用户';
    return true;
}

// 退出登录
function logout() {
    // 清除所有存储的认证信息
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('nickname');
    localStorage.removeItem('role');

    // 跳转到登录页
    window.location.href = '/login.html';
}
```

**API请求封装（自动携带Token）：**

```javascript
const API_BASE = '/api';

// 封装的API请求方法
async function apiRequest(url, options = {}) {
    const token = localStorage.getItem('token');

    // 默认请求头
    const defaultHeaders = {
        'Content-Type': 'application/json'
    };

    // 添加Token到请求头
    if (token) {
        defaultHeaders['Authorization'] = 'Bearer ' + token;
    }

    try {
        const response = await fetch(API_BASE + url, {
            ...options,
            headers: {
                ...defaultHeaders,
                ...options.headers
            }
        });

        // Token过期或无效
        if (response.status === 401 || response.status === 403) {
            logout();
            return null;
        }

        return response;
    } catch (error) {
        console.error('API请求失败:', error);
        alert('网络错误，请稍后重试');
        return null;
    }
}

// 使用示例
async function loadRecords() {
    const response = await apiRequest('/records');
    if (response) {
        const data = await response.json();
        // 处理数据...
    }
}
```

***

## 5. 数据库设计

### 5.1 用户表结构

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（唯一）',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    nickname VARCHAR(100) COMMENT '昵称',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    role VARCHAR(20) DEFAULT 'USER' COMMENT '角色：USER/ADMIN',
    status INT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 5.2 初始数据

```sql
-- 管理员账号 (密码: password)
INSERT INTO users (username, password, nickname, role, status) VALUES
('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '系统管理员', 'ADMIN', 1);

-- 测试用户 (密码: password)
INSERT INTO users (username, password, nickname, role, status) VALUES
('test', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '测试用户', 'USER', 1);
```

**注意：** 上述密码的明文是 `password`

***

## 6. 安全机制详解

### 6.1 密码安全

**BCrypt加密原理：**

```
明文密码: "admin123"
       ↓
BCrypt加密 (自动加盐)
       ↓
存储值: "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi"
       ↓
验证时: BCrypt自动提取盐值，重新加密输入，比对结果
```

**为什么使用BCrypt：**

- 自动加盐：每个密码使用不同的盐值
- 计算成本高：防止暴力破解
- 自适应：可调节工作因子

### 6.2 Token安全

**Token包含的信息：**

- `sub` (subject): 用户名
- `role`: 用户角色
- `iat`: 签发时间
- `exp`: 过期时间
- `signature`: 签名（防篡改）

**Token验证流程：**

```
1. 提取Token
2. 验证签名（确保未被篡改）
3. 检查过期时间（防止被盗用）
4. 提取用户信息
5. 验证用户存在且状态正常
6. 设置认证上下文
```

### 6.3 Spring Security配置

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // 1. 禁用CSRF（前后端分离不使用Cookie）
            .csrf(csrf -> csrf.disable())

            // 2. 配置CORS（允许跨域）
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 3. 无状态会话（不使用Session存储用户状态）
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 4. 授权配置
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()  // 所有请求放行（JWT过滤器处理认证）
            )

            // 5. 添加JWT过滤器（在用户名密码认证之前）
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class)

            .build();
    }
}
```

***

## 7. 配置文件说明

### 7.1 SecurityConfig.java - 安全配置

- 配置哪些路径需要认证
- 配置CORS跨域策略
- 配置无状态会话
- 添加JWT认证过滤器

### 7.2 SwaggerConfig.java - API文档配置

```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
            .info(new Info()
                    .title("二次压降检测系统 API")
                    .version("1.0.0")
                    .description("RESTful API文档")
                    .contact(new Contact()
                            .name("技术支持")
                            .email("support@hspyx.com")));
}
```

### 7.3 WebConfig.java - Web配置

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 根路径跳转到登录页
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("redirect:/login.html");
    }

    // 静态资源映射
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}
```

***

## 8. 项目文件清单

| 文件路径                                  | 功能说明              |
| ------------------------------------- | ----------------- |
| `entity/User.java`                    | 用户实体类             |
| `dto/LoginRequest.java`               | 登录请求DTO           |
| `dto/RegisterRequest.java`            | 注册请求DTO           |
| `dto/AuthResponse.java`               | 认证响应DTO           |
| `mapper/UserMapper.java`              | 用户数据访问层           |
| `service/AuthService.java`            | 认证业务逻辑            |
| `utils/JwtUtils.java`                 | JWT工具类            |
| `filter/JwtAuthenticationFilter.java` | JWT认证过滤器          |
| `controller/AuthController.java`      | 认证控制器             |
| `config/SecurityConfig.java`          | Spring Security配置 |
| `config/SwaggerConfig.java`           | Swagger文档配置       |
| `config/WebConfig.java`               | Web MVC配置         |
| `static/login.html`                   | 登录注册页面            |

***

## 9. 使用说明

### 9.1 启动步骤

1. **执行数据库脚本**

```bash
mysql -u root -p testdata < sql/init_database.sql
```

1. **启动应用**

```bash
cd web-test
mvn spring-boot:run
```

### 9.2 访问地址

| 功能    | 地址                                      |
| ----- | --------------------------------------- |
| 登录页面  | <http://localhost:8080/>                |
| 主页面   | <http://localhost:8080/index.html>      |
| API文档 | <http://localhost:8080/swagger-ui.html> |

### 9.3 默认账号

| 用户名   | 密码       | 角色     |
| ----- | -------- | ------ |
| admin | password | 管理员    |
| test  | password | <br /> |

这份系统架构图展示了一个经典的**前后端分离**应用架构，核心是基于 **JWT (JSON Web Token)** 实现的无状态身份认证系统。整个系统自上而下可以清晰地分为三个主要层级：前端、后端和数据库层。

以下是对架构图各部分的详细描述：

### 1. 前端层 (HTML/JavaScript)

前端主要负责与用户的直接交互以及 Token 的存储与携带。

- **页面交互：** 包含 `login.html`（处理登录和注册）和 `index.html`（受保护的主页面）。
- **状态存储：** 前端在用户成功登录后，会将后端返回的凭证（包括 JWT 令牌、用户名、昵称、角色等）安全地存储在浏览器的 **`localStorage`** 中。
- **通信机制：** 在后续访问受保护资源（如调用 API 请求）时，前端会自动从 `localStorage` 中取出 Token，并将其附加在 HTTP 请求头中（格式为 `Authorization: Bearer <token>`），发送给后端。

------

### 2. 后端层 (Spring Boot Backend)

后端是系统的核心，负责处理业务逻辑、安全拦截和 Token 签发。它主要由以下几个关键组件构成：

- **安全拦截与配置 (Spring Security & Filters)：**
  - **`SecurityConfig`：** 系统的安全大脑，配置了跨域策略 (CORS)、无状态会话管理（因为使用了 JWT，服务器不保存 Session），以及指定了哪些路由需要拦截。
  - **`JwtAuthenticationFilter`：** 这是一个关键的拦截器。它会拦截每一个到达服务器的 HTTP 请求，检查请求头中是否包含有效的 Bearer Token。如果包含，它会解析 Token，验证其合法性，并在当前线程的安全上下文 (`Security Context`) 中标记该用户已认证。
- **认证核心流程 (Authentication Flow)：**
  - **`AuthController`：** 暴露统一的认证接口（如 `/api/auth/login`）。
  - **`AuthService`：** 处理具体的登录/注册业务逻辑，比如校验用户名是否存在、使用 BCrypt 算法比对密码等。
  - **`JwtUtils`：** JWT 工具类，专门负责 Token 的生成（在登录成功后）和验证（在过滤器拦截时）。
- **数据访问：** 通过 `UserMapper` 组件（基于 MyBatis）与底层数据库进行数据交互，执行增删改查操作。

------

### 3. 数据层 (MySQL Database)

处于系统最底层，负责持久化存储业务数据。

- **`users` 表：** 核心数据表，记录了用户的唯一标识（`id`, `username`）、加密后的密码（`password`，不可逆的 BCrypt 加密）、权限标识（`role`）以及账户状态（`status`）等。

### 💡 整体运作流程总结

1. **登录流程：** 用户在前端输入账号密码 $\rightarrow$ 发送给后端 `AuthController` $\rightarrow$ `AuthService` 验证通过 $\rightarrow$ `JwtUtils` 签发 JWT $\rightarrow$ 返回给前端并存入 `localStorage`。
2. **鉴权流程：** 前端请求受保护数据（携带 JWT） $\rightarrow$ 后端 `JwtAuthenticationFilter` 拦截 $\rightarrow$ 验证 JWT 有效 $\rightarrow$ 放行请求至对应的 Controller $\rightarrow$ 返回业务数据给前端。