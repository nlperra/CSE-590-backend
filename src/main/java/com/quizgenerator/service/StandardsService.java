package com.quizgenerator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizgenerator.model.Standard;
import com.quizgenerator.repository.StandardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class StandardsService {
    
    private final StandardRepository standardRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public List<Standard> getAllStandards() {
        return standardRepository.findAll();
    }
    
    public Optional<Standard> getStandardByCode(String code) {
        return standardRepository.findByCode(code);
    }
    
    public List<Standard> getStandardsByTopic(String topic) {
        return standardRepository.findByTopicContaining(topic);
    }
    
    public List<Standard> getStandardsByDifficulty(Integer maxDifficulty) {
        return standardRepository.findByDifficultyLessThanEqual(maxDifficulty);
    }
    
    public List<Standard> getRecommendedSequence(String targetStandardCode) {
        Optional<Standard> targetStandard = standardRepository.findByCode(targetStandardCode);
        if (targetStandard.isEmpty()) {
            return List.of();
        }
        
        List<String> prerequisites = targetStandard.get().getPrerequisites();
        if (prerequisites == null || prerequisites.isEmpty()) {
            return List.of(targetStandard.get());
        }
        
        List<Standard> prerequisiteStandards = standardRepository.findByCodes(prerequisites);
        prerequisiteStandards.add(targetStandard.get());
        
        return prerequisiteStandards;
    }
    
    public void initializeStandards() {
        if (standardRepository.count() > 0) {
            log.info("Standards already initialized");
            return;
        }
        
        try {
            loadStandardsFromJson();
        } catch (Exception e) {
            log.warn("Failed to load standards from JSON, using sample standards: {}", e.getMessage());
            createSampleStandards();
        }
    }
    
    private void loadStandardsFromJson() throws IOException {
        ClassPathResource resource = new ClassPathResource("data/indiana_5th_grade_math_standards.json");
        
        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, Object> jsonData = objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> standardsData = (List<Map<String, Object>>) jsonData.get("standards");
            
            for (Map<String, Object> standardData : standardsData) {
                Standard standard = mapToStandard(standardData);
                standardRepository.save(standard);
            }
            
            log.info("Successfully loaded {} standards from JSON", standardsData.size());
        }
    }
    
    private Standard mapToStandard(Map<String, Object> standardData) {
        Standard standard = new Standard();
        standard.setCode((String) standardData.get("code"));
        standard.setTitle((String) standardData.get("title"));
        standard.setDescription((String) standardData.get("description"));
        standard.setSubSkills((List<String>) standardData.get("subSkills"));
        standard.setConcepts((List<String>) standardData.get("concepts"));
        standard.setPrerequisites((List<String>) standardData.get("prerequisites"));
        standard.setDifficulty((Integer) standardData.get("difficulty"));
        standard.setVocabulary((List<String>) standardData.get("vocabulary"));
        
        return standard;
    }
    
    private void createSampleStandards() {
        Standard standard1 = new Standard(
            "5.NS.1",
            "Place Value Understanding",
            "Understand the place value system through millions",
            Arrays.asList("Read whole numbers", "Write whole numbers", "Compare numbers"),
            Arrays.asList("place value", "number sense", "base-10"),
            Arrays.asList("4.NS.1"),
            2,
            Arrays.asList("place value", "digit", "standard form", "expanded form")
        );
        
        Standard standard2 = new Standard(
            "5.NS.2",
            "Powers of 10",
            "Understand powers of 10 and their relationships",
            Arrays.asList("Recognize patterns", "Multiply by powers of 10"),
            Arrays.asList("powers of 10", "patterns", "multiplication"),
            Arrays.asList("5.NS.1"),
            3,
            Arrays.asList("power", "exponent", "base", "pattern")
        );
        
        Standard standard3 = new Standard(
            "5.NS.3",
            "Decimal Place Value",
            "Read, write, and compare decimals to thousandths",
            Arrays.asList("Read decimals", "Write decimals", "Compare decimals"),
            Arrays.asList("decimal", "place value", "thousandths"),
            Arrays.asList("5.NS.1"),
            3,
            Arrays.asList("decimal", "decimal point", "tenths", "hundredths", "thousandths")
        );
        
        standardRepository.saveAll(Arrays.asList(standard1, standard2, standard3));
        log.info("Created sample standards");
    }
}