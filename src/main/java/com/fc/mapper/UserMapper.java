package com.fc.mapper;

import com.fc.model.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {
    void insertUser(User user);

    int selectEmailCount(String email);

    Integer selectUserIdByEmailAndPassword(@Param("email")String email, @Param("password")String password);

    Integer selectActivationStateByUserId(@Param("userId") Integer userId);

    User selectUserInfoByUserId(@Param("userId")Integer userId);
}
