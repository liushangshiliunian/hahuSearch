package com.fc.service;

import com.fc.async.MailTask;
import com.fc.mapper.UserMapper;
import com.fc.model.User;
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
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
}
