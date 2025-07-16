package com.quizgenerator.controller;

import com.quizgenerator.dto.QuizGenerationRequest;
import com.quizgenerator.dto.QuizResponse;
import com.quizgenerator.dto.SessionStartRequest;
import com.quizgenerator.dto.SessionStartResponse;
import com.quizgenerator.dto.SessionCompleteRequest;
import com.quizgenerator.dto.SessionCompleteResponse;
import com.quizgenerator.dto.QuizHistoryResponse;
import com.quizgenerator.dto.ErrorResponse;
import com.quizgenerator.model.Question;
import com.quizgenerator.exception.StudentNotFoundException;
import com.quizgenerator.service.AdaptiveService;
import com.quizgenerator.service.QuizService;
import com.quizgenerator.service.QuizSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/quiz")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class QuizController {
    
    private final QuizService quizService;
    private final AdaptiveService adaptiveService;
    private final QuizSessionService quizSessionService;
    
    @PostMapping("/generate")
    public ResponseEntity<QuizResponse> generateQuiz(@Valid @RequestBody QuizGenerationRequest request) {
        try {
            if (request.getStudentId() != null) {
                int adaptedDifficulty = adaptiveService.determineNextDifficulty(
                    request.getStudentId(), 
                    request.getDifficulty()
                );
                request.setDifficulty(adaptedDifficulty);
            }
            
            QuizResponse quiz = quizService.generateQuiz(request);
            return ResponseEntity.ok(quiz);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/question/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        Optional<Question> question = quizService.getQuestionById(id);
        return question.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/standard/{standardCode}")
    public ResponseEntity<List<Question>> getQuestionsByStandard(@PathVariable String standardCode) {
        List<Question> questions = quizService.getQuestionsByStandard(standardCode);
        return ResponseEntity.ok(questions);
    }
    
    @PostMapping("/session/start")
    public ResponseEntity<?> startQuizSession(@Valid @RequestBody SessionStartRequest request) {
        try {
            log.info("Starting quiz session for student {} with standard {} and {} questions", 
                request.getStudentId(), request.getStandardCode(), request.getQuestionCount());
            
            SessionStartResponse response = quizSessionService.startQuizSession(request);
            
            log.info("Quiz session started successfully: sessionId={}, quizId={}", 
                response.getSessionId(), response.getQuizId());
            
            return ResponseEntity.ok(response);
        } catch (StudentNotFoundException e) {
            log.warn("Cannot start quiz session - student not found: {}", request.getStudentId());
            ErrorResponse error = new ErrorResponse("student_not_found", e.getMessage());
            return ResponseEntity.status(404).body(error);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for quiz session start: {}", e.getMessage());
            ErrorResponse error = new ErrorResponse("validation_error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error starting quiz session for student {}: {}", request.getStudentId(), e.getMessage(), e);
            ErrorResponse error = new ErrorResponse("internal_error", "Failed to start quiz session: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @PostMapping("/session/complete")
    public ResponseEntity<?> completeQuizSession(@Valid @RequestBody SessionCompleteRequest request) {
        try {
            log.info("Completing quiz session {} for student {}", request.getSessionId(), request.getStudentId());
            
            SessionCompleteResponse response = quizSessionService.completeQuizSession(request);
            
            log.info("Quiz session completed successfully: sessionId={}", request.getSessionId());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for quiz session complete: {}", e.getMessage());
            ErrorResponse error = new ErrorResponse("validation_error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            log.error("Error completing quiz session {}: {}", request.getSessionId(), e.getMessage());
            ErrorResponse error = new ErrorResponse("session_error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error completing quiz session {}: {}", request.getSessionId(), e.getMessage(), e);
            ErrorResponse error = new ErrorResponse("internal_error", "Failed to complete quiz session: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @GetMapping("/history/{studentId}")
    public ResponseEntity<?> getQuizHistory(@PathVariable String studentId) {
        try {
            log.info("Getting quiz history for student: {}", studentId);
            
            QuizHistoryResponse history = quizService.getQuizHistory(studentId);
            
            log.info("Quiz history retrieved successfully for student: {} ({} quizzes)", 
                    studentId, history.getTotalQuizzes());
            
            return ResponseEntity.ok(history);
        } catch (StudentNotFoundException e) {
            log.warn("Cannot get quiz history - student not found: {}", studentId);
            ErrorResponse error = new ErrorResponse("student_not_found", e.getMessage());
            return ResponseEntity.status(404).body(error);
        } catch (Exception e) {
            log.error("Error getting quiz history for student {}: {}", studentId, e.getMessage(), e);
            ErrorResponse error = new ErrorResponse("internal_error", "Failed to get quiz history: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}