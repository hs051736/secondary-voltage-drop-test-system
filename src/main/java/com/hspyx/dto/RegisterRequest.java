package com.hspyx.dto;

import lombok.Data;

/**
 * 注册请求
 */
@Data
public class RegisterRequest {
    private String username; // 用户名
    private String password; // 密码
    private String nickname; // 昵称
    private String email; // 邮箱
    private String phone; // 手机号
}
