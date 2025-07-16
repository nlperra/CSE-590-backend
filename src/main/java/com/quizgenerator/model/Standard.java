package com.quizgenerator.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.List;

@Entity
@Table(name = "standards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Standard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String code;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ElementCollection
    @CollectionTable(name = "standard_sub_skills", joinColumns = @JoinColumn(name = "standard_id"))
    @Column(name = "sub_skill")
    private List<String> subSkills;
    
    @ElementCollection
    @CollectionTable(name = "standard_concepts", joinColumns = @JoinColumn(name = "standard_id"))
    @Column(name = "concept")
    private List<String> concepts;
    
    @ElementCollection
    @CollectionTable(name = "standard_prerequisites", joinColumns = @JoinColumn(name = "standard_id"))
    @Column(name = "prerequisite")
    private List<String> prerequisites;
    
    @Column(nullable = false)
    private Integer difficulty;
    
    @ElementCollection
    @CollectionTable(name = "standard_vocabulary", joinColumns = @JoinColumn(name = "standard_id"))
    @Column(name = "vocabulary_term")
    private List<String> vocabulary;

    public Standard(String code, String title, String description,
                   List<String> subSkills, List<String> concepts,
                   List<String> prerequisites, Integer difficulty,
                   List<String> vocabulary) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.subSkills = subSkills;
        this.concepts = concepts;
        this.prerequisites = prerequisites;
        this.difficulty = difficulty;
        this.vocabulary = vocabulary;
    }
}