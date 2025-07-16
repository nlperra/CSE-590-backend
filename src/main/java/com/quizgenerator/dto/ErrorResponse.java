package com.quizgenerator.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;
    private Boolean exists;
    
    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }
}