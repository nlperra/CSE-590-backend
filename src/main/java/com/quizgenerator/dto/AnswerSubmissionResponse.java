package com.quizgenerator.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSubmissionResponse {
    private Boolean correct;
    private String explanation;
    private String nextQuestionId;
    private Boolean sessionValid = true;
}