package com.fc.controller;

import com.fc.model.Collection;
import com.fc.model.User;
import com.fc.service.CollectionService;
import com.fc.service.UserService;
import com.fc.util.Response;
import com.sun.mail.iap.ResponseInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/")
public class CollectionController {
    @Autowired
    private UserService userService;

    @Autowired
    private CollectionService collectionService;

    @RequestMapping("/collections")
    public String collections(){
        return "collectionList";
    }

    @RequestMapping("/addCollection")
    @ResponseBody
    public Response addCollection(Collection collection, HttpServletRequest request){
        Integer userId = userService.getUserIdFromRedis(request);
        collectionService.addCollection(collection, userId);
        return new Response(0);
    }

    @RequestMapping("/listCreatingCollection")
    @ResponseBody
    public Response listCreatingCollection (HttpServletRequest request){
        Integer userId = userService.getUserIdFromRedis(request);
        List<Collection> collectionList = collectionService.listCreatingCollection(userId);
        Map<String,Object> map = new HashMap<>();
        map.put("collectionList",collectionList);
        return new Response(0,"",map);
    }
}
