package com.quizgenerator.controller;

import com.quizgenerator.dto.AnswerSubmissionRequest;
import com.quizgenerator.dto.AnswerSubmissionResponse;
import com.quizgenerator.dto.PerformanceReport;
import com.quizgenerator.dto.StudentRegistrationRequest;
import com.quizgenerator.dto.StudentRegistrationResponse;
import com.quizgenerator.dto.ErrorResponse;
import com.quizgenerator.model.StudentProfile;
import com.quizgenerator.exception.StudentNotFoundException;
import com.quizgenerator.service.AdaptiveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/adaptive")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class AdaptiveController {
    
    private final AdaptiveService adaptiveService;
    
    @PostMapping("/submit-answer")
    public ResponseEntity<?> submitAnswer(@Valid @RequestBody AnswerSubmissionRequest request) {
        try {
            log.info("Submitting answer for session {} student {} question {}", 
                request.getSessionId(), request.getStudentId(), request.getQuestionId());
            
            AnswerSubmissionResponse response = adaptiveService.submitAnswer(request);
            
            if (!response.getSessionValid()) {
                log.warn("Invalid session for answer submission: sessionId={}, studentId={}", 
                    request.getSessionId(), request.getStudentId());
                ErrorResponse error = new ErrorResponse("session_invalid", "Session is invalid or expired");
                return ResponseEntity.badRequest().body(error);
            }
            
            log.info("Answer submitted successfully for session {}: correct={}", 
                request.getSessionId(), response.getCorrect());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for answer submission: {}", e.getMessage());
            ErrorResponse error = new ErrorResponse("validation_error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            log.error("Error submitting answer for session {}: {}", request.getSessionId(), e.getMessage());
            ErrorResponse error = new ErrorResponse("submission_error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error submitting answer for session {}: {}", request.getSessionId(), e.getMessage(), e);
            ErrorResponse error = new ErrorResponse("internal_error", "Failed to submit answer: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @GetMapping("/student/{studentId}/profile")
    public ResponseEntity<StudentProfile> getStudentProfile(@PathVariable String studentId) {
        Optional<StudentProfile> profile = adaptiveService.getStudentProfile(studentId);
        return profile.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/student/{studentId}/report")
    public ResponseEntity<?> getPerformanceReport(@PathVariable String studentId) {
        try {
            log.info("Generating performance report for student: {}", studentId);
            
            PerformanceReport report = adaptiveService.generatePerformanceReport(studentId);
            
            log.info("Performance report generated successfully for student: {}", studentId);
            
            return ResponseEntity.ok(report);
        } catch (StudentNotFoundException e) {
            log.warn("Cannot generate performance report - student not found: {}", studentId);
            ErrorResponse error = new ErrorResponse("student_not_found", e.getMessage());
            return ResponseEntity.status(404).body(error);
        } catch (Exception e) {
            log.error("Error generating performance report for student {}: {}", studentId, e.getMessage(), e);
            ErrorResponse error = new ErrorResponse("internal_error", "Failed to generate performance report: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @PostMapping("/student/register")
    public ResponseEntity<StudentRegistrationResponse> registerStudent(@Valid @RequestBody StudentRegistrationRequest request) {
        try {
            StudentRegistrationResponse response = adaptiveService.registerStudent(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error registering student: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}