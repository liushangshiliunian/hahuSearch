package com.fc.controller;

import com.fc.model.Collection;
import com.fc.model.Message;
import com.fc.model.User;
import com.fc.service.CollectionService;
import com.fc.service.MessageService;
import com.fc.service.UserService;
import com.fc.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/")
public class IndexCotroller {
    @Autowired
    private UserService userService ;

    @Autowired
    private MessageService messageService;

    @Autowired
    private CollectionService collectionService;

    @RequestMapping(value = {"/","/index"})
    public String index(){return  "index";}

    @RequestMapping("getIndexDetail")
    @ResponseBody
    public Response getIndexDetail(Integer page, HttpServletRequest request){
        Integer userId = userService.getUserIdFromRedis(request);
        Map<String, Object> map = userService.getIndexDetail(userId, page);
        return new Response(0,"",map);
    }

    @RequestMapping("/explore")
    public String explore(){
        return "explore";
    }

    @RequestMapping("/message")
    public String message(HttpServletRequest request, Model model){
        Integer userId = userService.getUserIdFromRedis(request);
        Map<String, List<Message>> map = messageService.listMessage(userId);
        model.addAttribute("map", map);
        return "message";
    }

    @RequestMapping("/setting")
    public String setting (HttpServletRequest request, Model model){
        Integer userId = userService.getUserIdFromRedis(request);
        User user = userService.getProfileInfo(userId);
        model.addAttribute("user",user);
        return "editProfile";
    }

    @RequestMapping("/editProfile")
    public String editProfile(User user, HttpServletRequest request){
        Integer userId = userService.getUserIdFromRedis(request);
        user.setUserId(userId);
        userService.updateProfile(user);
        return "redirect:/profile/"+userId;
    }


}
