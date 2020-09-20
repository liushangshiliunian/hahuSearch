package com.fc.mapper;

import com.fc.model.AnswerComment;
import com.fc.model.QuestionComment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CommentMapper {

    int selectAnswerCommentCountByAnswerId(@Param("answerId") Integer answerId);

    List<QuestionComment> listQuestionCommentByQuestionId(@Param("questionId")Integer questionId);

    List<AnswerComment> listAnswerCommentByAnswerId(@Param("answerId")Integer answerId);
}
