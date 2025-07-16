package com.quizgenerator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSubmissionRequest {
    
    @NotBlank(message = "Session ID is required")
    private String sessionId;
    
    @NotBlank(message = "Student ID is required")
    private String studentId;
    
    @NotBlank(message = "Question ID is required")
    private String questionId;
    
    @NotNull(message = "Answer is required")
    @Min(value = 0, message = "Answer must be between 0 and 3")
    @Max(value = 3, message = "Answer must be between 0 and 3")
    private Integer answer;
    
    @NotNull(message = "Time spent is required")
    @Min(value = 0, message = "Time spent must be positive")
    private Long timeSpent;
    
    @NotBlank(message = "Quiz ID is required")
    private String quizId;
}