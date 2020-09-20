package com.fc.service;

import com.fc.mapper.CollectionMapper;
import com.fc.model.Collection;
import com.fc.util.MyUtil;
import com.fc.util.RedisKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.awt.image.BandedSampleModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
@Service
public class CollectionService {

    @Autowired
    private CollectionMapper collectionMapper;

    @Autowired
    private JedisPool jedisPool;

    public List<Collection> listFollowingCollection (Integer userId){
        Jedis jedis = jedisPool.getResource();
        Set<String> idStr = jedis.zrange(userId + RedisKey.FOLLOW_COLLECTION, 0, -1);
        List<Integer> idList = MyUtil.StringSetToIntegerList(idStr);

        List<Collection> list = new ArrayList<>();
        if (idList.size()>0){
           list = collectionMapper.listCollectionByCollectionId(idList);
           for (Collection collection: list){
               Long answerCount = jedis.zcard(collection.getCollectionId() + RedisKey.COLLECT);
               collection.setAnswerCount(Integer.parseInt(answerCount + ""));
               System.out.println(answerCount);
           }
        }

        jedisPool.returnResource(jedis);
        return list;
    }

    public List<Collection> listCreatingCollection(Integer userId){
        List<Collection> list = collectionMapper.listCreatingCollectionByUserId(userId);
        Jedis jedis = jedisPool.getResource();
        for (Collection collection:list){
            Long answerCount = jedis.zcard(collection.getCollectionId() + RedisKey.COLLECT);
            collection.setAnswerCount(Integer.parseInt(answerCount+""));
            System.out.println(answerCount);
        }

        jedisPool.returnResource(jedis);
        return list;
    }

    public void addCollection(Collection collection, Integer userId){
        collection.setUserId(userId);
        collection.setCreateTime(System.currentTimeMillis());
        collection.setUpdateTime(System.currentTimeMillis());
        collectionMapper.insertCollection(collection);
    }
}
