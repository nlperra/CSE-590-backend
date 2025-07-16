package com.quizgenerator.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_choices", joinColumns = @JoinColumn(name = "question_id"))
    @MapKeyColumn(name = "choice_key")
    @Column(name = "choice_value", columnDefinition = "TEXT")
    private Map<String, String> choices;
    
    @Column(nullable = false)
    private String correctAnswer;
    
    @Column(columnDefinition = "TEXT")
    private String explanation;
    
    @Column(nullable = false)
    private Integer difficulty;
    
    @Column(nullable = false)
    private String subSkill;
    
    @Column(nullable = false)
    private String standardCode;
    
    @Column(nullable = false)
    private String type;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_misconceptions", joinColumns = @JoinColumn(name = "question_id"))
    @MapKeyColumn(name = "choice_key")
    @Column(name = "misconception", columnDefinition = "TEXT")
    private Map<String, String> misconceptions;
    
    @Column(nullable = false)
    private LocalDateTime generatedAt;
    
    @Column(nullable = false)
    private Boolean isFallback = false;
    
    @Column(nullable = false)
    private Boolean isTargeted = false;
    
    @Column
    private String focusArea;
    
    public Question(String question, Map<String, String> choices, String correctAnswer,
                   String explanation, Integer difficulty, String subSkill,
                   String standardCode, String type, Map<String, String> misconceptions) {
        this.question = question;
        this.choices = choices;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.difficulty = difficulty;
        this.subSkill = subSkill;
        this.standardCode = standardCode;
        this.type = type;
        this.misconceptions = misconceptions;
        this.generatedAt = LocalDateTime.now();
        this.isFallback = false;
    }
    
    @PrePersist
    private void prePersist() {
        if (this.generatedAt == null) {
            this.generatedAt = LocalDateTime.now();
        }
    }
}