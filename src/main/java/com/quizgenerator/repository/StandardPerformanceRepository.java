package com.quizgenerator.repository;

import com.quizgenerator.model.StandardPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StandardPerformanceRepository extends JpaRepository<StandardPerformance, Long> {
    
    List<StandardPerformance> findByStudentId(String studentId);
    
    Optional<StandardPerformance> findByStudentIdAndStandardCode(String studentId, String standardCode);
    
    @Query("SELECT sp FROM StandardPerformance sp WHERE sp.studentId = :studentId ORDER BY sp.accuracy DESC")
    List<StandardPerformance> findByStudentIdOrderByAccuracyDesc(@Param("studentId") String studentId);
    
    @Query("SELECT sp FROM StandardPerformance sp WHERE sp.studentId = :studentId AND sp.masteryLevel = :masteryLevel")
    List<StandardPerformance> findByStudentIdAndMasteryLevel(@Param("studentId") String studentId, 
                                                            @Param("masteryLevel") StandardPerformance.MasteryLevel masteryLevel);
    
    @Query("SELECT sp FROM StandardPerformance sp WHERE sp.studentId = :studentId AND sp.masteryLevel IN :masteryLevels ORDER BY sp.lastAttempted DESC")
    List<StandardPerformance> findByStudentIdAndMasteryLevelIn(@Param("studentId") String studentId, 
                                                              @Param("masteryLevels") List<StandardPerformance.MasteryLevel> masteryLevels);
    
    @Query("SELECT COUNT(sp) FROM StandardPerformance sp WHERE sp.studentId = :studentId AND sp.masteryLevel = :masteryLevel")
    Long countByStudentIdAndMasteryLevel(@Param("studentId") String studentId, 
                                        @Param("masteryLevel") StandardPerformance.MasteryLevel masteryLevel);
}