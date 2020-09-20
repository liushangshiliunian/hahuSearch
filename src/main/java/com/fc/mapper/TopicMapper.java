package com.fc.mapper;

import com.fc.model.Topic;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TopicMapper {
    List<Topic> listTopicByTopicName(@Param("topicName") String topicName);

    List<Topic> listHotTopic();

    List<Topic> listRootTopic();

    Topic selectTopicByTopicId(@Param("topicId") Integer topicId);

   List<Integer> selectQuestionIdByTopicId(@Param("topicId") Integer topicId);

    List<Topic> listTopicByParentId(@Param("parentTopicId") Integer parentTopicId);

     Integer selectTopicIdByTopicName(@Param("topicName") String topicName);

    Integer insertTopic(Topic topic);

    List<Topic> listTopicByTopicId(List<Integer> idList);
}
