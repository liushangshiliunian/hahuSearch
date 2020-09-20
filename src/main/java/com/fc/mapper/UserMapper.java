package com.fc.mapper;

import com.fc.model.User;
import com.sun.org.glassfish.gmbal.ParameterNames;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {
    void insertUser(User user);

    int selectEmailCount(String email);

    Integer selectUserIdByEmailAndPassword(@Param("email")String email, @Param("password")String password);

    Integer selectActivationStateByUserId(@Param("userId") Integer userId);

    User selectUserInfoByUserId(@Param("userId")Integer userId);

    String getWeiboUserId(Integer userId);

    User selectUserInfoByWeiboUserId(String weiboUserId);

    int insertWeiboUser(User user);

    void updateActivationStateByActivationCode(@Param("activationCode")String activationCode);

    User selectProfileInfoByUserId(@Param("userId") Integer userId);

    void updateProfile(User user);

    List<User> listUserInfoByUserId(List<Integer> userIdList);
}
