package com.lear.MGCMS.controller;

import com.lear.MGCMS.domain.CtcToleranceRule;
import com.lear.MGCMS.domain.QualityPatternValidationHistory;
import com.lear.MGCMS.services.CtcToleranceRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ctcToleranceRule")
public class CtcToleranceRuleController {

    @Autowired
    private CtcToleranceRuleService service;

    @GetMapping("/all")
    public Page<CtcToleranceRule> findAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size,
            @RequestParam(value = "sort", defaultValue = "priority,desc", required = false) String sortBy
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
     * Get all rules
     */
    @GetMapping
    public List<CtcToleranceRule> getAllRules() {
        return service.findAll();
    }

    /**
     * Get active rules only
     */
    @GetMapping("/active")
    public List<CtcToleranceRule> getActiveRules() {
        return service.findAllActive();
    }

    /**
     * Get rule by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CtcToleranceRule> getRuleById(@PathVariable Long id) {
        Optional<CtcToleranceRule> rule = service.findById(id);
        return rule.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get matching rule for projet, type, and height
     */
    @GetMapping("/match")
    public ResponseEntity<CtcToleranceRule> getMatchingRule(
            @RequestParam String projet,
            @RequestParam String type,
            @RequestParam Double height) {
        CtcToleranceRule rule = service.findMatchingRule(projet, type, height);
        return rule != null ? ResponseEntity.ok(rule) : ResponseEntity.notFound().build();
    }

    /**
     * Get tolerances for a specific height
     */
    @GetMapping("/tolerances")
    public double[] getTolerances(
            @RequestParam String projet,
            @RequestParam String type,
            @RequestParam Double height) {
        return service.getTolerances(projet, type, height);
    }

    /**
     * Create a new rule
     */
    @PostMapping
    @PreAuthorize("hasRole('ENGINEERING')")
    public CtcToleranceRule createRule(@RequestBody CtcToleranceRule rule) {
        return service.save(rule);
    }

    /**
     * Update a rule
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ENGINEERING')")
    public ResponseEntity<CtcToleranceRule> updateRule(@PathVariable Long id, @RequestBody CtcToleranceRule rule) {
        Optional<CtcToleranceRule> existingRule = service.findById(id);
        if (existingRule.isPresent()) {
            rule.setId(id);
            return ResponseEntity.ok(service.save(rule));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Delete a rule
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ENGINEERING')")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        if (service.findById(id).isPresent()) {
            service.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Apply a specific rule to CTC Files database
     */
    @PostMapping("/{id}/apply")
    @PreAuthorize("hasRole('ENGINEERING')")
    public ResponseEntity<Map<String, Object>> applyRule(@PathVariable Long id) {
        Optional<CtcToleranceRule> ruleOpt = service.findById(id);
        if (!ruleOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        CtcToleranceRule rule = ruleOpt.get();
        if (!Boolean.TRUE.equals(rule.getActive())) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Cette règle est désactivée");
            return ResponseEntity.ok(result);
        }
        Map<String, Object> result = service.applyRuleToCTC(rule);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Apply all active rules to CTC Files database
     */
    @PostMapping("/apply-all")
    @PreAuthorize("hasRole('ENGINEERING')")
    public ResponseEntity<Map<String, Object>> applyAllRules() {
        Map<String, Object> result = service.applyAllActiveRulesToCTC();
        return ResponseEntity.ok(result);
    }
}
