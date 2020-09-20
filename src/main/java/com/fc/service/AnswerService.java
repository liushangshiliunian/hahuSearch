package com.fc.service;

import com.fc.mapper.AnswerMapper;
import com.fc.model.Answer;
import com.fc.model.PageBean;
import com.fc.util.RedisKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.xml.crypto.Data;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnswerService {
    @Autowired
    private AnswerMapper answerMapper;

    @Autowired
    private JedisPool jedisPool;

    public PageBean<Answer> listAnswerByUserId (Integer userId,Integer curPage){
        curPage = curPage== null ? 1:curPage;

        int limit = 8;
        int offset = (curPage-1)*limit;

        int allCount=answerMapper.selectAnswerCountByUserId(userId);
        int allPage=0;
        if (allCount <= limit){
            allPage=1;
        }else if (allCount / limit ==0){
            allPage = allCount /limit;
        }else{
            allPage = allCount /limit +1;
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("offset",offset);
        map.put("limit",limit);
        map.put("userId",userId);

        List<Answer> answerList = answerMapper.listAnswerByUserId(map);

        Jedis jedis = jedisPool.getResource();
        for (Answer answer:answerList){
            Long likedCount = jedis.zcard(answer.getAnswerId() + RedisKey.LIKED_ANSWER);
            answer.setLikedCount(Integer.parseInt(likedCount+""));
        }
        PageBean<Answer> pageBean = new PageBean<>(allPage, curPage);
        pageBean.setList(answerList);
        return pageBean;
    }

    public Map<String, Object> listTodayHotAnswer(){
        Map<String, Object> map = new HashMap<>();
        long period=1000 * 60 * 60 * 24L;
        long today = System.currentTimeMillis();
        System.out.println("period:"+period);
        System.out.println("today:" + today);
        System.out.println("today-period" + (today-period));
        System.out.println(new Date(today-period));
        List<Answer> answerList = answerMapper.listAnswerByCreateTime(today - period);
        map.put("answerList",answerList);
        return map;
    }

    public Map<String , Object> listMonthHotAnswer(){
        Map<String, Object> map = new HashMap<>();
        long period=1000 * 60 * 60 * 24 *30L;
        long today = System.currentTimeMillis();
        System.out.println("period:"+period);
        System.out.println("today:" + today);
        System.out.println("today-period" + (today-period));
        System.out.println(new Date(today-period));
        List<Answer> answerList = answerMapper.listAnswerByCreateTime(today - period);
        map.put("answerList",answerList);
        return map;
    }

}
