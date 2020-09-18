package com.fc.controller;

import com.fc.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/")
public class IndexCotroller {
    @Autowired
    private UserService userService;

    @RequestMapping(value = {"/","/index"})
    public String index(){return  "index";}
}
