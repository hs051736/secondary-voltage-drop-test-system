package com.hspyx.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id; // 用户ID，主键
    private String username; // 用户名，唯一标识
    private String password; // 密码，Bcrypt加密存储
    private String nickname; // 昵称
    private String email; // 邮箱
    private String phone; // 手机号
    private String role; // 角色：USER（普通用户）；ADMIN（管理员）
    private Integer status; // 状态
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}
