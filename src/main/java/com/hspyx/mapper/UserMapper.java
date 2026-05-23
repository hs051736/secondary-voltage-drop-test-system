package com.hspyx.mapper;

import com.hspyx.entity.User;
import org.apache.ibatis.annotations.*;

/**
 * 用户数据访问
 */
@Mapper
public interface UserMapper {

    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);

    @Insert("INSERT INTO users(username, password, nickname, email, phone, role, status, create_time) " +
            "VALUES(#{username}, #{password}, #{nickname}, #{email}, #{phone}, #{role}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE users SET nickname = #{nickname}, email = #{email}, phone = #{phone}, update_time = NOW() WHERE id = #{id}")
    int update(User user);

    @Update("UPDATE users SET password = #{password}, update_time = NOW() WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Select("SELECT COUNT(*) FROM users WHERE username = #{username}")
    int countByUsername(String username);
}
