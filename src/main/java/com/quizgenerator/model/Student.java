package com.quizgenerator.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    @Id
    private String id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "email", unique = true)
    private String email;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Column(name = "total_quizzes_completed", nullable = false)
    private Integer totalQuizzesCompleted = 0;
    
    @Column(name = "total_questions_answered", nullable = false)
    private Integer totalQuestionsAnswered = 0;
    
    @Column(name = "overall_accuracy", precision = 5, scale = 2, nullable = false)
    private BigDecimal overallAccuracy = BigDecimal.ZERO;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    public Student(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.lastLogin = LocalDateTime.now();
        this.totalQuizzesCompleted = 0;
        this.totalQuestionsAnswered = 0;
        this.overallAccuracy = BigDecimal.ZERO;
        this.isActive = true;
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
        if (this.isActive == null) {
            this.isActive = true;
        }
    }
    
    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateLogin() {
        this.lastLogin = LocalDateTime.now();
    }
    
    public void incrementQuizCompleted() {
        this.totalQuizzesCompleted++;
    }
    
    public void addQuestionsAnswered(int count) {
        this.totalQuestionsAnswered += count;
    }
    
    public void updateAccuracy(BigDecimal newAccuracy) {
        this.overallAccuracy = newAccuracy;
    }
}