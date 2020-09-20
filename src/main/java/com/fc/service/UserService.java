package com.fc.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fc.async.MailTask;
import com.fc.model.Answer;
import com.fc.mapper.AnswerMapper;
import com.fc.mapper.CommentMapper;
import com.fc.mapper.UserMapper;
import com.fc.model.User;
import com.fc.util.HttpUtils;
import com.fc.util.MyConstant;
import com.fc.util.MyUtil;
import com.fc.util.RedisKey;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UserService {
    private static final String WEIBO_APP_KEY = "968565512";
    private static final String WEIBO_APP_SECRET = "2bba0b19d588f1b65a4b4e348dcd45b6";
    private static final String REDIRECT_URL = "http://naivee.me/weiboLogin";

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AnswerMapper answerMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private JedisPool jedisPool;



    public Map<String,String> register (String username,String email, String password){
        Map<String ,String >map = new HashMap<>();
        Pattern p = Pattern.compile("^([a-zA-Z0-9_-])+@([a-zA-Z0-9_-])+((\\.[a-zA-Z0-9_-]{2,3}){1,2})$");
        Matcher m = p.matcher(email);
        if (!m.matches()){
            map.put("regi-email-error","请输入正确的邮箱");
            return map;
        }
        if (StringUtils.isEmpty(username) || username.length()>10){
            map.put("regi-username-error","用户名长度须在1-10个字符");
            return map;
        }

        p=Pattern.compile("^\\w{6,20}$");
        m = p.matcher(password);
        if (!m.matches()){
            map.put("regi-password-error","密码长度须在6-20个字符");
            return map;
        }

        try {
            int emailCount = userMapper.selectEmailCount(email);
            if (emailCount > 0){
                map.put("regi-email-error","该邮箱已注册");
                return map;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(MyUtil.md5(password));

        String activateCode = MyUtil.createRandomCode();
        user.setActivationCode(activateCode);
        user.setJoinTime(System.currentTimeMillis());

        user.setUsername(username);
        user.setAvatarUrl(MyConstant.QINIU_IMAGE_URL+"head.jpg");

        taskExecutor.execute(new MailTask(activateCode, user.getEmail(),javaMailSender, 1));

        userMapper.insertUser(user);

        Jedis jedis = jedisPool.getResource();
        jedis.zadd(user.getUserId() + RedisKey.FOLLOW_PEOPLE, new Date().getTime(), String.valueOf(3));
        jedis.zadd(3 + RedisKey.FOLLOWED_PEOPLE, new Date().getTime(), String.valueOf(user.getUserId()));
        jedis.zadd(user.getUserId() + RedisKey.FOLLOW_PEOPLE, new Date().getTime(), String.valueOf(4));
        jedis.zadd(4 + RedisKey.FOLLOWED_PEOPLE, new Date().getTime(), String.valueOf(user.getUserId()));
        jedisPool.returnResource(jedis);
        map.put("ok", "注册完成");
        return map;
    }

    public Map<String, Object> login(String eamil, String password, HttpServletResponse response){
        Map<String, Object> map= new HashMap<>();

        Integer userId = userMapper.selectUserIdByEmailAndPassword(eamil, MyUtil.md5(password));
        if (userId==null){
            map.put("error","用户名或密码错误");
            return map;
        }
        Integer activationState = userMapper.selectActivationStateByUserId(userId);
        if (activationState != 1){
            map.put("error", "您的帐号还没有激活");
            return map;
        }

        String loginToken = MyUtil.createRandomCode();
        Cookie cookie = new Cookie("loginToken", loginToken);
        cookie.setPath("/");
        cookie.setMaxAge(60*60*24*30);
        response.addCookie(cookie);

        Jedis jedis = jedisPool.getResource();
        jedis.set(loginToken,userId.toString(),"NX","EX",60 * 60 * 24 * 30);
        jedisPool.returnResource(jedis);

        User user = userMapper.selectUserInfoByUserId(userId);
        user.setUserId(userId);
        map.put("userInfo",user);

        return map;
    }

    public void weiboLogin(String code, HttpServletResponse response)throws IOException{

        Map<String, String> map= new HashMap<>();
        map.put("client_id",WEIBO_APP_KEY);
        map.put("client_secret",WEIBO_APP_SECRET);
        map.put("grant_type","authorization_code");
        map.put("code",code);
        map.put("redirect_url",REDIRECT_URL);

        String result = HttpUtils.send("https://api.weibo.com/oauth2/access_token", map, "utf8");
        JSONObject jsonObject = JSON.parseObject(result);
        String weibouserId = jsonObject.getString("uid");

        User user = userMapper.selectUserInfoByWeiboUserId(weibouserId);
        User temp = user;
        if (user== null || user.getUserId() == null){
            String accessToken = jsonObject.getString("access_token");

            String userStr = HttpUtils.get("https://api.weibo.com/2/users/show.json?access_token=" + accessToken + "&uid=" + weibouserId);
            JSONObject userInfo = JSON.parseObject(userStr);
            if (userInfo.get("error_code")!=null){
                throw new RuntimeException("审核还未通过~");
            }

            String username = userInfo.getString("name");
            String avatar = userInfo.getString("profile_image_url");

            temp = new User();
            temp.setUsername(username);
            temp.setAvatarUrl(avatar);
            temp.setWeiboUserId(weibouserId);

            userMapper.insertWeiboUser(temp);
        }

        String loginToken = MyUtil.createRandomCode();
        Cookie cookie = new Cookie("loginToken",loginToken);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 30);
        response.addCookie(cookie);

        Jedis jedis = jedisPool.getResource();
        jedis.set(loginToken, temp.getUserId().toString(), "NX","EX",60 * 60 * 24 * 30);
    }

    public void activate(String activationCode){
        userMapper.updateActivationStateByActivationCode(activationCode);
    }

    public String logout(HttpServletRequest request, HttpServletResponse response){
        String loginToken = null;
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie:cookies){
            if (cookie.getName().equals("loginToken")){
                loginToken = cookie.getValue();
                Jedis jedis = jedisPool.getResource();
                jedis.del(loginToken);
                jedisPool.returnResource(jedis);
                break;
            }
        }

        Cookie cookie = new Cookie("loginToken", "");
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 30);
        response.addCookie(cookie);

        return loginToken;
    }

    public Map<String, Object> profile(Integer userId, Integer localUserId){
        HashMap<String, Object> map = new HashMap<>();
        User user = userMapper.selectProfileInfoByUserId(userId);

        if (userId.equals(localUserId)){
            map.put("isSelf",true);
        }else {
            map.put("isSelf",false);
            System.out.println(false);
        }

        Jedis jedis = jedisPool.getResource();
        Long followCount = jedis.zcard(userId + RedisKey.FOLLOW_PEOPLE);
        Long followedCount = jedis.zcard(userId + RedisKey.FOLLOWED_PEOPLE);
        Long followTopicCount = jedis.zcard(userId + RedisKey.FOLLOW_TOPIC);
        Long followQuestionCount = jedis.zcard(userId + RedisKey.FOLLOW_QUESTION);
        Long followCollectionCount = jedis.zcard(userId + RedisKey.FOLLOW_COLLECTION);
        user.setFollowCount(Integer.parseInt(followCount + ""));
        user.setFollowedCount(Integer.parseInt(followedCount + ""));
        user.setFollowTopicCount(Integer.parseInt(followTopicCount + ""));
        user.setFollowQuestionCount(Integer.parseInt(followQuestionCount + ""));
        user.setFollowCollectionCount(Integer.parseInt(followCollectionCount+ ""));

        map.put("user", user);
        return map;
    }

    public  boolean judgePeopleFollowUser(Integer localUserId, Integer userId){
        Jedis jedis = jedisPool.getResource();
        Long rank = jedis.zrank(localUserId + RedisKey.FOLLOW_PEOPLE, String.valueOf(userId));
        jedisPool.returnResource(jedis);

        return  rank == null ? false: true;
    }

    public Integer getUserIdFromRedis(HttpServletRequest request) {
        String loginToken = null;
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie:cookies){
            if (cookie.getName().equals("loginToken")){
                loginToken = cookie.getValue();
                break;
            }
        }

        Jedis jedis = jedisPool.getResource();
        String userId = jedis.get(loginToken);

        return Integer.parseInt(userId);

    }

    public Map<String , Object> getIndexDetail(Integer userId, Integer curPage) {
        HashMap<String, Object> map = new HashMap<>();
        Jedis jedis = jedisPool.getResource();

        Set<String> idStr = jedis.zrange(userId + RedisKey.FOLLOW_PEOPLE, 0, -1);
        List<Integer> idList = MyUtil.StringSetToIntegerList(idStr);
        List<Answer> answerList = new ArrayList<>();
        if (idList.size()>0){
            answerList = _getIndexDetail(idList,curPage);
            for (Answer answer: answerList) {
                Long rank = jedis.zrank(answer.getAnswerId() + RedisKey.LIKED_ANSWER, String.valueOf(userId));
                System.out.println("rank:" + rank);
                answer.setLikeState(rank == null ? "false" : "true");
            }
        }

        map.put("answerList",answerList);
        jedisPool.returnResource(jedis);
        return map;
    }

    public List<Answer> _getIndexDetail(List<Integer> idList, Integer curPage){
        curPage = curPage==null ? 1 : curPage;
        int limit = 8;
        int offset = (curPage - 1) * limit;
        HashMap<String, Object> map = new HashMap<>();
        map.put("offset", offset);
        map.put("limit", limit);
        map.put("userIdList", idList);
        List<Answer> answerList = answerMapper.listAnswerByUserIdList(map);
        for (Answer answer:answerList){
            int commentCount = commentMapper.selectAnswerCommentCountByAnswerId(answer.getAnswerId());
            answer.setCommentCount(commentCount);
        }
        return answerList;
    }

    public User getProfileInfo(Integer userId){
        User user = userMapper.selectProfileInfoByUserId(userId);
        return user;
    }

    public void updateProfile(User user){
        userMapper.updateProfile(user);
    }

    public List<User> listFollowingUser(Integer userId){
        Jedis jedis = jedisPool.getResource();
        Set<String> idSet = jedis.zrange(userId + RedisKey.FOLLOW_PEOPLE, 0, -1);
        List<Integer> idList = MyUtil.StringSetToIntegerList(idSet);
        List<User> list = new ArrayList<>();
        if (idList.size()>0){
           list = userMapper.listUserInfoByUserId(idList);
        }
        jedisPool.returnResource(jedis);
        return list;
    }
}