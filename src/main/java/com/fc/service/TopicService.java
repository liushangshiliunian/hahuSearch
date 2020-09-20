package com.fc.service;

import com.fc.mapper.AnswerMapper;
import com.fc.mapper.QuestionMapper;
import com.fc.mapper.TopicMapper;
import com.fc.model.Answer;
import com.fc.model.PageBean;
import com.fc.model.Question;
import com.fc.model.Topic;
import com.fc.util.MyUtil;
import com.fc.util.RedisKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;

@Service
public class TopicService {
    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private AnswerMapper answerMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private JedisPool jedisPool;

    public Map<String, Object> listTopicByTopicName(String topicName){
        HashMap<String, Object> map = new HashMap<>();
        List<Topic> topicList = topicMapper.listTopicByTopicName("%" + topicName + "%");

        map.put("topicList", topicList);
        return map;
    }

    public Map<String, Object> listAllTopic(){
        HashMap<String, Object> map = new HashMap<>();
        List<Topic> hotTopicList = topicMapper.listHotTopic();
        List<Topic> rootTopicList = topicMapper.listRootTopic();
        map.put("hotTopicList",hotTopicList);
        map.put("rootTopicList",rootTopicList);

        return map;
    }

    public Map<String, Object > getTopicDetail(Integer topicId, Boolean allQuestion, Integer curPage, Integer userId){
        Map<String, Object> map = new HashMap<>();
        Topic topic = topicMapper.selectTopicByTopicId(topicId);
        Jedis jedis = jedisPool.getResource();

        Long followedCount = jedis.zcard(topicId + RedisKey.FOLLOW_TOPIC);
        topic.setFollowedCount(Integer.parseInt(followedCount+""));

        if (allQuestion == null || allQuestion.equals(false)){
            List<Integer> questionIdlList = topicMapper.selectQuestionIdByTopicId(topic.getTopicId());
            if (questionIdlList.size()>0){
                PageBean<Answer> pageBean = _listGoodAnswerByQuestionId(questionIdlList, curPage, jedis, userId);
                map.put("pageBean",pageBean);
            }else{
                map.put("pageBean", new PageBean<Answer>());
            }
        }else{
            PageBean<Question> pageBean = _listAllQuestionByTopicId(topic.getTopicId(), curPage, jedis);
            map.put("pageBean",pageBean);
            map.put("allQuestion", true);
        }

        map.put("topic", topic);
        jedisPool.returnResource(jedis);
        return map;
    }

    public PageBean<Answer> _listGoodAnswerByQuestionId(List<Integer> questionIdList, Integer curPage, Jedis jedis, Integer userId){
        curPage = curPage == null ? 1: curPage;
        int limit = 8;
        int offset = (curPage - 1 )*limit;

        int allCount = answerMapper.listAnswerCountByQuestionId(questionIdList);
        int allPage = 0;
        if (allCount <= limit){
            allPage = 1 ;
        }else if (allCount / limit == 0){
            allPage = allCount / limit;
        }else {
            allPage = allCount / limit+1;
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("offset",offset);
        map.put("limit", limit);
        map.put("questionIdList", questionIdList);

        List<Answer> answerList = answerMapper.listGoodAnswerByQuestionId(map);

        for (Answer answer : answerList){
            Long rank = jedis.zrank(answer.getAnswerId() + RedisKey.LIKED_ANSWER, String.valueOf(userId));
            answer.setLikeState(rank == null ? "false" : "true");
        }

        PageBean<Answer> pageBean = new PageBean<>(allPage, curPage);
        pageBean.setList(answerList);
        return pageBean;
    }

    private PageBean<Question> _listAllQuestionByTopicId (Integer topicId, Integer curPage, Jedis jedis){
        curPage = curPage == null ? 1: curPage;
        int limit = 8;
        int offset = (curPage -1 ) * limit;
        int allCount = questionMapper.selectQuestionCountByTopicId(topicId);
        int allPage = 0;
        if (allCount <= limit){
            allPage = 1;
        }else if (allCount / limit == 0){
            allPage = allCount/limit;
        }else{
            allPage = allCount / limit+1;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("offset",offset);
        map.put("limit",limit);
        map.put("topicId", topicId);

        List<Integer> questionIdList = questionMapper.listQuestionIdByTopicId(map);
        System.out.println(questionIdList);
        List<Question> questionList = new ArrayList<>();
        if (questionIdList.size() > 0 ){
            questionList = questionMapper.listQuestionByQuestionId(questionIdList);
            for (Question question: questionList){
                Long followedCount = jedis.zcard(question.getQuestionId() + RedisKey.FOLLOWED_QUESTION);
                question.setFollowedCount(Integer.parseInt(followedCount+""));
            }
        }

        PageBean<Question> pageBean = new PageBean<>(allPage, curPage);
        pageBean.setList(questionList);

        return pageBean;
    }

    public List<Topic> listTopicByParentTopicId(Integer parentTopicId){
        List<Topic> list = topicMapper.listTopicByParentId(parentTopicId);
        System.out.println(parentTopicId);
        System.out.println(list);
        return  list;
    }

    public List<Topic> listFollowingTopic(Integer userId){
        Jedis jedis = jedisPool.getResource();
        Set<String> idSet = jedis.zrange(userId + RedisKey.FOLLOW_TOPIC, 0, -1);
        List<Integer> idList = MyUtil.StringSetToIntegerList(idSet);

        List<Topic> list = new ArrayList<>();
        if (idList.size()>0){
            list = topicMapper.listTopicByTopicId(idList);
        }

        jedisPool.returnResource(jedis);
        return list;
    }
}
