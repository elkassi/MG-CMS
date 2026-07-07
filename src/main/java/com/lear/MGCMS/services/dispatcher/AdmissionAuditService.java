package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.dispatcher.AdmissionBlockedAudit;
import com.lear.MGCMS.domain.dispatcher.AdmissionBlockedAudit.ReasonCode;
import com.lear.MGCMS.repositories.dispatcher.AdmissionBlockedAuditRepository;

/**
 * Phase 9 — admission audit writer split out of {@link AdmissionService}
 * so the audit insert runs in its own transaction.
 *
 * <p>Decoupled per code review C3: with the audit save in
 * {@link Propagation#REQUIRES_NEW} a transient failure (locked row,
 * constraint hiccup) no longer rolls back the operator's admission
 * decision. Failures are caught and logged at {@code WARN} so the
 * shopfloor never sees an HTTP 500 over a logging mishap.</p>
 */
@Service
public class AdmissionAuditService {

    private static final Logger log = LoggerFactory.getLogger(AdmissionAuditService.class);

    @Autowired private AdmissionBlockedAuditRepository auditRepository;

    /**
     * Persist a single {@link AdmissionBlockedAudit} row. Runs in its
     * own transaction so audit availability never blocks admission.
     * Swallows {@link DataAccessException} on purpose — admission is
     * the production-critical path; audit is best-effort.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordBlock(SerieDispatchInfo serie, String zoneNom, LocalDate date, int shift,
                            ReasonCode rc, String detail, String requestedByMatricule) {
        try {
            auditRepository.save(new AdmissionBlockedAudit(
                    serie.getSerieId(), zoneNom, date, shift, rc, detail, requestedByMatricule));
        } catch (DataAccessException ex) {
            log.warn("AdmissionAudit save failed (decision unaffected): serie={} zone={} reason={} detail={}",
                     serie.getSerieId(), zoneNom, rc, detail, ex);
        }
    }
}
