package com.quizgenerator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TargetedQuizRequest {
    
    @NotBlank(message = "Student ID is required")
    private String studentId;
    
    @NotNull(message = "Standard codes are required")
    private List<String> standardCodes;
    
    @NotNull(message = "Question count is required")
    @Min(value = 1, message = "Question count must be at least 1")
    @Max(value = 20, message = "Question count must be at most 20")
    private Integer questionCount;
    
    @NotBlank(message = "Focus area is required")
    private String focusArea;
}