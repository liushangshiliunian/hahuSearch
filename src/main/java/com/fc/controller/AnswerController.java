package com.fc.controller;

import com.fc.service.AnswerService;
import com.fc.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/")
public class AnswerController {
    @Autowired
    private AnswerService answerService;


    @RequestMapping("/listTodayHotAnswer")
    @ResponseBody
    public Response listTodayHotAnswer(){
        Map<String, Object> map = answerService.listTodayHotAnswer();
        return new Response(0,"",map);
    }

    @RequestMapping("/listMonthHotAnswer")
    @ResponseBody
    public Response listMonthHotAnswer(){
        Map<String, Object> map = answerService.listMonthHotAnswer();
        return new Response(0,"",map);
    }
}
