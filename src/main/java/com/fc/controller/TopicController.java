package com.fc.controller;

import com.fc.model.Topic;
import com.fc.service.TopicService;
import com.fc.service.UserService;
import com.fc.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/")
public class TopicController {
    @Autowired
    private TopicService topicService;

    @Autowired
    private UserService userService;

    @RequestMapping("/listTopicByTopicName")
    public String listTopicByTopicName(String topicName, Model model)throws UnsupportedEncodingException{
        new String (topicName.getBytes("iso8859-1"),"UTF-8");
        Map<String, Object> map = topicService.listTopicByTopicName(topicName);
        model.addAllAttributes(map);
        return "topicList";
    }

    @RequestMapping("/topics")
    public String topic(Model model){
        Map<String, Object> map = topicService.listAllTopic();
        model.addAllAttributes(map);
        return "topics";
    }

    @RequestMapping("/topicDetail/{topicId}")
    public String topicDetail(@PathVariable Integer topicId, Integer page, Boolean allQuestion, Model model, HttpServletRequest request){
        Integer userId = userService.getUserIdFromRedis(request);
        Map<String, Object> map = topicService.getTopicDetail(topicId, allQuestion, page, userId);
        model.addAllAttributes(map);
        return "topicDetail";
    }

    @RequestMapping("/listTopicByParentTopicId")
    @ResponseBody
    public Response listTopicByParentTopicId(Integer parentTopicId){
        List<Topic> topics = topicService.listTopicByParentTopicId(parentTopicId);
        HashMap<String, Object> map = new HashMap<>();
        map.put("topicList", topics);
        return new Response(0,"",map);
    }
}
