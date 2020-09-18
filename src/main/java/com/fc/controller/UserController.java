package com.fc.controller;

import com.fc.service.UserService;
import com.fc.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;


@Controller
@RequestMapping("/")
public class UserController {
    @Autowired
    private UserService userService;

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
}
