package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.dispatcher.DispatchAudit;
import com.lear.MGCMS.repositories.dispatcher.DispatchAuditRepository;

/**
 * Single writer of {@code dispatch_audit} rows. Every dispatcher mutation
 * goes through {@link #write(String, String, String, String, DispatchAudit.Trigger, String)}
 * so the audit trail is uniform.
 */
@Service
public class DispatchAuditService {

    @Autowired
    private DispatchAuditRepository repository;

    /** Write one audit row. Returns the persisted entity. */
    @Transactional
    public DispatchAudit write(String sequence,
                               String fromZone,
                               String toZone,
                               String reason,
                               DispatchAudit.Trigger trigger,
                               String matricule) {
        DispatchAudit row = new DispatchAudit(
                sequence, fromZone, toZone, reason, trigger, matricule);
        return repository.save(row);
    }

    @Transactional(readOnly = true)
    public List<DispatchAudit> recent(int limit) {
        int safe = Math.max(1, Math.min(limit, 500));
        return repository.findLatest(PageRequest.of(0, safe));
    }

    @Transactional(readOnly = true)
    public List<DispatchAudit> bySequence(String sequence) {
        return repository.findBySequenceOrderByCreatedAtDesc(sequence);
    }

    @Transactional(readOnly = true)
    public List<DispatchAudit> since(LocalDateTime cutoff) {
        return repository.findSince(cutoff);
    }
}
