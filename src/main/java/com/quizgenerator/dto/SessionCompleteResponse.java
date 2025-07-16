package com.quizgenerator.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionCompleteResponse {
    private String sessionId;
    private QuizResults results;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizResults {
        private Integer totalQuestions;
        private Integer correctAnswers;
        private Double accuracy;
        private Long totalTime;
    }
}