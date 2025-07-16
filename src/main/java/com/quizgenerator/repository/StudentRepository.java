package com.quizgenerator.repository;

import com.quizgenerator.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
    
    Optional<Student> findById(String studentId);
    
    @Query("SELECT s FROM Student s WHERE s.id = :studentId")
    Optional<Student> findByStudentId(@Param("studentId") String studentId);
    
    boolean existsById(String studentId);
}