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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * JWT认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserMapper userMapper) {
        this.jwtUtils = jwtUtils;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;

        // 1. 先从请求头获取 Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // 2. 如果请求头没有，从URL参数获取（支持PDF导出等场景）
        if (token == null || token.isEmpty()) {
            String tokenParam = request.getParameter("token");
            if (tokenParam != null && !tokenParam.isEmpty()) {
                token = URLDecoder.decode(tokenParam, StandardCharsets.UTF_8);
            }
        }

        // 3. 如果获取到token，进行验证
        if (token != null && !token.isEmpty()) {
            // 4. 验证token有效性
            if (jwtUtils.validateToken(token)) {
                // 提取用户信息
                String username = jwtUtils.getUsernameFromToken(token);
                String role = jwtUtils.getRoleFromToken(token);

                // 5. 检查是否已认证且用户存在
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    // 从数据库查询用户
                    User user = userMapper.findByUsername(username);

                    // 6. 验证用户存在且状态正常
                    if (user != null && user.getStatus() == 1) {
                        // 7. 创建认证令牌
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user,
                                        null,
                                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                                );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
