package com.hspyx.dto;

import lombok.Data;


/**
 * 登录请求
 */
@Data
public class LoginRequest {
    private String username; // 用户名
    private String password; // 密码
}
