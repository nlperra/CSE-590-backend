package com.quizgenerator.repository;

import com.quizgenerator.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    
    List<QuizAttempt> findByStudentId(String studentId);
    
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.studentId = :studentId ORDER BY qa.attemptedAt DESC")
    List<QuizAttempt> findByStudentIdOrderByAttemptedAtDesc(@Param("studentId") String studentId);
    
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.studentId = :studentId ORDER BY qa.attemptedAt DESC LIMIT :limit")
    List<QuizAttempt> findRecentAttemptsByStudentId(@Param("studentId") String studentId, @Param("limit") int limit);
    
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.studentId = :studentId AND qa.standardCode = :standardCode ORDER BY qa.attemptedAt DESC LIMIT :limit")
    List<QuizAttempt> findRecentAttemptsByStudentIdAndStandardCode(@Param("studentId") String studentId, 
                                                                  @Param("standardCode") String standardCode, 
                                                                  @Param("limit") int limit);
    
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.attemptedAt < :cutoffDate")
    List<QuizAttempt> findAttemptsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    List<QuizAttempt> findBySessionId(String sessionId);
}