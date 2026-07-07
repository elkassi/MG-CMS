package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.QualityPatternValidationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QualityPatternValidationHistoryRepository extends JpaRepository<QualityPatternValidationHistory, Long>, JpaSpecificationExecutor<QualityPatternValidationHistory> {

    /**
     * Check if a serie has already been validated on a specific machine
     */
    boolean existsBySerieAndMachine(String serie, String machine);

    /**
     * Find validation history for a specific serie and machine
     */
    Optional<QualityPatternValidationHistory> findBySerieAndMachine(String serie, String machine);

    /**
     * Find all validations for a serie
     */
    List<QualityPatternValidationHistory> findBySerie(String serie);

    /**
     * Find validations by machine within a date range
     */
    @Query("SELECT qpvh FROM QualityPatternValidationHistory qpvh WHERE qpvh.machine = :machine " +
           "AND qpvh.validationDate BETWEEN :startDate AND :endDate")
    List<QualityPatternValidationHistory> findByMachineAndDateRange(@Param("machine") String machine,
                                                                    @Param("startDate") LocalDateTime startDate,
                                                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Find validations by validated user
     */
    List<QualityPatternValidationHistory> findByValidatedBy(String validatedBy);

    /**
     * Find recent validations (last N days)
     */
    @Query("SELECT qpvh FROM QualityPatternValidationHistory qpvh WHERE qpvh.validationDate >= :sinceDate ORDER BY qpvh.validationDate DESC")
    List<QualityPatternValidationHistory> findRecentValidations(@Param("sinceDate") LocalDateTime sinceDate);
}
