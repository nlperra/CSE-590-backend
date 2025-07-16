package com.quizgenerator.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentRegistrationResponse {
    
    private String studentId;
    private String name;
    private String email;
    private Boolean created;
}