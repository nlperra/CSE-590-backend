package com.quizgenerator.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "student_profiles")
@Data
@NoArgsConstructor
public class StudentProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String studentId;
    
    @Column(nullable = false)
    private Integer totalQuestions = 0;
    
    @Column(nullable = false)
    private Integer correctAnswers = 0;
    
    @ElementCollection
    @CollectionTable(name = "student_skill_mastery", joinColumns = @JoinColumn(name = "student_profile_id"))
    @MapKeyColumn(name = "skill_name")
    @Column(name = "mastery_data", columnDefinition = "TEXT")
    private Map<String, String> subSkillMasteryJson = new HashMap<>();
    
    @ElementCollection
    @CollectionTable(name = "student_difficulty_performance", joinColumns = @JoinColumn(name = "student_profile_id"))
    @MapKeyColumn(name = "difficulty_level")
    @Column(name = "performance_data", columnDefinition = "TEXT")
    private Map<Integer, String> difficultyPerformanceJson = new HashMap<>();
    
    @ElementCollection
    @CollectionTable(name = "student_misconceptions", joinColumns = @JoinColumn(name = "student_profile_id"))
    @MapKeyColumn(name = "misconception")
    @Column(name = "count")
    private Map<String, Integer> misconceptions = new HashMap<>();
    
    @Column(nullable = false)
    private Double averageTime = 0.0;
    
    @Column(nullable = false)
    private LocalDateTime lastUpdated;
    
    public StudentProfile(String studentId) {
        this.studentId = studentId;
        this.lastUpdated = LocalDateTime.now();
    }
    
    @PrePersist
    private void prePersist() {
        if (this.lastUpdated == null) {
            this.lastUpdated = LocalDateTime.now();
        }
    }
    
    public double getAccuracy() {
        return totalQuestions > 0 ? (double) correctAnswers / totalQuestions : 0.0;
    }
}