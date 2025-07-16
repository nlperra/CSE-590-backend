package com.quizgenerator.dto;

import com.quizgenerator.model.Question;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TargetedQuizResponse {
    private String id;
    private String sessionId;
    private List<Question> questions;
    private String studentId;
    private String focusArea;
    private List<String> targetedStandards;
    private boolean isTargeted;
}