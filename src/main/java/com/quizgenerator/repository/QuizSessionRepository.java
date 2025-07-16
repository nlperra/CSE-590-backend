package com.quizgenerator.repository;

import com.quizgenerator.model.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizSessionRepository extends JpaRepository<QuizSession, String> {
    
    List<QuizSession> findByStudentId(String studentId);
    
    List<QuizSession> findByStudentIdAndStatus(String studentId, QuizSession.SessionStatus status);
    
    @Query("SELECT qs FROM QuizSession qs WHERE qs.studentId = :studentId ORDER BY qs.startedAt DESC")
    List<QuizSession> findByStudentIdOrderByStartedAtDesc(@Param("studentId") String studentId);
    
    @Query("SELECT qs FROM QuizSession qs WHERE qs.studentId = :studentId AND qs.status = 'COMPLETED' ORDER BY qs.completedAt DESC")
    List<QuizSession> findCompletedSessionsByStudentId(@Param("studentId") String studentId);
    
    List<QuizSession> findByStudentIdAndStatusOrderByCompletedAtDesc(String studentId, QuizSession.SessionStatus status);
    
    Optional<QuizSession> findByIdAndStudentId(String sessionId, String studentId);
}