package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.EtatMachineHistorique;
import com.lear.MGCMS.repositories.EtatMachineHistoriqueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EtatMachineHistoriqueService {

    @Autowired
    private EtatMachineHistoriqueRepository repository;

    public List<EtatMachineHistorique> findAll() {
        return repository.findAll();
    }

    /**
     * Batch get current status codes for ALL machines at a point in time.
     * Returns Map: machine name -> codeEtat. Single query instead of N per-machine queries.
     */
    public Map<String, String> getAllCurrentStatusCodes(LocalDateTime dateTime) {
        Map<String, String> result = new HashMap<>();
        List<Object[]> rows = repository.findAllCurrentStatuses(dateTime);
        for (Object[] row : rows) {
            result.put((String) row[0], (String) row[1]);
        }
        return result;
    }

    public Optional<EtatMachineHistorique> findById(Long id) {
        return repository.findById(id);
    }

    public List<EtatMachineHistorique> findByMachine(String machine) {
        return repository.findByMachine(machine);
    }

    public List<EtatMachineHistorique> findByDateRangeOverlap(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        return repository.findByDateRangeOverlap(rangeStart, rangeEnd);
    }

    public List<EtatMachineHistorique> findListBetweenDate(LocalDateTime dateDebut, LocalDateTime dateFin) {
        return repository.findListBetweenDate(dateDebut, dateFin);
    }

    public List<EtatMachineHistorique> findByMachineAndDateRangeOverlap(String machine, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        return repository.findByMachineAndDateRangeOverlap(machine, rangeStart, rangeEnd);
    }

    public List<EtatMachineHistorique> findActiveByMachine(String machine) {
        return repository.findActiveByMachine(machine);
    }

    /**
     * Get the status of a machine at a specific point in time.
     * Returns null if no status is found (meaning machine is in default state).
     */
    public EtatMachineHistorique getStatusAtTime(String machine, LocalDateTime dateTime) {
        List<EtatMachineHistorique> statuses = repository.findByMachineAndDateTime(machine, dateTime);
        return statuses.isEmpty() ? null : statuses.get(0);
    }

    /**
     * Get the status code of a machine at a specific point in time.
     * Returns "M" (Marche) as default if no status is found.
     */
    public String getStatusCodeAtTime(String machine, LocalDateTime dateTime) {
        EtatMachineHistorique status = getStatusAtTime(machine, dateTime);
        return status != null ? status.getCodeEtat() : "M";
    }

    @Transactional
    public EtatMachineHistorique save(EtatMachineHistorique entity) {
        return repository.save(entity);
    }

    @Transactional
    public EtatMachineHistorique create(EtatMachineHistorique entity, String username) {
        entity.setCreatedBy(username);
        entity.setCreatedAt(LocalDateTime.now());
        return repository.save(entity);
    }

    @Transactional
    public EtatMachineHistorique update(Long id, EtatMachineHistorique entity, String username) {
        Optional<EtatMachineHistorique> existing = repository.findById(id);
        if (existing.isPresent()) {
            EtatMachineHistorique toUpdate = existing.get();
            toUpdate.setMachine(entity.getMachine());
            toUpdate.setStartDate(entity.getStartDate());
            toUpdate.setEndDate(entity.getEndDate());
            toUpdate.setCodeEtat(entity.getCodeEtat());
            toUpdate.setCause(entity.getCause());
            toUpdate.setAction(entity.getAction());
            toUpdate.setUpdatedBy(username);
            toUpdate.setUpdatedAt(LocalDateTime.now());
            return repository.save(toUpdate);
        }
        return null;
    }

    @Transactional
    public EtatMachineHistorique close(Long id, String username) {
        Optional<EtatMachineHistorique> existing = repository.findById(id);
        if (existing.isPresent()) {
            EtatMachineHistorique toClose = existing.get();
            toClose.setEndDate(LocalDateTime.now());
            toClose.setClosedBy(username);
            toClose.setClosedAt(LocalDateTime.now());
            return repository.save(toClose);
        }
        return null;
    }

    @Transactional
    public EtatMachineHistorique closeWithDate(Long id, LocalDateTime endDate, String username) {
        Optional<EtatMachineHistorique> existing = repository.findById(id);
        if (existing.isPresent()) {
            EtatMachineHistorique toClose = existing.get();
            toClose.setEndDate(endDate);
            toClose.setClosedBy(username);
            toClose.setClosedAt(LocalDateTime.now());
            return repository.save(toClose);
        }
        return null;
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public List<String> findDistinctMachines() {
        return repository.findDistinctMachines();
    }

    public Long countActiveBreakdowns() {
        return repository.countActiveBreakdowns();
    }
}
