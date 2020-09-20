package com.fc.controller;

import com.fc.model.Question;
import com.fc.service.QuestionService;
import com.fc.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentController {
    @Autowired
    private QuestionService questionService;
    

}
