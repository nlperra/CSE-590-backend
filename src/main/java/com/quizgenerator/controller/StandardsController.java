package com.quizgenerator.controller;

import com.quizgenerator.model.Standard;
import com.quizgenerator.service.StandardsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/standards")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class StandardsController {
    
    private final StandardsService standardsService;
    
    @GetMapping
    public ResponseEntity<List<Standard>> getAllStandards() {
        List<Standard> standards = standardsService.getAllStandards();
        return ResponseEntity.ok(standards);
    }
    
    @GetMapping("/{code}")
    public ResponseEntity<Standard> getStandardByCode(@PathVariable String code) {
        Optional<Standard> standard = standardsService.getStandardByCode(code);
        return standard.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Standard>> searchStandardsByTopic(@RequestParam String topic) {
        List<Standard> standards = standardsService.getStandardsByTopic(topic);
        return ResponseEntity.ok(standards);
    }
    
    @GetMapping("/difficulty/{maxDifficulty}")
    public ResponseEntity<List<Standard>> getStandardsByDifficulty(@PathVariable Integer maxDifficulty) {
        List<Standard> standards = standardsService.getStandardsByDifficulty(maxDifficulty);
        return ResponseEntity.ok(standards);
    }
    
    @GetMapping("/{code}/sequence")
    public ResponseEntity<List<Standard>> getRecommendedSequence(@PathVariable String code) {
        List<Standard> sequence = standardsService.getRecommendedSequence(code);
        return ResponseEntity.ok(sequence);
    }
}