package com.quizgenerator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizgenerator.dto.AnswerSubmissionRequest;
import com.quizgenerator.dto.AnswerSubmissionResponse;
import com.quizgenerator.dto.PerformanceReport;
import com.quizgenerator.dto.StudentRegistrationRequest;
import com.quizgenerator.dto.StudentRegistrationResponse;
import com.quizgenerator.model.Question;
import com.quizgenerator.model.StudentProfile;
import com.quizgenerator.repository.QuestionRepository;
import com.quizgenerator.repository.StudentProfileRepository;
import com.quizgenerator.repository.StudentAnswerRepository;
import com.quizgenerator.repository.StudentRepository;
import com.quizgenerator.repository.QuizSessionRepository;
import com.quizgenerator.model.StudentAnswer;
import com.quizgenerator.model.Student;
import com.quizgenerator.model.QuizSession;
import com.quizgenerator.exception.StudentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class AdaptiveService {

    private final StudentProfileRepository studentProfileRepository;
    private final QuestionRepository questionRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final StudentRepository studentRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizSessionService quizSessionService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public AnswerSubmissionResponse submitAnswer(AnswerSubmissionRequest request) {
        if (!quizSessionService.isValidSession(request.getSessionId(), request.getStudentId())) {
            AnswerSubmissionResponse response = new AnswerSubmissionResponse();
            response.setSessionValid(false);
            response.setCorrect(false);
            response.setExplanation("Session is invalid or expired");
            return response;
        }
        
        long questionId;
        try {
            questionId = Long.parseLong(request.getQuestionId());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid question ID format");
        }
        
        Optional<Question> questionOpt = questionRepository.findById(questionId);
        if (questionOpt.isEmpty()) {
            throw new RuntimeException("Question not found");
        }
        
        Question question = questionOpt.get();
        
        String answerKey = convertAnswerIndexToKey(request.getAnswer());
        boolean isCorrect = answerKey.equals(question.getCorrectAnswer());
        
        quizSessionService.recordAnswer(
            request.getSessionId(),
            request.getQuestionId(), 
            request.getAnswer(),
            isCorrect,
            request.getTimeSpent()
        );
        
        StudentAnswer studentAnswer = new StudentAnswer(
            request.getStudentId(),
            request.getQuestionId(),
            request.getQuizId(),
            request.getAnswer(),
            isCorrect,
            request.getTimeSpent()
        );
        studentAnswerRepository.save(studentAnswer);
        
        StudentProfile profile = getOrCreateStudentProfile(request.getStudentId());
        updateStudentProfile(profile, question, answerKey, request.getTimeSpent(), isCorrect);
        
        AnswerSubmissionResponse response = new AnswerSubmissionResponse();
        response.setCorrect(isCorrect);
        response.setExplanation(question.getExplanation());
        response.setSessionValid(true);
        
        response.setNextQuestionId(null);
        
        return response;
    }
    
    private StudentProfile getOrCreateStudentProfile(String studentId) {
        return studentProfileRepository.findByStudentId(studentId)
            .orElseGet(() -> {
                StudentProfile profile = new StudentProfile(studentId);
                return studentProfileRepository.save(profile);
            });
    }
    
    private void updateStudentProfile(StudentProfile profile, Question question, String answer, 
                                     Long timeSpent, boolean isCorrect) {
        profile.setTotalQuestions(profile.getTotalQuestions() + 1);
        
        if (isCorrect) {
            profile.setCorrectAnswers(profile.getCorrectAnswers() + 1);
        }
        
        updateSubSkillMastery(profile, question.getSubSkill(), isCorrect);
        
        updateDifficultyPerformance(profile, question.getDifficulty(), isCorrect);
        
        if (!isCorrect && question.getMisconceptions() != null) {
            String misconception = question.getMisconceptions().get(answer);
            if (misconception != null) {
                profile.getMisconceptions().merge(misconception, 1, Integer::sum);
            }
        }
        
        updateAverageTime(profile, timeSpent);
        
        profile.setLastUpdated(LocalDateTime.now());
        studentProfileRepository.save(profile);
    }
    
    private void updateSubSkillMastery(StudentProfile profile, String subSkill, boolean isCorrect) {
        try {
            Map<String, String> masteryData = profile.getSubSkillMasteryJson();
            
            Map<String, Object> skillData;
            if (masteryData.containsKey(subSkill)) {
                skillData = objectMapper.readValue(masteryData.get(subSkill), new TypeReference<Map<String, Object>>() {});
            } else {
                skillData = new HashMap<>();
                skillData.put("attempted", 0);
                skillData.put("correct", 0);
                skillData.put("recentPerformance", new ArrayList<Boolean>());
            }
            
            skillData.put("attempted", ((Integer) skillData.get("attempted")) + 1);
            
            if (isCorrect) {
                skillData.put("correct", ((Integer) skillData.get("correct")) + 1);
            }
            
            List<Boolean> recentPerformance = (List<Boolean>) skillData.get("recentPerformance");
            recentPerformance.add(isCorrect);
            if (recentPerformance.size() > 5) {
                recentPerformance.remove(0);
            }
            
            skillData.put("masteryLevel", calculateMasteryLevel(skillData));
            
            masteryData.put(subSkill, objectMapper.writeValueAsString(skillData));
            
        } catch (JsonProcessingException e) {
            log.error("Error updating sub-skill mastery: {}", e.getMessage());
        }
    }
    
    private void updateDifficultyPerformance(StudentProfile profile, Integer difficulty, boolean isCorrect) {
        try {
            Map<Integer, String> difficultyData = profile.getDifficultyPerformanceJson();
            
            Map<String, Object> performanceData;
            if (difficultyData.containsKey(difficulty)) {
                performanceData = objectMapper.readValue(difficultyData.get(difficulty), new TypeReference<Map<String, Object>>() {});
            } else {
                performanceData = new HashMap<>();
                performanceData.put("attempted", 0);
                performanceData.put("correct", 0);
            }
            
            performanceData.put("attempted", ((Integer) performanceData.get("attempted")) + 1);
            
            if (isCorrect) {
                performanceData.put("correct", ((Integer) performanceData.get("correct")) + 1);
            }
            
            difficultyData.put(difficulty, objectMapper.writeValueAsString(performanceData));
            
        } catch (JsonProcessingException e) {
            log.error("Error updating difficulty performance: {}", e.getMessage());
        }
    }
    
    private void updateAverageTime(StudentProfile profile, Long timeSpent) {
        if (timeSpent != null && timeSpent > 0) {
            double currentAverage = profile.getAverageTime();
            int totalQuestions = profile.getTotalQuestions();
            
            if (totalQuestions == 1) {
                profile.setAverageTime(timeSpent.doubleValue());
            } else {
                double newAverage = ((currentAverage * (totalQuestions - 1)) + timeSpent) / totalQuestions;
                profile.setAverageTime(newAverage);
            }
        }
    }
    
    private String calculateMasteryLevel(Map<String, Object> skillData) {
        int attempted = (Integer) skillData.get("attempted");
        int correct = (Integer) skillData.get("correct");
        List<Boolean> recentPerformance = (List<Boolean>) skillData.get("recentPerformance");
        
        if (attempted == 0) return "unknown";
        
        double accuracyRate = (double) correct / attempted;
        double recentAccuracy = recentPerformance.stream().mapToDouble(b -> b ? 1.0 : 0.0).average().orElse(0.0);
        
        if (accuracyRate >= 0.8 && recentAccuracy >= 0.8) {
            return "mastered";
        } else if (accuracyRate >= 0.6 && recentAccuracy >= 0.6) {
            return "developing";
        } else {
            return "struggling";
        }
    }
    
    public int determineNextDifficulty(String studentId, int currentDifficulty) {
        Optional<StudentProfile> profileOpt = studentProfileRepository.findByStudentId(studentId);
        if (profileOpt.isEmpty()) {
            return currentDifficulty;
        }
        
        StudentProfile profile = profileOpt.get();
        double overallAccuracy = profile.getAccuracy();
        
        if (overallAccuracy >= 0.8 && currentDifficulty < 5) {
            return Math.min(currentDifficulty + 1, 5);
        } else if (overallAccuracy <= 0.4 && currentDifficulty > 1) {
            return Math.max(currentDifficulty - 1, 1);
        }
        
        return currentDifficulty;
    }
    
    private boolean shouldOfferHint(StudentProfile profile, Question question) {
        String subSkill = question.getSubSkill();
        
        try {
            Map<String, String> masteryData = profile.getSubSkillMasteryJson();
            if (masteryData.containsKey(subSkill)) {
                Map<String, Object> skillData = objectMapper.readValue(masteryData.get(subSkill), new TypeReference<Map<String, Object>>() {});
                String masteryLevel = (String) skillData.get("masteryLevel");
                return "struggling".equals(masteryLevel);
            }
        } catch (JsonProcessingException e) {
            log.error("Error checking mastery level: {}", e.getMessage());
        }
        
        return false;
    }
    
    public Optional<StudentProfile> getStudentProfile(String studentId) {
        return studentProfileRepository.findByStudentId(studentId);
    }
    
    public PerformanceReport generatePerformanceReport(String studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new StudentNotFoundException("Student not found: " + studentId));
        
        Optional<StudentProfile> profileOpt = studentProfileRepository.findByStudentId(studentId);
        
        PerformanceReport report = new PerformanceReport();
        report.setStudentId(studentId);
        report.setGeneratedAt(LocalDateTime.now());
        
        report.setTotalQuestions(student.getTotalQuestionsAnswered());
        report.setOverallAccuracy(student.getOverallAccuracy().doubleValue());
        
        List<QuizSession> recentSessions = quizSessionRepository.findCompletedSessionsByStudentId(studentId);
        List<PerformanceReport.RecentSession> recentSessionDtos = new ArrayList<>();
        
        for (QuizSession session : recentSessions.subList(0, Math.min(5, recentSessions.size()))) {
            PerformanceReport.RecentSession sessionDto = new PerformanceReport.RecentSession(
                session.getId(),
                session.getStandardCode(),
                session.getCompletedAt(),
                session.getAccuracy()
            );
            recentSessionDtos.add(sessionDto);
        }
        report.setRecentSessions(recentSessionDtos);
        
        double avgTime = recentSessions.stream()
            .mapToLong(QuizSession::getTotalTimeMs)
            .average()
            .orElse(0.0);
        report.setAverageTime(avgTime);
        
        if (profileOpt.isPresent()) {
            StudentProfile profile = profileOpt.get();
            List<PerformanceReport.SubSkillPerformance> subSkillBreakdown = new ArrayList<>();
            for (Map.Entry<String, String> entry : profile.getSubSkillMasteryJson().entrySet()) {
                try {
                    Map<String, Object> skillData = objectMapper.readValue(entry.getValue(), new TypeReference<Map<String, Object>>() {});
                    
                    PerformanceReport.SubSkillPerformance skillPerformance = new PerformanceReport.SubSkillPerformance();
                    skillPerformance.setSkill(entry.getKey());
                    skillPerformance.setMasteryLevel((String) skillData.get("masteryLevel"));
                    
                    int attempted = (Integer) skillData.get("attempted");
                    int correct = (Integer) skillData.get("correct");
                    skillPerformance.setAccuracy(attempted > 0 ? (double) correct / attempted : 0.0);
                    skillPerformance.setQuestionsAttempted(attempted);
                    
                    subSkillBreakdown.add(skillPerformance);
                } catch (JsonProcessingException e) {
                    log.error("Error processing sub-skill data: {}", e.getMessage());
                }
            }
            report.setSubSkillBreakdown(subSkillBreakdown);
            report.setRecommendations(generateRecommendations(profile));
        } else {
            report.setSubSkillBreakdown(new ArrayList<>());
            report.setRecommendations(new ArrayList<>());
        }
        
        report.setStandardsProgress(calculateStandardsProgress(studentId));
        
        return report;
    }
    
    private List<PerformanceReport.Recommendation> generateRecommendations(StudentProfile profile) {
        List<PerformanceReport.Recommendation> recommendations = new ArrayList<>();
        
        double overallAccuracy = profile.getAccuracy();
        
        if (overallAccuracy < 0.6) {
            PerformanceReport.Recommendation rec = new PerformanceReport.Recommendation();
            rec.setType("difficulty_adjustment");
            rec.setPriority("high");
            rec.setMessage("Consider reviewing prerequisite skills before attempting current level");
            recommendations.add(rec);
        } else if (overallAccuracy > 0.85) {
            PerformanceReport.Recommendation rec = new PerformanceReport.Recommendation();
            rec.setType("challenge_increase");
            rec.setPriority("medium");
            rec.setMessage("Ready for more challenging problems to continue growth");
            recommendations.add(rec);
        }
        
        return recommendations;
    }
    
    private Map<String, Double> calculateStandardsProgress(String studentId) {
        Map<String, Double> standardsProgress = new HashMap<>();
        
        List<QuizSession> completedSessions = quizSessionRepository.findCompletedSessionsByStudentId(studentId);
        
        Map<String, List<QuizSession>> sessionsByStandard = completedSessions.stream()
                .filter(session -> session.getStandardCode() != null)
                .collect(Collectors.groupingBy(QuizSession::getStandardCode));
        
        for (Map.Entry<String, List<QuizSession>> entry : sessionsByStandard.entrySet()) {
            String standardCode = entry.getKey();
            List<QuizSession> sessions = entry.getValue();
            
            double averageAccuracy = sessions.stream()
                    .mapToDouble(QuizSession::getAccuracy)
                    .average()
                    .orElse(0.0);
            
            standardsProgress.put(standardCode, averageAccuracy);
        }
        
        return standardsProgress;
    }
    
    public StudentRegistrationResponse registerStudent(StudentRegistrationRequest request) {
        String studentId = request.getStudentId();
        String name = request.getName();
        String email = request.getEmail();
        
        boolean exists = studentRepository.existsById(studentId);
        if (exists) {
            Student existingStudent = studentRepository.findById(studentId).get();
            return new StudentRegistrationResponse(
                existingStudent.getId(),
                existingStudent.getName(), 
                existingStudent.getEmail(),
                false
            );
        }
        
        Student newStudent = new Student(studentId, name, email);
        Student savedStudent = studentRepository.save(newStudent);
        
        StudentProfile newProfile = new StudentProfile(studentId);
        studentProfileRepository.save(newProfile);
        
        log.info("New student registered: {} ({})", studentId, name);
        
        return new StudentRegistrationResponse(
            savedStudent.getId(),
            savedStudent.getName(),
            savedStudent.getEmail(),
            true
        );
    }
    
    private String convertAnswerIndexToKey(Integer answerIndex) {
        return switch (answerIndex) {
            case 0 -> "A";
            case 1 -> "B";
            case 2 -> "C";
            case 3 -> "D";
            default -> throw new RuntimeException("Invalid answer index: " + answerIndex);
        };
    }
}