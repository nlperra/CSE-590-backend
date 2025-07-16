package com.quizgenerator.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizHistoryResponse {
    private String studentId;
    private Integer totalQuizzes;
    private List<QuizDetails> quizzes;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizDetails {
        private String sessionId;
        private String quizId;
        private String standardCode;
        private String standardTitle;
        private Double accuracy;
        private Integer totalQuestions;
        private Integer correctAnswers;
        private Long completionTimeMs;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String masteryLevel;
    }
}