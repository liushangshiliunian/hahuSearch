package com.fc.service;

import com.alibaba.fastjson.JSON;
import com.fc.mapper.*;
import com.fc.model.*;
import com.fc.util.MyUtil;
import com.fc.util.RedisKey;
import javafx.beans.binding.ObjectExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.swing.text.html.ObjectView;
import javax.xml.stream.events.Comment;
import java.util.*;

@Service
public class QuestionService {
    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private AnswerMapper answerMapper;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private JedisPool jedisPool;


    public PageBean<Question> listQuestionByUserId(Integer userId, Integer curPage){
        curPage = curPage ==null ? 1 : curPage;

        int limit = 8;
        int offset = (curPage-1)*limit;
        int allCount = questionMapper.selectQuestionCountByUserId(userId);
        int allPage=0;
        if (allCount <= limit){
            allPage = 1;
        }else if (allCount / limit == 0){
            allPage = allCount / limit;
        }else{
            allPage= allCount / limit +1 ;
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("offset",offset);
        map.put("limit",limit);
        map.put("userId",userId);

        List<Question> questionList = questionMapper.listQuestionByUserId(map);

        PageBean<Question> pageBean = new PageBean<>(allPage, curPage);
        pageBean.setList(questionList);

        return pageBean;
    }

    public List<Question> listFollowingQuestion (Integer userId){
        Jedis jedis = jedisPool.getResource();
        Set<String> idSet = jedis.zrange(userId + RedisKey.FOLLOW_QUESTION, 0, -1);
        List<Integer> idList = MyUtil.StringSetToIntegerList(idSet);

        List<Question> list = new ArrayList<>();
        if (idList.size()>0){
            list = questionMapper.listQuestionByQuestionId(idList);
            for (Question question : list){
                int answerCount = answerMapper.selectAnswerCountByQuestionId(question.getQuestionId());
                question.setAnswerCount(answerCount);
                Long followedCount = jedis.zcard(question.getQuestionId() + RedisKey.FOLLOWED_QUESTION);
                question.setFollowedCount(Integer.parseInt(followedCount+""));
            }
        }

        jedisPool.returnResource(jedis);
        return list;
    }

    public Integer ask (Question question, String topicNameString, Integer userId){
        String[] topicNames = topicNameString.split(",");
        System.out.println(Arrays.toString(topicNames));
        Map<Integer , String> map = new HashMap<>();

        List<Integer> topicIdList = new ArrayList<>();
        for (String topicName : topicNames){
            Topic topic = new Topic();
            Integer topicId = topicMapper.selectTopicIdByTopicName(topicName);
            if (topicId == null){
                topic.setTopicName(topicName);
                topic.setParentTopicId(1);
                topicMapper.insertTopic(topic);
                topicId = topic.getTopicId();
            }
            map.put(topicId, topicName);
            topicIdList.add(topicId);
        }
        String topicKvList = JSON.toJSONString(map);
        question.setTopicKvList(topicKvList);
        question.setCreateTime(System.currentTimeMillis());
        question.setUserId(userId);
        questionMapper.insertQuestion(question);

        for (Integer topicId : topicIdList){
            questionMapper.insertIntoQuestionTopic(question.getQuestionId(), topicId);
        }
        return question.getQuestionId();
    }

    public Map<String, Object> getQuestionDetail(Integer questionId, Integer userId){
        Map<String, Object> map= new HashMap<>();
        Question question = questionMapper.selectQuestionByQuestionId(questionId);

        if (question == null){
            throw new RuntimeException("该问题id不存在~");
        }

        Jedis jedis = jedisPool.getResource();
        jedis.zincrby(RedisKey.QUESTION_SCANED_COUNT, 1, questionId+"");
        question.setScanedCount((int)jedis.zscore(RedisKey.QUESTION_SCANED_COUNT,questionId+"").doubleValue());

        Set<String> userIdSet = jedis.zrange(questionId + RedisKey.FOLLOWED_QUESTION, 0, 9);
        List<Integer> userIdList = MyUtil.StringSetToIntegerList(userIdSet);
        List<User> followedUserList = new ArrayList<>();
        if (userIdList.size()>0){
            followedUserList = userMapper.listUserInfoByUserId(userIdList);
        }

        List<Question> relatedQuestionList = questionMapper.listRelatedQuestion(questionId);
        System.out.println("relatedQuestionList" + relatedQuestionList);

        User askUser = userMapper.selectUserInfoByUserId(question.getUserId());
        question.setUser(askUser);

        List<QuestionComment> questionCommentList = commentMapper.listQuestionCommentByQuestionId(questionId);

        for (QuestionComment comment : questionCommentList){
            User commentUser = userMapper.selectUserInfoByUserId(comment.getUserId());
            comment.setUser(commentUser);

            Long rank = jedis.zrank(userId + RedisKey.LIKED_QUESTION_COMMENT, comment.getQuestionCommentId() + "");
            comment.setLikedState(rank == null ? "false" : "true");

            Long likedCount = jedis.zcard(comment.getQuestionCommentId() + RedisKey.LIKED_QUESTION_COMMENT);
            comment.setLikedCount(Integer.valueOf(likedCount+""));
        }
        question.setQuestionCommentList(questionCommentList);

        List<Answer> answerList = answerMapper.selectAnswerByQuestionId(questionId);
        for (Answer answer : answerList){
            User answerUser = userMapper.selectUserInfoByUserId(answer.getUserId());
            answer.setUser(answerUser);
            List<AnswerComment> answerCommentList = commentMapper.listAnswerCommentByAnswerId(answer.getAnswerId());
            for (AnswerComment comment : answerCommentList){
                User commentUser = userMapper.selectUserInfoByUserId(comment.getUserId());
                comment.setUser(commentUser);
                Long rank=jedis.zrank(userId + RedisKey.LIKE_ANSWER_COMMENT, comment.getAnswerCommentId()+"");
                comment.setLikeState(rank == null ? "false" : "true");
                Long likedCount = jedis.zcard(comment.getAnswerCommentId() + RedisKey.LIKED_ANSWER_COMMENT);
                comment.setLikedCount(Integer.valueOf(likedCount+""));
            }

            answer.setAnswerCommentList(answerCommentList);

            Long rank = jedis.zrank(answer.getAnswerId() + RedisKey.LIKED_ANSWER, String.valueOf(userId));
            answer.setLikeState(rank == null ? "false" : "true");
            Long likedCount = jedis.zcard(answer.getAnswerId() + RedisKey.LIKED_ANSWER);
            answer.setLikedCount(Integer.valueOf(likedCount+""));

        }

        Map<Integer, String> topicMap =(Map<Integer, String>) JSON.parse(question.getTopicKvList());

        map.put("topicMap", topicMap);
        map.put("question", question);
        map.put("answerList", answerList);
        map.put("followedUserList", followedUserList);
        map.put("relatedQuestionList", relatedQuestionList);
        jedisPool.returnResource(jedis);
        return map;
    }

    public List<Question> listQuestionByPage(Integer curPage){
        curPage = curPage ==null? 1: curPage;
        int limit =3;
        int offset = (curPage -1 ) * limit;
        Jedis jedis = jedisPool.getResource();

        Set<String> idSet = jedis.zrange(RedisKey.QUESTION_SCANED_COUNT, offset, offset + limit - 1);
        List<Integer> idList = MyUtil.StringSetToIntegerList(idSet);
        System.out.println(idList);
        List<Question> questionList = new ArrayList<>();
        if (idList.size()>0){
            questionList=questionMapper.listQuestionByQuestionId(idList);

            for (Question question : questionList){
                question.setAnswerCount(answerMapper.selectAnswerCountByQuestionId(question.getQuestionId()));
                question.setFollowedCount(Integer.parseInt(jedis.zcard(question.getQuestionId()+RedisKey.FOLLOWED_QUESTION)+""));

            }
        }

        jedisPool.returnResource(jedis);
        return questionList;
    }
}
