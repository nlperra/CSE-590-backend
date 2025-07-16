package com.quizgenerator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentRegistrationRequest {
    
    @NotBlank(message = "Student ID is required")
    @Size(min = 1, max = 50, message = "Student ID must be between 1 and 50 characters")
    private String studentId;
    
    @NotBlank(message = "Student name is required")
    @Size(max = 100, message = "Student name must be less than 100 characters")
    private String name;
    
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must be less than 255 characters")
    private String email;
}