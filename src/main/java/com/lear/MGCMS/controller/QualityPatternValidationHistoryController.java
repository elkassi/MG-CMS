package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.QualityPatternValidationHistory;
import com.lear.MGCMS.domain.QualityValidationHistory;
import com.lear.MGCMS.services.QualityPatternValidationHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/qualityPatternValidationHistory")
public class QualityPatternValidationHistoryController {

    @Autowired
    private QualityPatternValidationHistoryService service;


    @GetMapping("/all")
    public Page<QualityPatternValidationHistory> findAll(
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
     * Get all validation history
     */
    @GetMapping
    public List<QualityPatternValidationHistory> getAllValidationHistory() {
        return service.findAll();
    }

    /**
     * Get validation history by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<QualityPatternValidationHistory> getValidationHistoryById(@PathVariable Long id) {
        Optional<QualityPatternValidationHistory> validation = service.findById(id);
        return validation.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Check if a serie is already validated on a machine
     */
    @GetMapping("/checkValidated")
    public ResponseEntity<Boolean> checkIfValidated(@RequestParam String serie, @RequestParam String machine) {
        boolean validated = service.isAlreadyValidated(serie, machine);
        return ResponseEntity.ok(validated);
    }

    /**
     * Get validation history for a serie and machine
     */
    @GetMapping("/getValidation")
    public ResponseEntity<QualityPatternValidationHistory> getValidationHistory(@RequestParam String serie, 
                                                                               @RequestParam String machine) {
        Optional<QualityPatternValidationHistory> validation = service.getValidationHistory(serie, machine);
        return validation.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all validations for a serie
     */
    @GetMapping("/serie/{serie}")
    public List<QualityPatternValidationHistory> getValidationsBySerie(@PathVariable String serie) {
        return service.getValidationsBySerie(serie);
    }

    /**
     * Get validations by machine and date range
     */
    @GetMapping("/machine/{machine}")
    public List<QualityPatternValidationHistory> getValidationsByMachineAndDateRange(
            @PathVariable String machine,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        return service.getValidationsByMachineAndDateRange(machine, startDate, endDate);
    }

    /**
     * Get recent validations
     */
    @GetMapping("/recent")
    public List<QualityPatternValidationHistory> getRecentValidations(@RequestParam(defaultValue = "7") int days) {
        return service.getRecentValidations(days);
    }

    /**
     * Create a new validation record
     */
    @PostMapping
    public QualityPatternValidationHistory createValidation(@RequestBody QualityPatternValidationHistory validation) {
        return service.save(validation);
    }

    /**
     * Create validation with parameters
     */
    @PostMapping("/validate")
    public ResponseEntity<QualityPatternValidationHistory> validateSerie(
            @RequestParam String serie,
            @RequestParam(required = false) String machine,
            @RequestParam(required = false) String placement,
            @RequestParam(required = false) String partNumberMaterial,
            @RequestParam(required = false) String pattern,
            @RequestParam(required = false) String validatedBy,
            @RequestParam(required = false) String qualityCode,
            @RequestParam(required = false) String comments) {

        QualityPatternValidationHistory validation = service.createValidation(
            serie, machine, placement, partNumberMaterial, pattern, validatedBy, qualityCode, comments);

        return ResponseEntity.ok(validation);
    }

    /**
     * Delete validation record
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteValidation(@PathVariable Long id) {
        if (service.findById(id).isPresent()) {
            service.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
