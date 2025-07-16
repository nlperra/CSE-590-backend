package com.quizgenerator.repository;

import com.quizgenerator.model.Standard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StandardRepository extends JpaRepository<Standard, Long> {
    
    Optional<Standard> findByCode(String code);
    
    @Query("SELECT s FROM Standard s JOIN s.concepts c WHERE LOWER(c) LIKE LOWER(CONCAT('%', :topic, '%'))")
    List<Standard> findByTopicContaining(@Param("topic") String topic);
    
    List<Standard> findByDifficultyLessThanEqual(Integer difficulty);
    
    List<Standard> findByDifficultyBetween(Integer minDifficulty, Integer maxDifficulty);
    
    @Query("SELECT s FROM Standard s WHERE s.code IN :codes")
    List<Standard> findByCodes(@Param("codes") List<String> codes);
}