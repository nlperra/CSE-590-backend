package com.quizgenerator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ClaudeApiService {
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    @Value("${app.anthropic.api-key}")
    private String apiKey;
    
    @Value("${app.anthropic.base-url}")
    private String baseUrl;
    
    @Value("${app.anthropic.model}")
    private String model;
    
    @Value("${app.anthropic.max-tokens}")
    private int maxTokens;
    
    @Value("${app.anthropic.temperature}")
    private double temperature;
    
    public ClaudeApiService() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    
    public String makeRequest(String prompt, int maxTokens, double temperature) throws IOException {
        String requestBody = createRequestBody(prompt, maxTokens, temperature);
        
        Request request = new Request.Builder()
            .url(baseUrl + "/v1/messages")
            .addHeader("Content-Type", "application/json")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(RequestBody.create(requestBody, MediaType.get("application/json")))
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                handleApiError(response);
            }
            
            String responseBody = response.body().string();
            return extractContentFromResponse(responseBody);
        }
    }
    
    private String createRequestBody(String prompt, int maxTokens, double temperature) throws JsonProcessingException {
        return objectMapper.writeValueAsString(new RequestBodyBuilder()
            .model(model)
            .maxTokens(maxTokens)
            .temperature(temperature)
            .addMessage("user", prompt)
            .build());
    }
    
    private String extractContentFromResponse(String responseBody) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode contentArray = rootNode.get("content");
        
        if (contentArray != null && contentArray.isArray() && !contentArray.isEmpty()) {
            return contentArray.get(0).get("text").asText();
        }
        
        throw new RuntimeException("Unexpected response format from Claude API");
    }
    
    private void handleApiError(Response response) throws IOException {
        int statusCode = response.code();
        String responseBody = response.body() != null ? response.body().string() : "";
        
        switch (statusCode) {
            case 401:
                throw new RuntimeException("Invalid API key. Please check your credentials.");
            case 429:
                throw new RuntimeException("Rate limit exceeded. Please try again later.");
            case 500:
                throw new RuntimeException("Claude API server error. Please try again later.");
            default:
                throw new RuntimeException("API request failed with status " + statusCode + ": " + responseBody);
        }
    }
    
    public String parseStandards(String pdfText) throws IOException {
        String prompt = """
            You are an expert in educational standards analysis. Parse the following Indiana 5th Grade Math Standards text and extract structured information.

            For each standard, identify:
            1. Standard code (e.g., "5.NS.1")
            2. Standard title/description
            3. Sub-skills and learning objectives
            4. Mathematical concepts involved
            5. Prerequisite skills needed
            6. Difficulty level (1-5 scale)
            7. Key vocabulary terms

            Format your response as a JSON array with this structure:
            {
              "standards": [
                {
                  "code": "standard_code",
                  "title": "standard_title",
                  "description": "detailed_description",
                  "subSkills": ["skill1", "skill2"],
                  "concepts": ["concept1", "concept2"],
                  "prerequisites": ["prereq1", "prereq2"],
                  "difficulty": 1-5,
                  "vocabulary": ["term1", "term2"]
                }
              ]
            }

            Standards text:
            """ + pdfText;
        
        return makeRequest(prompt, 8000, 0.7);
    }
    
    public String generateQuestions(String standardCode, String standardTitle, String description, 
                                   String subSkills, String type, int count, int difficulty) throws IOException {
        String prompt = String.format("""
            Generate %d multiple-choice questions for the following 5th grade math standard:

            Standard: %s - %s
            Description: %s
            Sub-skills: %s
            Difficulty Level: %d/5
            Question Type: %s

            Requirements:
            1. Each question should have 4 answer choices (A, B, C, D)
            2. Include realistic distractors that reveal common misconceptions
            3. Vary question types: word problems, visual scenarios, direct computation
            4. Ensure mathematical accuracy
            5. Age-appropriate language and contexts

            Format your response as JSON:
            {
              "questions": [
                {
                  "question": "question_text",
                  "choices": {
                    "A": "choice_text",
                    "B": "choice_text",
                    "C": "choice_text",
                    "D": "choice_text"
                  },
                  "correctAnswer": "A",
                  "explanation": "why_this_is_correct",
                  "difficulty": 1-5,
                  "misconceptions": {
                    "B": "misconception1",
                    "C": "misconception2",
                    "D": "misconception3"
                  },
                  "subSkill": "specific_subskill_assessed"
                }
              ]
            }
            """, count, standardCode, standardTitle, description, subSkills, difficulty, type);
        
        return makeRequest(prompt, 6000, 0.7);
    }
    
    public String generateHint(String question, String correctAnswer, String studentAnswer) throws IOException {
        String prompt = String.format("""
            A student answered "%s" to this question:
            "%s"

            The correct answer is "%s".

            Generate a helpful hint that:
            1. Doesn't give away the answer directly
            2. Guides the student toward the correct thinking process
            3. Addresses the likely misconception behind their wrong answer
            4. Is encouraging and age-appropriate for 5th graders

            Respond with just the hint text, no additional formatting.
            """, studentAnswer, question, correctAnswer);
        
        return makeRequest(prompt, 500, 0.7);
    }
    
    private static class RequestBodyBuilder {
        private String model;
        private int maxTokens;
        private double temperature;
        private java.util.List<java.util.Map<String, String>> messages = new java.util.ArrayList<>();
        
        public RequestBodyBuilder model(String model) {
            this.model = model;
            return this;
        }
        
        public RequestBodyBuilder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }
        
        public RequestBodyBuilder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }
        
        public RequestBodyBuilder addMessage(String role, String content) {
            messages.add(java.util.Map.of("role", role, "content", content));
            return this;
        }
        
        public java.util.Map<String, Object> build() {
            return java.util.Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "temperature", temperature,
                "messages", messages
            );
        }
    }
}