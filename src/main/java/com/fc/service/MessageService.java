package com.fc.service;

import com.fc.mapper.MessageMapper;
import com.fc.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
@Service
public class MessageService {
    @Autowired
    private MessageMapper messageMapper;

    public Map<String, List<Message>> listMessage(Integer userId){
        List<Message> messagelist = messageMapper.listMessageByUserId(userId);
        Map<String , List<Message>> map = new HashMap<>();
        for (Message message : messagelist){
            String time = message.getMessageDate();
            if (map.get(time) == null){
                map.put(time, new LinkedList<Message>());
                map.get(time).add(message);
            }else{
                map.get(time).add(message);
            }
        }
        return map;
    }
}
