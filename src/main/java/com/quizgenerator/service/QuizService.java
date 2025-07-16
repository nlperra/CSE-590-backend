package com.quizgenerator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizgenerator.dto.QuizGenerationRequest;
import com.quizgenerator.dto.QuizResponse;
import com.quizgenerator.dto.TargetedQuizRequest;
import com.quizgenerator.dto.TargetedQuizResponse;
import com.quizgenerator.dto.QuizHistoryResponse;
import com.quizgenerator.exception.StudentNotFoundException;
import com.quizgenerator.model.Question;
import com.quizgenerator.model.Standard;
import com.quizgenerator.model.StandardPerformance;
import com.quizgenerator.model.QuizSession;
import com.quizgenerator.repository.StudentRepository;
import com.quizgenerator.repository.StandardPerformanceRepository;
import com.quizgenerator.repository.QuizSessionRepository;
import com.quizgenerator.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class QuizService {
    
    private final QuestionRepository questionRepository;
    private final StandardsService standardsService;
    private final ClaudeApiService claudeApiService;
    private final StudentRepository studentRepository;
    private final StandardPerformanceRepository standardPerformanceRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public QuizResponse generateQuiz(QuizGenerationRequest request) {
        Optional<Standard> standardOpt = standardsService.getStandardByCode(request.getStandardCode());
        if (standardOpt.isEmpty()) {
            throw new RuntimeException("Standard not found: " + request.getStandardCode());
        }
        
        Standard standard = standardOpt.get();
        
        List<Question> questions = generateQuestions(standard, request.getQuestionCount(), request.getDifficulty());
        
        QuizResponse response = new QuizResponse();
        response.setId(UUID.randomUUID().toString());
        response.setStandardCode(standard.getCode());
        response.setStandardTitle(standard.getTitle());
        response.setQuestions(questions);
        response.setDifficulty(request.getDifficulty());
        response.setAdaptiveMode(request.getStudentId() != null);
        
        return response;
    }
    
    private List<Question> generateQuestions(Standard standard, int questionCount, int difficulty) {
        List<Question> questions = new ArrayList<>();
        
        List<String> questionTypes = Arrays.asList("word_problem", "direct_computation", "visual");
        Map<String, Integer> typeDistribution = distributeQuestionTypes(questionTypes, questionCount);
        
        for (Map.Entry<String, Integer> entry : typeDistribution.entrySet()) {
            String type = entry.getKey();
            int count = entry.getValue();
            
            try {
                List<Question> typeQuestions = generateQuestionsByType(standard, type, count, difficulty);
                questions.addAll(typeQuestions);
            } catch (Exception e) {
                log.error("Failed to generate {} questions for type {}: {}", count, type, e.getMessage());
                questions.addAll(generateFallbackQuestions(standard, type, count, difficulty));
            }
        }
        
        Collections.shuffle(questions);
        return questions;
    }
    
    private List<Question> generateQuestionsByType(Standard standard, String type, int count, int difficulty) throws IOException {
        String subSkills = standard.getSubSkills() != null ? String.join(", ", standard.getSubSkills()) : "";
        
        String claudeResponse = claudeApiService.generateQuestions(
            standard.getCode(),
            standard.getTitle(),
            standard.getDescription(),
            subSkills,
            type,
            count,
            difficulty
        );
        
        Map<String, Object> responseMap = objectMapper.readValue(claudeResponse, new TypeReference<Map<String, Object>>() {});
        List<Map<String, Object>> questionsData = (List<Map<String, Object>>) responseMap.get("questions");
        
        List<Question> questions = new ArrayList<>();
        for (Map<String, Object> questionData : questionsData) {
            Question question = mapToQuestion(questionData, standard.getCode(), type);
            questionRepository.save(question);
            questions.add(question);
        }
        
        return questions;
    }
    
    private Question mapToQuestion(Map<String, Object> questionData, String standardCode, String type) {
        Question question = new Question();
        question.setQuestion((String) questionData.get("question"));
        
        Map<String, String> originalChoices = (Map<String, String>) questionData.get("choices");
        String originalCorrectKey = (String) questionData.get("correctAnswer");
        String correctAnswerText = originalChoices.get(originalCorrectKey);
        
        List<String> answerTexts = new ArrayList<>(originalChoices.values());
        Collections.shuffle(answerTexts);
        
        Map<String, String> shuffledChoices = new LinkedHashMap<>();
        String newCorrectKey = null;
        String[] keys = {"A", "B", "C", "D"};
        
        for (int i = 0; i < answerTexts.size() && i < keys.length; i++) {
            String answerText = answerTexts.get(i);
            shuffledChoices.put(keys[i], answerText);
            
            if (answerText.equals(correctAnswerText)) {
                newCorrectKey = keys[i];
            }
        }
        
        question.setChoices(shuffledChoices);
        question.setCorrectAnswer(newCorrectKey);
        question.setExplanation((String) questionData.get("explanation"));
        question.setDifficulty((Integer) questionData.get("difficulty"));
        question.setSubSkill((String) questionData.get("subSkill"));
        question.setStandardCode(standardCode);
        question.setType(type);
        
        Map<String, String> originalMisconceptions = (Map<String, String>) questionData.get("misconceptions");
        if (originalMisconceptions != null) {
            Map<String, String> shuffledMisconceptions = new HashMap<>();
            for (Map.Entry<String, String> entry : shuffledChoices.entrySet()) {
                String key = entry.getKey();
                String answerText = entry.getValue();
                
                for (Map.Entry<String, String> origEntry : originalChoices.entrySet()) {
                    if (origEntry.getValue().equals(answerText) && originalMisconceptions.containsKey(origEntry.getKey())) {
                        shuffledMisconceptions.put(key, originalMisconceptions.get(origEntry.getKey()));
                        break;
                    }
                }
            }
            question.setMisconceptions(shuffledMisconceptions);
        }
        
        return question;
    }
    
    private Map<String, Integer> distributeQuestionTypes(List<String> types, int totalCount) {
        Map<String, Integer> distribution = new HashMap<>();
        int baseCount = totalCount / types.size();
        int remainder = totalCount % types.size();
        
        for (int i = 0; i < types.size(); i++) {
            int count = baseCount + (i < remainder ? 1 : 0);
            distribution.put(types.get(i), count);
        }
        
        return distribution;
    }
    
    private List<Question> generateFallbackQuestions(Standard standard, String type, int count, int difficulty) {
        List<Question> fallbackQuestions = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            Question question = new Question();
            question.setQuestion(String.format("Sample %s question for %s", type, standard.getTitle()));
            
            List<String> answerTexts = Arrays.asList(
                "Sample choice A",
                "Sample choice B", 
                "Sample choice C",
                "Sample choice D"
            );
            
            Collections.shuffle(answerTexts);
            
            Map<String, String> choices = new LinkedHashMap<>();
            String[] keys = {"A", "B", "C", "D"};
            for (int j = 0; j < keys.length; j++) {
                choices.put(keys[j], answerTexts.get(j));
            }
            
            String correctKey = keys[random.nextInt(keys.length)];
            
            question.setChoices(choices);
            question.setCorrectAnswer(correctKey);
            question.setExplanation("This is a fallback question generated when API call fails. The correct answer is " + correctKey + ": " + choices.get(correctKey));
            question.setDifficulty(difficulty);
            question.setSubSkill(standard.getSubSkills() != null && !standard.getSubSkills().isEmpty() 
                ? standard.getSubSkills().get(0) : "general");
            question.setStandardCode(standard.getCode());
            question.setType(type);
            question.setIsFallback(true);
            
            questionRepository.save(question);
            fallbackQuestions.add(question);
        }
        
        return fallbackQuestions;
    }
    
    public Optional<Question> getQuestionById(Long questionId) {
        return questionRepository.findById(questionId);
    }
    
    public List<Question> getQuestionsByStandard(String standardCode) {
        return questionRepository.findByStandardCode(standardCode);
    }
    
    public TargetedQuizResponse generateTargetedQuiz(TargetedQuizRequest request) {
        if (!studentRepository.existsById(request.getStudentId())) {
            throw new StudentNotFoundException("Student not found: " + request.getStudentId());
        }
        
        List<Standard> targetedStandards = new ArrayList<>();
        Map<String, StandardPerformance> performanceMap = new HashMap<>();
        
        for (String standardCode : request.getStandardCodes()) {
            Optional<Standard> standardOpt = standardsService.getStandardByCode(standardCode);
            if (standardOpt.isPresent()) {
                targetedStandards.add(standardOpt.get());
                
                Optional<StandardPerformance> performance = standardPerformanceRepository
                        .findByStudentIdAndStandardCode(request.getStudentId(), standardCode);
                performance.ifPresent(p -> performanceMap.put(standardCode, p));
            }
        }
        
        if (targetedStandards.isEmpty()) {
            throw new IllegalArgumentException("No valid standards found for the provided codes");
        }
        
        List<Question> allQuestions = new ArrayList<>();
        int questionsPerStandard = Math.max(1, request.getQuestionCount() / targetedStandards.size());
        int remainingQuestions = request.getQuestionCount();
        
        for (Standard standard : targetedStandards) {
            int questionsForThisStandard = Math.min(questionsPerStandard, remainingQuestions);
            if (questionsForThisStandard <= 0) break;
            
            int difficulty = determineDifficultyLevel(
                    performanceMap.get(standard.getCode()), request.getFocusArea());
            
            try {
                List<Question> questions = generateQuestionsForTargeted(
                        standard, questionsForThisStandard, difficulty, request.getFocusArea());
                allQuestions.addAll(questions);
                remainingQuestions -= questionsForThisStandard;
            } catch (Exception e) {
                log.error("Failed to generate targeted questions for standard {}: {}", 
                         standard.getCode(), e.getMessage());
                
                List<Question> fallbackQuestions = generateFallbackQuestions(
                        standard, "targeted", questionsForThisStandard, difficulty);
                allQuestions.addAll(fallbackQuestions);
                remainingQuestions -= questionsForThisStandard;
            }
        }
        
        Collections.shuffle(allQuestions);
        
        return TargetedQuizResponse.builder()
                .id(UUID.randomUUID().toString())
                .sessionId(UUID.randomUUID().toString())
                .questions(allQuestions)
                .studentId(request.getStudentId())
                .focusArea(request.getFocusArea())
                .targetedStandards(request.getStandardCodes())
                .isTargeted(true)
                .build();
    }
    
    private int determineDifficultyLevel(StandardPerformance performance, String focusArea) {
        if (performance == null) {
            return "struggling".equals(focusArea) ? 1 : 2;
        }

        return switch (performance.getMasteryLevel()) {
            case STRUGGLING -> "review".equals(focusArea) ? 2 : 1;
            case DEVELOPING -> "struggling".equals(focusArea) ? 1 : 2;
            case PROFICIENT, MASTERED -> "struggling".equals(focusArea) ? 2 : 3;
        };
    }
    
    private List<Question> generateQuestionsForTargeted(Standard standard, int count, 
                                                       int difficulty, String focusArea) throws IOException {
        String subSkills = standard.getSubSkills() != null ? String.join(", ", standard.getSubSkills()) : "";
        String questionType = determineQuestionType(focusArea);
        
        String claudeResponse = claudeApiService.generateQuestions(
            standard.getCode(),
            standard.getTitle(),
            standard.getDescription(),
            subSkills,
            questionType,
            count,
            difficulty
        );
        
        Map<String, Object> responseMap = objectMapper.readValue(claudeResponse, new TypeReference<Map<String, Object>>() {});
        List<Map<String, Object>> questionsData = (List<Map<String, Object>>) responseMap.get("questions");
        
        List<Question> questions = new ArrayList<>();
        for (Map<String, Object> questionData : questionsData) {
            Question question = mapToQuestion(questionData, standard.getCode(), questionType);
            question.setIsTargeted(true);
            question.setFocusArea(focusArea);
            questionRepository.save(question);
            questions.add(question);
        }
        
        return questions;
    }
    
    private String determineQuestionType(String focusArea) {
        return switch (focusArea.toLowerCase()) {
            case "struggling" -> "direct_computation";
            case "review" -> "word_problem";
            case "mixed" -> "visual";
            default -> "word_problem";
        };
    }
    
    public QuizHistoryResponse getQuizHistory(String studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new StudentNotFoundException("Student not found: " + studentId);
        }
        
        List<QuizSession> completedSessions = quizSessionRepository.findByStudentIdAndStatusOrderByCompletedAtDesc(
                studentId, QuizSession.SessionStatus.COMPLETED);
        
        List<QuizHistoryResponse.QuizDetails> quizDetails = new ArrayList<>();
        
        for (QuizSession session : completedSessions) {
            String standardTitle = "Unknown Standard";
            if (session.getStandardCode() != null) {
                Optional<Standard> standard = standardsService.getStandardByCode(session.getStandardCode());
                standardTitle = standard.map(Standard::getTitle).orElse("Unknown Standard");
            }
            
            QuizHistoryResponse.QuizDetails details = new QuizHistoryResponse.QuizDetails(
                    session.getId(),
                    session.getQuizId(),
                    session.getStandardCode(),
                    standardTitle,
                    session.getAccuracy(),
                    session.getTotalQuestions(),
                    session.getCorrectAnswers(),
                    session.getTotalTimeMs(),
                    session.getStartedAt(),
                    session.getCompletedAt(),
                    session.getMasteryLevel() != null ? session.getMasteryLevel().name().toLowerCase() : "developing"
            );
            
            quizDetails.add(details);
        }
        
        return new QuizHistoryResponse(
                studentId,
                completedSessions.size(),
                quizDetails
        );
    }
}