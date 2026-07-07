package com.lear.MGCMS.repositories;

import com.lear.MGCMS.domain.CapaciteInstalleeRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CapaciteInstalleeRuleRepository extends JpaRepository<CapaciteInstalleeRule, Long> {

    /**
     * Candidate rules whose validity interval contains {@code date} and whose
     * groupe is null (any) or matches. {@code dayOfWeek} / {@code shiftNumber}
     * are matched in Java by the service (cheap on this small result set), which
     * keeps the query trivial and index-friendly.
     */
    @Query("SELECT r FROM CapaciteInstalleeRule r WHERE "
         + "(r.dateDebut IS NULL OR r.dateDebut <= :date) AND "
         + "(r.dateFin IS NULL OR r.dateFin >= :date) AND "
         + "(r.groupe IS NULL OR r.groupe = :groupe)")
    List<CapaciteInstalleeRule> findCandidates(@Param("date") LocalDate date,
                                               @Param("groupe") String groupe);
}
