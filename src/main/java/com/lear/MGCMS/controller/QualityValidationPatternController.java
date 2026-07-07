package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.QualityPatternValidationHistory;
import com.lear.MGCMS.domain.QualityValidationPattern;
import com.lear.MGCMS.services.QualityValidationPatternService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/qualityValidationPattern")
public class QualityValidationPatternController {

    @Autowired
    private QualityValidationPatternService service;

    @GetMapping("/all")
    public Page<QualityValidationPattern> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "date,desc", required = false) String sortBy
    ) {
        if (filters == null) {
            filters = new HashMap<>();
        }
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        return service.findAll(filters, page,size,sortBy);
    }


    /**
     * Get all patterns
     */
    @GetMapping
    public List<QualityValidationPattern> getAllPatterns() {
        return service.findAll();
    }

    /**
     * Get pattern by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<QualityValidationPattern> getPatternById(@PathVariable Long id) {
        Optional<QualityValidationPattern> pattern = service.findById(id);
        return pattern.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get active patterns for a machine
     */
    @GetMapping("/machine/{machine}")
    public List<QualityValidationPattern> getActivePatternsByMachine(
            @PathVariable String machine,
            @RequestParam(required = false) String applicationType) {
        if (applicationType != null && !applicationType.isEmpty()) {
            return service.findActivePatternsByMachineAndType(machine, applicationType);
        }
        return service.findActivePatternsByMachine(machine);
    }

    /**
     * Check if quality validation is required
     */
    @GetMapping("/checkValidationRequired")
    public ResponseEntity<Boolean> checkValidationRequired(
            @RequestParam String machine,
            @RequestParam(required = false) String placement,
            @RequestParam(required = false) String partNumberMaterial,
            @RequestParam(required = false) String patterns,
            @RequestParam(required = false, defaultValue = "BOTH") String applicationType) {
        
        boolean required = service.isQualityValidationRequired(machine, placement, partNumberMaterial, patterns, applicationType);
        return ResponseEntity.ok(required);
    }

    /**
     * Get matching patterns for debugging
     */
    @GetMapping("/getMatchingPatterns")
    public List<QualityValidationPattern> getMatchingPatterns(
            @RequestParam String machine,
            @RequestParam(required = false) String placement,
            @RequestParam(required = false) String partNumberMaterial,
            @RequestParam(required = false) String pattern,
            @RequestParam(required = false, defaultValue = "BOTH") String applicationType) {
        
        return service.getMatchingPatterns(machine, placement, partNumberMaterial, pattern, applicationType);
    }

    /**
     * Create a new pattern
     */
    @PostMapping
    public QualityValidationPattern createPattern(@RequestBody QualityValidationPattern pattern, 
                                                   Authentication authentication) {
        if (authentication != null) {
            pattern.setCreatedBy(authentication.getName());
            pattern.setUpdatedBy(authentication.getName());
        }
        return service.save(pattern);
    }

    /**
     * Update a pattern
     */
    @PutMapping("/{id}")
    public ResponseEntity<QualityValidationPattern> updatePattern(@PathVariable Long id, 
                                                                 @RequestBody QualityValidationPattern pattern,
                                                                 Authentication authentication) {
        Optional<QualityValidationPattern> existingPattern = service.findById(id);
        if (existingPattern.isPresent()) {
            pattern.setId(id);
            // Preserve createdAt and createdBy from existing record
            pattern.setCreatedAt(existingPattern.get().getCreatedAt());
            pattern.setCreatedBy(existingPattern.get().getCreatedBy());
            if (authentication != null) {
                pattern.setUpdatedBy(authentication.getName());
            }
            return ResponseEntity.ok(service.save(pattern));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Update pattern active status
     */
    @PutMapping("/{id}/active")
    public ResponseEntity<QualityValidationPattern> updateActiveStatus(@PathVariable Long id, 
                                                                       @RequestParam boolean active) {
        QualityValidationPattern updated = service.updateActiveStatus(id, active);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    /**
     * Delete a pattern
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePattern(@PathVariable Long id) {
        if (service.findById(id).isPresent()) {
            service.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
