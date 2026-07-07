package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.EtatMachineHistorique;
import com.lear.MGCMS.services.EtatMachineHistoriqueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/etatMachineHistorique")
public class EtatMachineHistoriqueController {

    @Autowired
    private EtatMachineHistoriqueService service;

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public List<EtatMachineHistorique> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<EtatMachineHistorique> findById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/machine/{machine}")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public List<EtatMachineHistorique> findByMachine(@PathVariable String machine) {
        return service.findByMachine(machine);
    }

    @GetMapping("/byDateRange")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public List<EtatMachineHistorique> findByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return service.findByDateRangeOverlap(startDate, endDate);
    }

    @GetMapping("/listBetweenDate")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public List<EtatMachineHistorique> findListBetweenDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin) {
        return service.findListBetweenDate(dateDebut, dateFin);
    }

    @GetMapping("/machine/{machine}/byDateRange")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public List<EtatMachineHistorique> findByMachineAndDateRange(
            @PathVariable String machine,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return service.findByMachineAndDateRangeOverlap(machine, startDate, endDate);
    }

    @GetMapping("/machine/{machine}/active")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public List<EtatMachineHistorique> findActiveByMachine(@PathVariable String machine) {
        return service.findActiveByMachine(machine);
    }

    @GetMapping("/machine/{machine}/status")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<String> getStatusAtTime(
            @PathVariable String machine,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime) {
        return ResponseEntity.ok(service.getStatusCodeAtTime(machine, dateTime));
    }

    @GetMapping("/distinctMachines")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public List<String> findDistinctMachines() {
        return service.findDistinctMachines();
    }

    @GetMapping("/activeBreakdowns/count")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public Long countActiveBreakdowns() {
        return service.countActiveBreakdowns();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public EtatMachineHistorique create(@RequestBody EtatMachineHistorique entity, Authentication authentication) {
        String username = authentication.getName();
        return service.create(entity, username);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<EtatMachineHistorique> update(
            @PathVariable Long id,
            @RequestBody EtatMachineHistorique entity,
            Authentication authentication) {
        String username = authentication.getName();
        EtatMachineHistorique updated = service.update(id, entity, username);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<EtatMachineHistorique> close(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        EtatMachineHistorique closed = service.close(id, username);
        if (closed != null) {
            return ResponseEntity.ok(closed);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/closeWithDate")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<EtatMachineHistorique> closeWithDate(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {
        String username = authentication.getName();
        EtatMachineHistorique closed = service.closeWithDate(id, endDate, username);
        if (closed != null) {
            return ResponseEntity.ok(closed);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROCESS', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
