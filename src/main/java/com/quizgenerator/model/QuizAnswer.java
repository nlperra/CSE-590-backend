package com.quizgenerator.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    @Column(name = "question_id", nullable = false)
    private String questionId;
    
    @Column(name = "student_answer", nullable = false)
    private Integer studentAnswer;
    
    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;
    
    @Column(name = "time_spent_ms", nullable = false)
    private Long timeSpentMs;
    
    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;
    
    public QuizAnswer(String sessionId, String questionId, Integer studentAnswer, 
                     Boolean isCorrect, Long timeSpentMs) {
        this.sessionId = sessionId;
        this.questionId = questionId;
        this.studentAnswer = studentAnswer;
        this.isCorrect = isCorrect;
        this.timeSpentMs = timeSpentMs;
        this.answeredAt = LocalDateTime.now();
    }
    
    @PrePersist
    private void prePersist() {
        if (this.answeredAt == null) {
            this.answeredAt = LocalDateTime.now();
        }
    }
}