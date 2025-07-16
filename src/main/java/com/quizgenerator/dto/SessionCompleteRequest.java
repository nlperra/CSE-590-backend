package com.quizgenerator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionCompleteRequest {
    
    @NotBlank(message = "Session ID is required")
    private String sessionId;
    
    @NotBlank(message = "Student ID is required")
    private String studentId;
}