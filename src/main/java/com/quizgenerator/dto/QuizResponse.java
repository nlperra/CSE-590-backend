package com.quizgenerator.dto;

import com.quizgenerator.model.Question;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class QuizResponse {
    private String id;
    private String standardCode;
    private String standardTitle;
    private List<Question> questions;
    private Integer difficulty;
    private Boolean adaptiveMode;
    private LocalDateTime createdAt = LocalDateTime.now();
    private Integer totalQuestions;
    private Integer estimatedTime;
    
    public void setQuestions(List<Question> questions) { 
        this.questions = questions; 
        this.totalQuestions = questions != null ? questions.size() : 0;
        this.estimatedTime = this.totalQuestions * 2;
    }
}