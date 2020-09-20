package com.fc.controller;

import com.fc.model.Collection;
import com.fc.model.Question;
import com.fc.service.CollectionService;
import com.fc.service.QuestionService;
import com.fc.service.UserService;
import com.fc.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.portlet.MockActionRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/")
public class QuestionController {
    @Autowired
    private CollectionService collectionService;

    @Autowired
    private UserService userService;

    @Autowired
    private QuestionService questionService;

    @RequestMapping("/questionList")
    public String questionList() {return "questionList";}

    @RequestMapping("/ask")
    @ResponseBody
    public Response ask(Question question, String topicNameString, HttpServletRequest request){
        Integer userId = userService.getUserIdFromRedis(request);
        Integer questionId = questionService.ask(question, topicNameString, userId);
        return new Response(0,"",questionId);
    }

    @RequestMapping("/question/{questionId}")
    public String questionDetail(@PathVariable Integer questionId, HttpServletRequest request, Model model){
        Integer userId = userService.getUserIdFromRedis(request);
        Map<String, Object> questionDetail = questionService.getQuestionDetail(questionId, userId);

        List<Collection> collectionList = collectionService.listCreatingCollection(userId);
        questionDetail.put("collectionList",collectionList);
        model.addAllAttributes(questionDetail);
        return "questionDetail";
    }

    @RequestMapping("/listQuestionByPage")
    @ResponseBody
    public Response listQuestionByPage(Integer page){
        Map<String, Object> map = new HashMap<>();
        List<Question> questionList = questionService.listQuestionByPage(page);
        map.put("questionList", questionList);
        return new Response(0, "",map);

    }
}
