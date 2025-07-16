package com.quizgenerator.repository;

import com.quizgenerator.model.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {
    
    List<StudentAnswer> findByStudentId(String studentId);
    
    List<StudentAnswer> findByQuizId(String quizId);
    
    List<StudentAnswer> findByStudentIdAndQuizId(String studentId, String quizId);
    
    @Query("SELECT sa FROM StudentAnswer sa WHERE sa.studentId = :studentId ORDER BY sa.submittedAt DESC")
    List<StudentAnswer> findByStudentIdOrderBySubmittedAtDesc(@Param("studentId") String studentId);
    
    @Query("SELECT COUNT(sa) FROM StudentAnswer sa WHERE sa.studentId = :studentId AND sa.isCorrect = true")
    Long countCorrectAnswersByStudentId(@Param("studentId") String studentId);
    
    @Query("SELECT COUNT(sa) FROM StudentAnswer sa WHERE sa.studentId = :studentId")
    Long countTotalAnswersByStudentId(@Param("studentId") String studentId);
}