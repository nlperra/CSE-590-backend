package com.quizgenerator.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String studentId;
    
    @Column(nullable = false)
    private String questionId;
    
    @Column(nullable = false)
    private String quizId;
    
    @Column(nullable = false)
    private Integer answer;
    
    @Column(nullable = false)
    private Boolean isCorrect;
    
    @Column(nullable = false)
    private Long timeSpent;
    
    @Column(nullable = false)
    private LocalDateTime submittedAt;
    
    @PrePersist
    private void prePersist() {
        if (this.submittedAt == null) {
            this.submittedAt = LocalDateTime.now();
        }
    }
    
    public StudentAnswer(String studentId, String questionId, String quizId, 
                        Integer answer, Boolean isCorrect, Long timeSpent) {
        this.studentId = studentId;
        this.questionId = questionId;
        this.quizId = quizId;
        this.answer = answer;
        this.isCorrect = isCorrect;
        this.timeSpent = timeSpent;
        this.submittedAt = LocalDateTime.now();
    }
}