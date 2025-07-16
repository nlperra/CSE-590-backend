package com.quizgenerator.repository;

import com.quizgenerator.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    List<Question> findByStandardCode(String standardCode);
    
    List<Question> findByStandardCodeAndDifficulty(String standardCode, Integer difficulty);
    
    List<Question> findByStandardCodeAndType(String standardCode, String type);
    
    List<Question> findBySubSkill(String subSkill);
    
    @Query("SELECT q FROM Question q WHERE q.standardCode = :standardCode AND q.difficulty BETWEEN :minDifficulty AND :maxDifficulty")
    List<Question> findByStandardCodeAndDifficultyRange(@Param("standardCode") String standardCode, 
                                                       @Param("minDifficulty") Integer minDifficulty,
                                                       @Param("maxDifficulty") Integer maxDifficulty);
    
    @Query("SELECT q FROM Question q WHERE q.standardCode = :standardCode AND q.type = :type ORDER BY q.generatedAt DESC")
    List<Question> findRecentQuestionsByStandardAndType(@Param("standardCode") String standardCode, 
                                                       @Param("type") String type);
}