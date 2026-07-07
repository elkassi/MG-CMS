package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.QualityValidationHistory;
import com.lear.MGCMS.domain.QualityValidationPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QualityValidationPatternRepository extends JpaRepository<QualityValidationPattern, Long>, JpaSpecificationExecutor<QualityValidationPattern> {

    /**
     * Find active patterns for a specific machine
     */
    List<QualityValidationPattern> findByMachineAndActiveTrue(String machine);

    /**
     * Find patterns that match placement using LIKE operator
     */
    @Query("SELECT qvp FROM QualityValidationPattern qvp WHERE qvp.machine = :machine AND qvp.active = true " +
           "AND (qvp.placement IS NULL OR LENGTH(qvp.placement) = 0 OR :placement LIKE qvp.placement)")
    List<QualityValidationPattern> findMatchingPlacementPatterns(@Param("machine") String machine, 
                                                                 @Param("placement") String placement);

    /**
     * Find patterns that match partNumberMaterial using LIKE operator
     */
    @Query("SELECT qvp FROM QualityValidationPattern qvp WHERE qvp.machine = :machine AND qvp.active = true " +
           "AND (qvp.partNumberMaterial IS NULL OR LENGTH(qvp.partNumberMaterial) = 0 OR :partNumberMaterial LIKE qvp.partNumberMaterial)")
    List<QualityValidationPattern> findMatchingPartNumberPatterns(@Param("machine") String machine, 
                                                                  @Param("partNumberMaterial") String partNumberMaterial);

    /**
     * Find patterns that match a pattern field using LIKE operator
     */
    @Query("SELECT qvp FROM QualityValidationPattern qvp WHERE (qvp.machine is null or LENGTH(qvp.machine) = 0 or qvp.machine = :machine) AND qvp.active = true " +
           "AND (qvp.pattern IS NULL OR LENGTH(qvp.pattern) = 0 OR :patternValue LIKE qvp.pattern)")
    List<QualityValidationPattern> findMatchingPatterns(@Param("machine") String machine, 
                                                        @Param("patternValue") String patternValue);

    /**
     * Comprehensive search that checks if any of the criteria match the patterns
     */
    @Query("SELECT DISTINCT qvp FROM QualityValidationPattern qvp WHERE (qvp.machine is null or LENGTH(qvp.machine) = 0 or qvp.machine = :machine) AND qvp.active = true " +
           "AND (" +
            "(qvp.placement IS NULL OR LENGTH(qvp.placement) = 0 OR :placement LIKE qvp.placement) " +
           "OR (qvp.partNumberMaterial IS NULL OR LENGTH(qvp.partNumberMaterial) = 0 OR :partNumberMaterial LIKE qvp.partNumberMaterial) " +
           "OR (qvp.pattern IS NULL OR LENGTH(qvp.pattern) = 0 OR :patternValue LIKE qvp.pattern))")
    List<QualityValidationPattern> findMatchingAnyPattern(@Param("machine") String machine,
                                                          @Param("placement") String placement,
                                                          @Param("partNumberMaterial") String partNumberMaterial,
                                                          @Param("patternValue") String patternValue);

    /**
     * Find patterns that match with AND logic between criteria
     */
    @Query("SELECT DISTINCT qvp FROM QualityValidationPattern qvp WHERE " +
           "(qvp.machine IS NULL OR LENGTH(qvp.machine) = 0 OR qvp.machine = :machine) " +
           "AND qvp.active = true " +
           "AND (qvp.placement IS NULL OR LENGTH(qvp.placement) = 0 OR :placement LIKE qvp.placement) " +
           "AND (qvp.partNumberMaterial IS NULL OR LENGTH(qvp.partNumberMaterial) = 0 OR :partNumberMaterial LIKE qvp.partNumberMaterial) " +
           "AND (qvp.pattern IS NULL OR LENGTH(qvp.pattern) = 0 OR :patternValue LIKE qvp.pattern) " +
           "AND (" +
           "    (qvp.placement IS NOT NULL AND LENGTH(qvp.placement) > 0) " +
           "    OR (qvp.partNumberMaterial IS NOT NULL AND LENGTH(qvp.partNumberMaterial) > 0) " +
           "    OR (qvp.pattern IS NOT NULL AND LENGTH(qvp.pattern) > 0)" +
           ")")
    List<QualityValidationPattern> findMatchingPattern(@Param("machine") String machine,
                                                       @Param("placement") String placement,
                                                       @Param("partNumberMaterial") String partNumberMaterial,
                                                       @Param("patternValue") String patternValue);

    /**
     * Find active patterns for a specific machine and application type
     */
    @Query("SELECT qvp FROM QualityValidationPattern qvp WHERE qvp.machine = :machine AND qvp.active = true " +
           "AND (qvp.applicationType = :applicationType OR qvp.applicationType = 'BOTH')")
    List<QualityValidationPattern> findByMachineAndApplicationTypeAndActiveTrue(@Param("machine") String machine,
                                                                                 @Param("applicationType") String applicationType);

    /**
     * Find patterns that match with AND logic, filtered by application type
     */
    @Query("SELECT DISTINCT qvp FROM QualityValidationPattern qvp WHERE " +
           "(qvp.machine IS NULL OR LENGTH(qvp.machine) = 0 OR qvp.machine = :machine) " +
           "AND qvp.active = true " +
           "AND (qvp.applicationType = :applicationType OR qvp.applicationType = 'BOTH') " +
           "AND (qvp.placement IS NULL OR LENGTH(qvp.placement) = 0 OR :placement LIKE qvp.placement) " +
           "AND (qvp.partNumberMaterial IS NULL OR LENGTH(qvp.partNumberMaterial) = 0 OR :partNumberMaterial LIKE qvp.partNumberMaterial) " +
           "AND (qvp.pattern IS NULL OR LENGTH(qvp.pattern) = 0 OR :patternValue LIKE qvp.pattern) " +
           "AND (" +
           "    (qvp.placement IS NOT NULL AND LENGTH(qvp.placement) > 0) " +
           "    OR (qvp.partNumberMaterial IS NOT NULL AND LENGTH(qvp.partNumberMaterial) > 0) " +
           "    OR (qvp.pattern IS NOT NULL AND LENGTH(qvp.pattern) > 0)" +
           ")")
    List<QualityValidationPattern> findMatchingPatternWithApplicationType(@Param("machine") String machine,
                                                              @Param("placement") String placement,
                                                              @Param("partNumberMaterial") String partNumberMaterial,
                                                              @Param("patternValue") String patternValue,
                                                              @Param("applicationType") String applicationType);
}
