package com.quizgenerator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionStartRequest {
    
    @NotBlank(message = "Student ID is required")
    private String studentId;
    
    @NotBlank(message = "Standard code is required")
    private String standardCode;
    
    @NotNull(message = "Question count is required")
    @Min(value = 1, message = "Question count must be at least 1")
    private Integer questionCount;
}