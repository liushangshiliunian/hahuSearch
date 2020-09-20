package com.fc.mapper;

import com.fc.model.Question;
import com.google.gson.internal.$Gson$Preconditions;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface QuestionMapper {
    int selectQuestionCountByUserId(@Param("userId")Integer userId);

    List<Question> listQuestionByUserId(Map<String ,Object> map);

    List <Question> listQuestionByQuestionId (List<Integer> questionIdList);

    int selectQuestionCountByTopicId (@Param("topicId") Integer topicId);

    List<Integer> listQuestionIdByTopicId (Map<String , Object> map);

    Integer insertQuestion(Question question);

    void insertIntoQuestionTopic(@Param("questionId") Integer questionId, @Param("topicId")Integer topicId);

    Question selectQuestionByQuestionId(@Param("questionId") Integer questionId);

    List<Question> listRelatedQuestion(@Param("questionId") Integer questionId );
}
