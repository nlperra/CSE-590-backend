package com.quizgenerator.service;

import com.quizgenerator.dto.StudentAnalytics;
import com.quizgenerator.exception.StudentNotFoundException;
import com.quizgenerator.model.*;
import com.quizgenerator.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AnalyticsService {
    
    private final StudentRepository studentRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final StandardPerformanceRepository standardPerformanceRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final StandardRepository standardRepository;
    
    public StudentAnalytics getStudentAnalytics(String studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found: " + studentId));
        
        List<QuizSession> completedSessions = quizSessionRepository.findByStudentIdAndStatus(
                studentId, QuizSession.SessionStatus.COMPLETED);
        
        List<StandardPerformance> allPerformances = standardPerformanceRepository.findByStudentId(studentId);
        List<QuizAttempt> recentAttempts = quizAttemptRepository.findRecentAttemptsByStudentId(studentId, 10);
        
        return new StudentAnalytics(
                studentId,
                student.getName(),
                completedSessions.size(),
                completedSessions.stream().mapToInt(QuizSession::getTotalQuestions).sum(),
                calculateOverallAccuracy(completedSessions),
                calculateAverageQuizTime(completedSessions),
                getStrongStandards(allPerformances),
                getStrugglingStandards(allPerformances),
                mapRecentActivity(recentAttempts),
                calculateMasteryDistribution(allPerformances)
        );
    }
    
    private Double calculateOverallAccuracy(List<QuizSession> sessions) {
        if (sessions.isEmpty()) return 0.0;
        
        int totalQuestions = sessions.stream().mapToInt(QuizSession::getTotalQuestions).sum();
        int totalCorrect = sessions.stream().mapToInt(QuizSession::getCorrectAnswers).sum();
        
        return totalQuestions > 0 ? (double) totalCorrect / totalQuestions * 100 : 0.0;
    }
    
    private Long calculateAverageQuizTime(List<QuizSession> sessions) {
        if (sessions.isEmpty()) return 0L;
        
        return sessions.stream()
                .mapToLong(QuizSession::getTotalTimeMs)
                .reduce(0L, Long::sum) / sessions.size();
    }
    
    private List<StudentAnalytics.StandardPerformanceDto> getStrongStandards(List<StandardPerformance> performances) {
        return performances.stream()
                .filter(p -> p.getMasteryLevel() == StandardPerformance.MasteryLevel.PROFICIENT || 
                           p.getMasteryLevel() == StandardPerformance.MasteryLevel.MASTERED)
                .sorted((p1, p2) -> p2.getAccuracy().compareTo(p1.getAccuracy()))
                .limit(5)
                .map(this::mapToPerformanceDto)
                .collect(Collectors.toList());
    }
    
    private List<StudentAnalytics.StandardPerformanceDto> getStrugglingStandards(List<StandardPerformance> performances) {
        return performances.stream()
                .filter(p -> p.getMasteryLevel() == StandardPerformance.MasteryLevel.STRUGGLING || 
                           p.getMasteryLevel() == StandardPerformance.MasteryLevel.DEVELOPING)
                .sorted(Comparator.comparing(StandardPerformance::getAccuracy))
                .limit(5)
                .map(this::mapToPerformanceDto)
                .collect(Collectors.toList());
    }
    
    private StudentAnalytics.StandardPerformanceDto mapToPerformanceDto(StandardPerformance performance) {
        Optional<Standard> standard = standardRepository.findByCode(performance.getStandardCode());
        String title = standard.map(Standard::getTitle).orElse("Unknown Standard");
        
        Long averageTime = calculateAverageTimeForStandard(
                performance.getStudentId(), performance.getStandardCode());
        
        return new StudentAnalytics.StandardPerformanceDto(
                performance.getStandardCode(),
                title,
                performance.getTotalQuestions(),
                performance.getCorrectAnswers(),
                performance.getAccuracy().doubleValue(),
                averageTime,
                performance.getMasteryLevel().name().toLowerCase(),
                performance.getLastAttempted(),
                performance.getTrend().name().toLowerCase()
        );
    }
    
    private Long calculateAverageTimeForStandard(String studentId, String standardCode) {
        List<QuizAttempt> attempts = quizAttemptRepository.findRecentAttemptsByStudentIdAndStandardCode(
                studentId, standardCode, 5);
        
        if (attempts.isEmpty()) return 0L;
        
        return attempts.stream()
                .mapToLong(qa -> qa.getTimeSpentMs() != null ? qa.getTimeSpentMs() / qa.getQuestionsAnswered() : 0L)
                .reduce(0L, Long::sum) / attempts.size();
    }
    
    private List<StudentAnalytics.RecentActivityDto> mapRecentActivity(List<QuizAttempt> attempts) {
        return attempts.stream()
                .map(attempt -> new StudentAnalytics.RecentActivityDto(
                        attempt.getAttemptedAt(),
                        attempt.getStandardCode(),
                        attempt.getAccuracy() != null ? attempt.getAccuracy().doubleValue() : 0.0,
                        attempt.getQuestionsAnswered()
                ))
                .collect(Collectors.toList());
    }
    
    private StudentAnalytics.MasteryDistribution calculateMasteryDistribution(List<StandardPerformance> performances) {
        Map<StandardPerformance.MasteryLevel, Long> distribution = performances.stream()
                .collect(Collectors.groupingBy(
                        StandardPerformance::getMasteryLevel,
                        Collectors.counting()
                ));
        
        return new StudentAnalytics.MasteryDistribution(
                distribution.getOrDefault(StandardPerformance.MasteryLevel.STRUGGLING, 0L).intValue(),
                distribution.getOrDefault(StandardPerformance.MasteryLevel.DEVELOPING, 0L).intValue(),
                distribution.getOrDefault(StandardPerformance.MasteryLevel.PROFICIENT, 0L).intValue(),
                distribution.getOrDefault(StandardPerformance.MasteryLevel.MASTERED, 0L).intValue()
        );
    }
    
    public void updateStudentPerformance(String studentId, String sessionId) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        
        if (!session.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("Session does not belong to student: " + studentId);
        }
        
        if (session.getStandardCode() != null) {
            updateStandardPerformance(studentId, session.getStandardCode(), session);
            createQuizAttemptRecord(studentId, session);
        }
    }
    
    private void updateStandardPerformance(String studentId, String standardCode, QuizSession session) {
        StandardPerformance performance = standardPerformanceRepository
                .findByStudentIdAndStandardCode(studentId, standardCode)
                .orElse(new StandardPerformance(studentId, standardCode));
        
        performance.updatePerformance(session.getTotalQuestions(), session.getCorrectAnswers());
        performance.setTrend(calculateTrend(studentId, standardCode));
        
        standardPerformanceRepository.save(performance);
    }
    
    private void createQuizAttemptRecord(String studentId, QuizSession session) {
        QuizAttempt attempt = new QuizAttempt(
                studentId,
                session.getStandardCode(),
                session.getId(),
                session.getTotalQuestions(),
                session.getCorrectAnswers(),
                session.getTotalTimeMs()
        );
        
        quizAttemptRepository.save(attempt);
    }
    
    private StandardPerformance.Trend calculateTrend(String studentId, String standardCode) {
        List<QuizAttempt> recentAttempts = quizAttemptRepository
                .findRecentAttemptsByStudentIdAndStandardCode(studentId, standardCode, 5);
        
        if (recentAttempts.size() < 3) {
            return StandardPerformance.Trend.STABLE;
        }
        
        List<Double> accuracies = recentAttempts.stream()
                .map(qa -> qa.getAccuracy() != null ? qa.getAccuracy().doubleValue() : 0.0)
                .toList();
        
        double recentAvg = accuracies.subList(0, Math.min(2, accuracies.size())).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        double earlierAvg = accuracies.subList(Math.min(2, accuracies.size()), accuracies.size()).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        double improvement = recentAvg - earlierAvg;
        
        if (improvement > 10.0) return StandardPerformance.Trend.IMPROVING;
        if (improvement < -10.0) return StandardPerformance.Trend.DECLINING;
        return StandardPerformance.Trend.STABLE;
    }
}