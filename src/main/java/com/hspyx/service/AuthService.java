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

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 用户登录
     * 流程：查询用户 验证密码 生成Token 返回响应
     * @return
     */
    public AuthResponse login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername());

        if (user == null) {
            return AuthResponse.error("用户不存在");
        }

        if (user.getStatus() != 1) {
            return AuthResponse.error("账号已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return AuthResponse.error("密码错误");
        }

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole());
        return AuthResponse.success(token, user.getUsername(),
                user.getNickname() != null ? user.getNickname() : user.getUsername(),
                user.getRole());
    }

    /**
     * 用户注册
     * 流程：检查用户名 → 加密密码 → 保存用户 → 生成Token → 返回响应
     * @return
     */
    public AuthResponse register(RegisterRequest request) {
        if (userMapper.countByUsername(request.getUsername()) > 0) {
            return AuthResponse.error("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());

        // BCrypt加密密码
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole("USER");
        user.setStatus(1);

        userMapper.insert(user);

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole());
        return AuthResponse.success(token, user.getUsername(), user.getNickname(), user.getRole());
    }

    public User getCurrentUser(String username) {
        return userMapper.findByUsername(username);
    }
}
