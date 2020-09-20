package com.fc.controller;

import com.fc.model.*;
import com.fc.service.*;
import com.fc.util.Response;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private TopicService topicService;

    @RequestMapping("/toLogin")
    public String toLogin(){
        return "toLogin";
    }

    @RequestMapping("/register")
    @ResponseBody
    public Response register (String username,String email, String password){
        Map<String ,String> map= userService.register(username,email,password);
        if (map.get("ok") != null){
            return new Response(0, "系统已经向你的邮箱发送了一封邮件哦，验证后就可以登录啦~");
        }else{
            return new Response(1,"error",map);
        }
    }

    @RequestMapping("/login")
    @ResponseBody
    public Response login (String email, String password, HttpServletResponse response){
        Map<String, Object> map = userService.login(email, password, response);
        if (map.get("error") == null){
            return new Response(0, "", map);
        }else{
            return new Response(1,map.get("error").toString());
        }
    }

    @RequestMapping("/weiboLogin")
    public String weiboLogin(String code,HttpServletResponse response)throws IOException{
        userService.weiboLogin(code,response);
        return "index";
    }

    @RequestMapping("/activate")
    public String activate(String code){
        userService.activate(code);
        return "redirect:/toLogin#activateSuccess";
    }

    @RequestMapping("/logout")
    public String logout(HttpServletRequest request,HttpServletResponse response){
        userService.logout(request,response);
        return "redirect:/toLogin";
    }

    @RequestMapping("/profile/{userId}")
    public String profile(@PathVariable Integer userId, Integer page, HttpServletRequest request, Model model){
        Integer localUserId = userService.getUserIdFromRedis(request);

        Map<String, Object> map = userService.profile(userId, localUserId);

        PageBean<Answer> pageBean = answerService.listAnswerByUserId(userId, page);
        map.put("pageBean",pageBean);

        model.addAllAttributes(map);
        return "profileAnswer";
    }

    @RequestMapping("/profileQuestion/{userId}")
    public String profileQuestion(@PathVariable Integer userId, Integer page,HttpServletRequest request, Model model){
        Integer localUserId = userService.getUserIdFromRedis(request);
        Map<String, Object> map = userService.profile(userId, localUserId);
        PageBean<Question> pageBean = questionService.listQuestionByUserId(userId, page);
        map.put("pageBean",pageBean);
        model.addAllAttributes(map);
        return "profileQuestion";
    }

    @RequestMapping("/profileFollowCollection/{userId}")
    public String profileFollowCollection(@PathVariable Integer userId, HttpServletRequest request, Model model){
        Integer localUserId = userService.getUserIdFromRedis(request);
        Map<String, Object> map = userService.profile(userId, localUserId);
        List<Collection> collectionlist = collectionService.listFollowingCollection(userId);
        map.put("collectionList",collectionlist);

        model.addAllAttributes(map);
        return "profileFollowCollection";
    }

    @RequestMapping("profileFollowQuestion/{userId}")
    public  String profileFollowQuestion(@PathVariable Integer userId, HttpServletRequest request,Model model){
        Integer localUserId = userService.getUserIdFromRedis(request);
        Map<String, Object> map = userService.profile(userId, localUserId);

        List<Question> questionList = questionService.listFollowingQuestion(userId);
        map.put("questionList",questionList);
        model.addAllAttributes(map);
        return "profileFollowQuestion";
    }

    @RequestMapping("/profileCollection/{userId}")
    public String profileCollection(@PathVariable Integer userId, HttpServletRequest request, Model model) {
        Integer localUserId = userService.getUserIdFromRedis(request);
        Map<String, Object> map = userService.profile(userId, localUserId);
        List<Collection> collectionList = collectionService.listCreatingCollection(userId);
        map.put("collectionList", collectionList);
        model.addAllAttributes(map);
        return  "profileCollection";
    }

    @RequestMapping("/profileFollowPeople/{userId}")
    public String profileFollowPeople(@PathVariable Integer userId, HttpServletRequest request, Model model){
        Integer localUserId = userService.getUserIdFromRedis(request);
        Map<String, Object> map = userService.profile(userId, localUserId);
        List<User> userList = userService.listFollowingUser(userId);
        map.put("userList",userList);
        model.addAllAttributes(map);
        return "profileFollowPeople";
    }

    @RequestMapping("/profileFollowTopic/{userId}")
    public String profileFollowTopic(@PathVariable Integer userId, HttpServletRequest request, Model model){
        Integer localUserId = userService.getUserIdFromRedis(request);
        Map<String, Object> map = userService.profile(userId, localUserId);
        List<Topic> topicList = topicService.listFollowingTopic(userId);
        map.put("topicList",topicList);
        model.addAllAttributes(map);
        return "profileFollowTopic";
    }
}
