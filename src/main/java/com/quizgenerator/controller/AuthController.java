package com.quizgenerator.controller;

import com.quizgenerator.dto.*;
import com.quizgenerator.model.Student;
import com.quizgenerator.repository.StudentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class AuthController {

    private final StudentRepository studentRepository;

    @GetMapping("/check/{studentId}")
    public ResponseEntity<StudentExistsResponse> checkStudentExists(@PathVariable String studentId) {
        try {
            log.info("Checking if student exists: {}", studentId);
            
            boolean exists = studentRepository.existsById(studentId);
            
            log.info("Student {} exists: {}", studentId, exists);
            
            return ResponseEntity.ok(new StudentExistsResponse(exists));
        } catch (Exception e) {
            log.error("Error checking student existence for {}: {}", studentId, e.getMessage(), e);
            return ResponseEntity.ok(new StudentExistsResponse(false));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginStudent(@Valid @RequestBody StudentLoginRequest request) {
        try {
            log.info("Login attempt for student: {}", request.getStudentId());
            
            Optional<Student> studentOpt = studentRepository.findById(request.getStudentId());
            
            if (studentOpt.isEmpty()) {
                log.warn("Login failed - student not found: {}", request.getStudentId());
                ErrorResponse error = new ErrorResponse("student_not_found", "Student not found");
                error.setExists(false);
                return ResponseEntity.status(404).body(error);
            }
            
            Student student = studentOpt.get();
            
            student.updateLogin();
            studentRepository.save(student);
            
            log.info("Login successful for student: {}", request.getStudentId());
            
            StudentLoginResponse response = new StudentLoginResponse(
                student.getId(),
                student.getName(),
                student.getEmail(),
                true
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during login for student {}: {}", request.getStudentId(), e.getMessage(), e);
            ErrorResponse error = new ErrorResponse("internal_error", "Login failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerStudent(@Valid @RequestBody StudentRegistrationRequest request) {
        try {
            log.info("Registration attempt for student: {} with name: {}", request.getStudentId(), request.getName());
            
            if (studentRepository.existsById(request.getStudentId())) {
                log.warn("Registration failed - student already exists: {}", request.getStudentId());
                ErrorResponse error = new ErrorResponse("student_exists", "Student ID already exists");
                error.setExists(true);
                return ResponseEntity.status(409).body(error);
            }
            
            Student newStudent = new Student(
                request.getStudentId(),
                request.getName(),
                request.getEmail()
            );
            
            Student savedStudent = studentRepository.save(newStudent);
            
            log.info("Registration successful for student: {}", request.getStudentId());
            
            StudentRegistrationResponse response = new StudentRegistrationResponse(
                savedStudent.getId(),
                savedStudent.getName(),
                savedStudent.getEmail(),
                true
            );
            
            return ResponseEntity.ok(response);
            
        } catch (DataIntegrityViolationException e) {
            log.warn("Registration failed - data integrity violation for {}: {}", request.getStudentId(), e.getMessage());
            ErrorResponse error = new ErrorResponse("student_exists", "Student ID already exists");
            error.setExists(true);
            return ResponseEntity.status(409).body(error);
        } catch (Exception e) {
            log.error("Error during registration for student {}: {}", request.getStudentId(), e.getMessage(), e);
            ErrorResponse error = new ErrorResponse("internal_error", "Registration failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}