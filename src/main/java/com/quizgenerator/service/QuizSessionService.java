package com.quizgenerator.service;

import com.quizgenerator.dto.*;
import com.quizgenerator.model.*;
import com.quizgenerator.repository.*;
import com.quizgenerator.exception.StudentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class QuizSessionService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final StudentRepository studentRepository;
    private final QuizService quizService;
    private final AnalyticsService analyticsService;

    public SessionStartResponse startQuizSession(SessionStartRequest request) {
        try {
            Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new StudentNotFoundException("Student not found: " + request.getStudentId()));
            
            student.updateLogin();
            studentRepository.save(student);

            String sessionId = UUID.randomUUID().toString();
            String quizId = "quiz_" + sessionId;
            
            QuizSession session = new QuizSession(sessionId, request.getStudentId(), quizId, request.getStandardCode());
            quizSessionRepository.save(session);
            
            log.info("Started quiz session {} for student {} with standard {}", 
                sessionId, request.getStudentId(), request.getStandardCode());
            
            return new SessionStartResponse(sessionId, quizId);
            
        } catch (StudentNotFoundException e) {
            log.warn("Cannot start quiz session - student not found: {}", request.getStudentId());
            throw e;
        } catch (Exception e) {
            log.error("Error starting quiz session for student {}: {}", request.getStudentId(), e.getMessage(), e);
            throw new RuntimeException("Failed to start quiz session: " + e.getMessage());
        }
    }

    public SessionCompleteResponse completeQuizSession(SessionCompleteRequest request) {
        Optional<QuizSession> sessionOpt = quizSessionRepository.findByIdAndStudentId(
            request.getSessionId(), request.getStudentId());
        
        if (sessionOpt.isEmpty()) {
            throw new RuntimeException("Quiz session not found or not owned by student");
        }

        QuizSession session = sessionOpt.get();
        
        Long totalQuestions = quizAnswerRepository.countTotalAnswersBySessionId(request.getSessionId());
        Long correctAnswers = quizAnswerRepository.countCorrectAnswersBySessionId(request.getSessionId());
        Long totalTimeMs = quizAnswerRepository.sumTimeSpentBySessionId(request.getSessionId());
        
        session.setTotalQuestions(totalQuestions.intValue());
        session.setCorrectAnswers(correctAnswers.intValue());
        session.setTotalTimeMs(totalTimeMs != null ? totalTimeMs : 0L);
        session.completeSession();
        
        quizSessionRepository.save(session);
        
        updateStudentStatistics(request.getStudentId());
        
        try {
            analyticsService.updateStudentPerformance(request.getStudentId(), request.getSessionId());
            log.info("Analytics updated for session {}", request.getSessionId());
        } catch (Exception e) {
            log.error("Failed to update analytics for session {}: {}", request.getSessionId(), e.getMessage());
        }
        
        log.info("Completed quiz session {} for student {}", request.getSessionId(), request.getStudentId());
        
        SessionCompleteResponse.QuizResults results = new SessionCompleteResponse.QuizResults(
            totalQuestions.intValue(),
            correctAnswers.intValue(),
            totalQuestions > 0 ? (double) correctAnswers / totalQuestions : 0.0,
            totalTimeMs != null ? totalTimeMs : 0L
        );
        
        return new SessionCompleteResponse(request.getSessionId(), results);
    }

    public boolean isValidSession(String sessionId, String studentId) {
        Optional<QuizSession> session = quizSessionRepository.findByIdAndStudentId(sessionId, studentId);
        return session.isPresent() && session.get().getStatus() == QuizSession.SessionStatus.ACTIVE;
    }

    public void recordAnswer(String sessionId, String questionId, Integer answer, 
                           Boolean isCorrect, Long timeSpent) {
        QuizAnswer quizAnswer = new QuizAnswer(sessionId, questionId, answer, isCorrect, timeSpent);
        quizAnswerRepository.save(quizAnswer);
        
        Optional<QuizSession> sessionOpt = quizSessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            QuizSession session = sessionOpt.get();
            session.addAnswer(isCorrect, timeSpent);
            quizSessionRepository.save(session);
        }
    }


    private void updateStudentStatistics(String studentId) {
        Optional<Student> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            return;
        }

        Student student = studentOpt.get();
        
        var completedSessions = quizSessionRepository.findCompletedSessionsByStudentId(studentId);
        
        int totalQuizzes = completedSessions.size();
        int totalQuestions = completedSessions.stream().mapToInt(QuizSession::getTotalQuestions).sum();
        int totalCorrect = completedSessions.stream().mapToInt(QuizSession::getCorrectAnswers).sum();
        
        BigDecimal overallAccuracy = totalQuestions > 0 ? 
            BigDecimal.valueOf((double) totalCorrect / totalQuestions).setScale(4, java.math.RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;

        student.setTotalQuizzesCompleted(totalQuizzes);
        student.setTotalQuestionsAnswered(totalQuestions);
        student.setOverallAccuracy(overallAccuracy);
        
        studentRepository.save(student);
    }
}