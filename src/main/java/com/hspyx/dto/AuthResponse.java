package com.hspyx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token; // JWT Token
    private String username; // 用户名
    private String nickname; // 昵称
    private String role; // 角色
    private Long expiresIn; // 过期时间
    private Integer code; // 状态码
    private String message; // 消息


    /**
     * 成功响应方法
     */
    public static AuthResponse success(String token, String username, String nickname, String role) {
        return new AuthResponse(token, username, nickname, role, 86400000L, 200, "登录成功");
    }

    /**
     * 失败响应方法
     */
    public static AuthResponse error(String message) {
        return new AuthResponse(null, null, null, null, null, 401, message);
    }
}
