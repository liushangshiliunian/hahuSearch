package com.fc.mapper;

import com.fc.model.Message;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface MessageMapper {

    List<Message> listMessageByUserId (@Param("userId") Integer userId);
}
