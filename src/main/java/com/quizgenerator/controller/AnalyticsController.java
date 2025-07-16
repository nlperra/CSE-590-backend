package com.quizgenerator.controller;

import com.quizgenerator.dto.StudentAnalytics;
import com.quizgenerator.dto.TargetedQuizRequest;
import com.quizgenerator.dto.TargetedQuizResponse;
import com.quizgenerator.exception.StudentNotFoundException;
import com.quizgenerator.service.AnalyticsService;
import com.quizgenerator.service.QuizService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CrossOrigin(origins = "*")
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    private final QuizService quizService;
    
    @GetMapping("/student/{studentId}")
    public ResponseEntity<StudentAnalytics> getStudentAnalytics(@PathVariable String studentId) {
        try {
            StudentAnalytics analytics = analyticsService.getStudentAnalytics(studentId);
            return ResponseEntity.ok(analytics);
        } catch (StudentNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/quiz/generate/targeted")
    public ResponseEntity<?> generateTargetedQuiz(@Valid @RequestBody TargetedQuizRequest request) {
        try {
            TargetedQuizResponse response = quizService.generateTargetedQuiz(request);
            return ResponseEntity.ok(response);
        } catch (StudentNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to generate targeted quiz: " + e.getMessage());
        }
    }
}