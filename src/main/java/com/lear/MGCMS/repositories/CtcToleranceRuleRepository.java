package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.CtcToleranceRule;
import com.lear.MGCMS.domain.QualityPatternValidationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CtcToleranceRuleRepository extends JpaRepository<CtcToleranceRule, Long>, JpaSpecificationExecutor<CtcToleranceRule> {

    /**
     * Find active rules ordered by priority
     */
    List<CtcToleranceRule> findByActiveTrueOrderByPriorityDesc();

    /**
     * Find rules for a specific projet and type
     */
    @Query("SELECT r FROM CtcToleranceRule r WHERE r.active = true " +
           "AND (r.projet IS NULL OR r.projet = :projet) " +
           "AND (r.type IS NULL OR r.type = :type) " +
           "ORDER BY r.priority DESC")
    List<CtcToleranceRule> findMatchingRules(@Param("projet") String projet, @Param("type") String type);

    /**
     * Find rules that match height range for a projet and type
     */
    @Query("SELECT r FROM CtcToleranceRule r WHERE r.active = true " +
           "AND (r.projet IS NULL OR r.projet = :projet) " +
           "AND (r.type IS NULL OR r.type = :type) " +
           "AND (r.heightMin IS NULL OR r.heightMin <= :height) " +
           "AND (r.heightMax IS NULL OR r.heightMax > :height) " +
           "ORDER BY r.priority DESC")
    List<CtcToleranceRule> findMatchingRulesForHeight(@Param("projet") String projet, 
                                                       @Param("type") String type, 
                                                       @Param("height") Double height);

    /**
     * Find rules by projet
     */
    List<CtcToleranceRule> findByProjetAndActiveTrue(String projet);
}
