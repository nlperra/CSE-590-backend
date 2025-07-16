package com.quizgenerator.repository;

import com.quizgenerator.model.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    
    List<QuizAnswer> findBySessionId(String sessionId);
    
    List<QuizAnswer> findBySessionIdOrderByAnsweredAt(String sessionId);
    
    @Query("SELECT qa FROM QuizAnswer qa WHERE qa.sessionId = :sessionId AND qa.questionId = :questionId")
    List<QuizAnswer> findBySessionIdAndQuestionId(@Param("sessionId") String sessionId, 
                                                  @Param("questionId") String questionId);
    
    @Query("SELECT COUNT(qa) FROM QuizAnswer qa WHERE qa.sessionId = :sessionId AND qa.isCorrect = true")
    Long countCorrectAnswersBySessionId(@Param("sessionId") String sessionId);
    
    @Query("SELECT COUNT(qa) FROM QuizAnswer qa WHERE qa.sessionId = :sessionId")
    Long countTotalAnswersBySessionId(@Param("sessionId") String sessionId);
    
    @Query("SELECT SUM(qa.timeSpentMs) FROM QuizAnswer qa WHERE qa.sessionId = :sessionId")
    Long sumTimeSpentBySessionId(@Param("sessionId") String sessionId);
}