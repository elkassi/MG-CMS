package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.CapaciteInstallee;
import com.lear.MGCMS.domain.CapaciteInstalleeRule;
import com.lear.MGCMS.services.CapaciteInstalleeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/capaciteInstallee")
public class CapaciteInstalleeController {

    @Autowired
    private CapaciteInstalleeService capaciteInstalleeService;

    @GetMapping("/all")
    public Page<CapaciteInstallee> findAll(
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "id,desc", required = false) String sortBy
    ) {
        String[] sortParts = sortBy.split(",");
        String sortProp = sortParts[0];
        Sort.Direction sortDir = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return capaciteInstalleeService.findAll(PageRequest.of(page, size, Sort.by(sortDir, sortProp)));
    }

    @GetMapping("/list")
    public List<CapaciteInstallee> list() {
        return capaciteInstalleeService.findAll();
    }

    @GetMapping("/byDateRange")
    public List<CapaciteInstallee> findByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return capaciteInstalleeService.findByDateRange(startDate, endDate);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CapaciteInstallee> findById(@PathVariable Long id) {
        Optional<CapaciteInstallee> entry = capaciteInstalleeService.findById(id);
        return entry.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CapaciteInstallee> create(@RequestBody CapaciteInstallee capaciteInstallee) {
        if (capaciteInstallee.getGroupe() == null || capaciteInstallee.getCapaciteInstallee() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(capaciteInstalleeService.save(capaciteInstallee));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CapaciteInstallee> update(@PathVariable Long id, @RequestBody CapaciteInstallee capaciteInstallee) {
        Optional<CapaciteInstallee> existing = capaciteInstalleeService.findById(id);
        if (existing.isPresent()) {
            capaciteInstallee.setId(id);
            return ResponseEntity.ok(capaciteInstalleeService.save(capaciteInstallee));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Optional<CapaciteInstallee> existing = capaciteInstalleeService.findById(id);
        if (existing.isPresent()) {
            capaciteInstalleeService.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/ensureNextTwoDays")
    public ResponseEntity<?> ensureNextTwoDays() {
        capaciteInstalleeService.ensureNextTwoDays();
        return ResponseEntity.ok().build();
    }

    // ----------------------------------------------------------------- effective

    /** Resolved capacity per (date, shift, groupe) for the range (explicit row → rule → default). */
    @GetMapping("/effectiveByDateRange")
    public List<CapaciteInstallee> effectiveByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return capaciteInstalleeService.getEffectiveForRange(startDate, endDate);
    }

    // ----------------------------------------------------------------- rules CRUD

    @GetMapping("/rules")
    public List<CapaciteInstalleeRule> listRules() {
        return capaciteInstalleeService.findAllRules();
    }

    @PostMapping("/rules")
    public ResponseEntity<CapaciteInstalleeRule> createRule(@RequestBody CapaciteInstalleeRule rule) {
        rule.setId(null);
        return ResponseEntity.ok(capaciteInstalleeService.saveRule(rule));
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<CapaciteInstalleeRule> updateRule(@PathVariable Long id, @RequestBody CapaciteInstalleeRule rule) {
        if (capaciteInstalleeService.findRuleById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        rule.setId(id);
        return ResponseEntity.ok(capaciteInstalleeService.saveRule(rule));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<?> deleteRule(@PathVariable Long id) {
        if (capaciteInstalleeService.findRuleById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        capaciteInstalleeService.deleteRule(id);
        return ResponseEntity.ok().build();
    }
}
