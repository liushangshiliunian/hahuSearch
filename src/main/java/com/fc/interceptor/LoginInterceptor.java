package com.fc.interceptor;


import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


/**
 * @author me
 */
public class LoginInterceptor extends HandlerInterceptorAdapter {
    @Autowired
    private JedisPool jedisPool;

    private List<String> excludedUrls;

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public List<String> getExcludedUrls() {
        return excludedUrls;
    }

    public void setExcludedUrls(List<String> excludedUrls) {
        this.excludedUrls = excludedUrls;
    }

    public LoginInterceptor() {
        super();
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            String requestURI = request.getRequestURI();

            for (String s: excludedUrls){
                if (requestURI.endsWith(s)){
                    return true;
                }
            }
            String loginToken=null;
            Cookie[] cookies= request.getCookies();
            if (ArrayUtils.isEmpty(cookies)){
                request.getRequestDispatcher("toLogin").forward(request,response);
                return false;
            }else{
                for (Cookie cookie : cookies){
                    if (cookie.getName().equals("loginToken")){
                        loginToken = cookie.getValue();
                        break;
                    }
                }
            }

            if (StringUtils.isEmpty(loginToken)){
                request.getRequestDispatcher("toLogin").forward(request,response);
                return false;
            }

            Jedis jedis = jedisPool.getResource();
            String userId = jedis.get(loginToken);

            if (StringUtils.isEmpty(userId)){
                request.getRequestDispatcher("toLogin").forward(request,response);
                return false;
            }
            return true;
        } catch (ServletException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return true;
    }
}
