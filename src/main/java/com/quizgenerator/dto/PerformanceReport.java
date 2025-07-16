package com.quizgenerator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class PerformanceReport {
    private String studentId;
    private Double overallAccuracy;
    private Integer totalQuestions;
    private Double averageTime;
    private LocalDateTime generatedAt;
    private List<SubSkillPerformance> subSkillBreakdown;
    private List<Recommendation> recommendations;
    private Map<String, Double> standardsProgress;
    private List<RecentSession> recentSessions;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubSkillPerformance {
        private String skill;
        private String masteryLevel;
        private Double accuracy;
        private Integer questionsAttempted;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recommendation {
        private String type;
        private String priority;
        private String message;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentSession {
        private String sessionId;
        private String standardCode;
        private LocalDateTime completedAt;
        private Double accuracy;
    }
}