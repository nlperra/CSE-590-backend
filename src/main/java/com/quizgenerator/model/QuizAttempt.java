package com.quizgenerator.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "quiz_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "student_id", nullable = false)
    private String studentId;
    
    @Column(name = "standard_code", length = 50)
    private String standardCode;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "questions_answered")
    private Integer questionsAnswered;
    
    @Column(name = "correct_answers")
    private Integer correctAnswers;
    
    @Column(name = "accuracy", precision = 5, scale = 2)
    private BigDecimal accuracy;
    
    @Column(name = "time_spent_ms")
    private Long timeSpentMs;
    
    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;
    
    public QuizAttempt(String studentId, String standardCode, String sessionId, 
                      Integer questionsAnswered, Integer correctAnswers, Long timeSpentMs) {
        this.studentId = studentId;
        this.standardCode = standardCode;
        this.sessionId = sessionId;
        this.questionsAnswered = questionsAnswered;
        this.correctAnswers = correctAnswers;
        this.timeSpentMs = timeSpentMs;
        this.attemptedAt = LocalDateTime.now();
        
        if (questionsAnswered != null && questionsAnswered > 0 && correctAnswers != null) {
            this.accuracy = BigDecimal.valueOf((double) correctAnswers / questionsAnswered * 100)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        } else {
            this.accuracy = BigDecimal.ZERO;
        }
    }
    
    @PrePersist
    private void prePersist() {
        if (this.attemptedAt == null) {
            this.attemptedAt = LocalDateTime.now();
        }
    }
    
    public double getAccuracyValue() {
        return accuracy != null ? accuracy.doubleValue() : 0.0;
    }
}