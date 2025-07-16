package com.quizgenerator.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "quiz_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizSession {
    @Id
    private String id;
    
    @Column(name = "student_id", nullable = false)
    private String studentId;
    
    @Column(name = "quiz_id")
    private String quizId;
    
    @Column(name = "standard_code")
    private String standardCode;
    
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions = 0;
    
    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswers = 0;
    
    @Column(name = "total_time_ms", nullable = false)
    private Long totalTimeMs = 0L;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SessionStatus status = SessionStatus.ACTIVE;
    
    @Column(name = "accuracy", precision = 5, scale = 2)
    private BigDecimal accuracy = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "mastery_level")
    private MasteryLevel masteryLevel = MasteryLevel.DEVELOPING;
    
    public enum SessionStatus {
        ACTIVE, COMPLETED
    }
    
    public enum MasteryLevel {
        STRUGGLING, DEVELOPING, PROFICIENT, MASTERED
    }
    
    public QuizSession(String id, String studentId, String quizId, String standardCode) {
        this.id = id;
        this.studentId = studentId;
        this.quizId = quizId;
        this.standardCode = standardCode;
        this.startedAt = LocalDateTime.now();
        this.status = SessionStatus.ACTIVE;
        this.totalQuestions = 0;
        this.correctAnswers = 0;
        this.totalTimeMs = 0L;
    }
    
    @PrePersist
    private void prePersist() {
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = SessionStatus.ACTIVE;
        }
    }
    
    public void addAnswer(boolean isCorrect, long timeSpentMs) {
        this.totalQuestions++;
        if (isCorrect) {
            this.correctAnswers++;
        }
        this.totalTimeMs += timeSpentMs;
        
        if (this.totalQuestions > 0) {
            this.accuracy = BigDecimal.valueOf((double) this.correctAnswers / this.totalQuestions * 100)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        }
        
        this.masteryLevel = calculateMasteryLevel();
    }
    
    private MasteryLevel calculateMasteryLevel() {
        if (totalQuestions < 5) return MasteryLevel.DEVELOPING;
        
        double accuracyValue = accuracy.doubleValue();
        if (accuracyValue >= 90.0) return MasteryLevel.MASTERED;
        if (accuracyValue >= 80.0) return MasteryLevel.PROFICIENT;
        if (accuracyValue >= 60.0) return MasteryLevel.DEVELOPING;
        return MasteryLevel.STRUGGLING;
    }
    
    public void completeSession() {
        this.status = SessionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
    
    public double getAccuracy() {
        return totalQuestions > 0 ? (double) correctAnswers / totalQuestions : 0.0;
    }
}