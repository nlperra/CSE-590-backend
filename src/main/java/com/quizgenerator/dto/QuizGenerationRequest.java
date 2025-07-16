package com.quizgenerator.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizGenerationRequest {
    
    @NotBlank(message = "Standard code is required")
    private String standardCode;
    
    @Min(value = 1, message = "Difficulty must be between 1 and 5")
    @Max(value = 5, message = "Difficulty must be between 1 and 5")
    private Integer difficulty = 3;
    
    @Min(value = 1, message = "Question count must be at least 1")
    @Max(value = 50, message = "Question count must not exceed 50")
    private Integer questionCount = 10;
    
    private String studentId;
}