package com.quizgenerator.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "standard_performance", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "standard_code"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StandardPerformance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "student_id", nullable = false)
    private String studentId;
    
    @Column(name = "standard_code", nullable = false, length = 50)
    private String standardCode;
    
    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions = 0;
    
    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswers = 0;
    
    @Column(name = "accuracy", precision = 5, scale = 2, nullable = false)
    private BigDecimal accuracy = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "mastery_level", nullable = false)
    private MasteryLevel masteryLevel = MasteryLevel.DEVELOPING;
    
    @Column(name = "last_attempted", nullable = false)
    private LocalDateTime lastAttempted;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "trend", nullable = false)
    private Trend trend = Trend.STABLE;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum MasteryLevel {
        STRUGGLING,
        DEVELOPING,
        PROFICIENT,
        MASTERED
    }
    
    public enum Trend {
        IMPROVING,
        DECLINING, 
        STABLE
    }
    
    public StandardPerformance(String studentId, String standardCode) {
        this.studentId = studentId;
        this.standardCode = standardCode;
        this.totalQuestions = 0;
        this.correctAnswers = 0;
        this.accuracy = BigDecimal.ZERO;
        this.masteryLevel = MasteryLevel.DEVELOPING;
        this.trend = Trend.STABLE;
        this.lastAttempted = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.lastAttempted == null) {
            this.lastAttempted = now;
        }
    }
    
    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updatePerformance(int questionsAnswered, int correctCount) {
        this.totalQuestions += questionsAnswered;
        this.correctAnswers += correctCount;
        
        if (this.totalQuestions > 0) {
            this.accuracy = BigDecimal.valueOf((double) this.correctAnswers / this.totalQuestions * 100)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        }
        
        this.masteryLevel = calculateMasteryLevel();
        this.lastAttempted = LocalDateTime.now();
    }
    
    private MasteryLevel calculateMasteryLevel() {
        if (totalQuestions < 5) return MasteryLevel.DEVELOPING;
        
        double accuracyValue = accuracy.doubleValue();
        if (accuracyValue >= 90.0) return MasteryLevel.MASTERED;
        if (accuracyValue >= 80.0) return MasteryLevel.PROFICIENT;
        if (accuracyValue >= 60.0) return MasteryLevel.DEVELOPING;
        return MasteryLevel.STRUGGLING;
    }
    
    public double getAccuracyValue() {
        return accuracy.doubleValue();
    }
}