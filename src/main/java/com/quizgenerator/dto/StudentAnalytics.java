package com.quizgenerator.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentAnalytics {
    private String studentId;
    private String name;
    private Integer totalQuizzesTaken;
    private Integer totalQuestionsAnswered;
    private Double overallAccuracy;
    private Long averageQuizTime;
    private List<StandardPerformanceDto> strongStandards;
    private List<StandardPerformanceDto> strugglingStandards;
    private List<RecentActivityDto> recentActivity;
    private MasteryDistribution masteryDistribution;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StandardPerformanceDto {
        private String standardCode;
        private String standardTitle;
        private Integer totalQuestions;
        private Integer correctAnswers;
        private Double accuracy;
        private Long averageTime;
        private String masteryLevel;
        private LocalDateTime lastAttempted;
        private String trend;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityDto {
        private LocalDateTime date;
        private String standardCode;
        private Double accuracy;
        private Integer questionsAnswered;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MasteryDistribution {
        private Integer struggling;
        private Integer developing;
        private Integer proficient;
        private Integer mastered;
    }
}