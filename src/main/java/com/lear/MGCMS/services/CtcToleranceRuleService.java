package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.CtcToleranceRule;
import com.lear.MGCMS.domain.CtcToleranceRule;
import com.lear.MGCMS.repositories.CtcToleranceRuleRepository;
import com.lear.MGCMS.services.ctc.FilesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CtcToleranceRuleService {

    @Autowired
    private CtcToleranceRuleRepository repository;
    
    @Autowired
    private FilesService filesService;
    
    @Value("${lear.pltfolder}")
    private String pltfolder;

    private Sort.Direction replaceOrderStringThroughDirection(String sortDirection) {
        if (sortDirection.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        } else {
            return Sort.Direction.ASC;
        }
    }


    public Page<CtcToleranceRule> findAll(Map<String, String> filters, int page, int size, String sort) {
        String[] sortArr = sort.split(",");
        String evalSort = sortArr[0];
        String sortDirection = sortArr[1];
        Sort.Direction evalDirection = replaceOrderStringThroughDirection(sortDirection);
        Sort sortOrderIgnoreCase = Sort.by(new Sort.Order(evalDirection, evalSort).ignoreCase());

        Specification<CtcToleranceRule> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Add filters based on the key-value pairs in the 'filters' map
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                System.out.println(entry.getKey() + " : "+ entry.getValue());
                String[] strArr = entry.getKey().split("\\.");

                if(strArr.length >= 2) {
                    Path<String> path = root.get(strArr[1]);
                    for(int i = 2; i < strArr.length; i++) {
                        path = path.get(strArr[i]);
                    }

                    // Handle different data types
                    if (path.getJavaType().equals(String.class)) {
                        System.out.println("String");
                        if (entry.getKey().startsWith("startWith.")) {
                            predicates.add(builder.like(path.as(String.class), entry.getValue() + "%"));
                        } else if (entry.getKey().startsWith("endWith.")) {
                            predicates.add(builder.like(path.as(String.class), "%" + entry.getValue()));
                        } else if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(String.class), entry.getValue()));
                        } else if (entry.getKey().startsWith("notEqual.")) {
                            predicates.add(builder.notEqual(path.as(String.class), entry.getValue()));
                        } else if (entry.getKey().startsWith("contains.")) {
                            predicates.add(builder.like(path.as(String.class), "%" + entry.getValue() + "%"));
                        }
                    } else if (path.getJavaType().equals(Boolean.class)) {
                        System.out.println("Boolean");
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Boolean.class), Boolean.parseBoolean(entry.getValue())));
                        } else if (entry.getKey().startsWith("notEqual.")) {
                            predicates.add(builder.notEqual(path.as(Boolean.class), Boolean.parseBoolean(entry.getValue())));
                        }
                    } else if (path.getJavaType().equals(LocalDate.class)) {
                        System.out.println("LocalDate");
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(LocalDate.class), LocalDate.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(LocalDate.class), LocalDate.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(LocalDate.class), LocalDate.parse(entry.getValue())));
                        }
//                        else if (entry.getKey().startsWith("startWith.")) {
//                            LocalDateTime startDateTime = LocalDateTime.parse(entry.getValue(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS".substring(0, entry.getValue().length())));
//                            Integer lg = entry.getValue().length() ;
//                            LocalDateTime endDateTime = startDateTime.plusDays(1).minusNanos(1);
//                            switch(lg) {
//                            	case 1 :
//                            		endDateTime = startDateTime.plusYears(1000).minusNanos(1);break;
//                            	case 2 :
//                            		endDateTime = startDateTime.plusYears(100).minusNanos(1);break;
//                            	case 3:
//                            		endDateTime = startDateTime.plusYears(10).minusNanos(1);break;
//
//                            }
//                            predicates.add(builder.between(path.as(LocalDateTime.class), startDateTime, endDateTime));
//                        }
                    } else if (path.getJavaType().equals(LocalDateTime.class)) {
                        System.out.println("LocalDateTime");
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(LocalDateTime.class), LocalDateTime.parse(entry.getValue())));
                        } else if (entry.getKey().startsWith("startWith.")) {
                            LocalDateTime startDateTime = LocalDateTime.parse(entry.getValue());
                            LocalDateTime endDateTime = startDateTime.plusDays(1).minusNanos(1);
                            predicates.add(builder.between(path.as(LocalDateTime.class), startDateTime, endDateTime));
                        }
                    } else if (path.getJavaType().equals(Integer.class)) {
                        // Handle Integer conditions
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Integer.class), Integer.parseInt(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(Integer.class), Integer.parseInt(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(Integer.class), Integer.parseInt(entry.getValue())));
                        }
                    } else if (path.getJavaType().equals(Double.class)) {
                        // Handle Double conditions
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Double.class), Double.parseDouble(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(Double.class), Double.parseDouble(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(Double.class), Double.parseDouble(entry.getValue())));
                        }
                    } else if (path.getJavaType().equals(Long.class)) {
                        // Handle Long conditions
                        if (entry.getKey().startsWith("equal.")) {
                            predicates.add(builder.equal(path.as(Long.class), Long.parseLong(entry.getValue())));
                        } else if (entry.getKey().startsWith("greaterThan.")) {
                            predicates.add(builder.greaterThan(path.as(Long.class), Long.parseLong(entry.getValue())));
                        } else if (entry.getKey().startsWith("lessThan.")) {
                            predicates.add(builder.lessThan(path.as(Long.class), Long.parseLong(entry.getValue())));
                        }
                    }

                    if(entry.getKey().startsWith("isNull.")) {
                        predicates.add(builder.isNull(path));
                    } else if(entry.getKey().startsWith("isNotNull.")) {
                        predicates.add(builder.isNotNull(path));
                    }

                }
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };

        return repository.findAll(specification, PageRequest.of(page, size, sortOrderIgnoreCase));
    }


    /**
     * Save a tolerance rule
     */
    public CtcToleranceRule save(CtcToleranceRule rule) {
        return repository.save(rule);
    }

    /**
     * Find all active rules
     */
    public List<CtcToleranceRule> findAllActive() {
        return repository.findByActiveTrueOrderByPriorityDesc();
    }

    /**
     * Find all rules
     */
    public List<CtcToleranceRule> findAll() {
        return repository.findAll();
    }

    /**
     * Find by ID
     */
    public Optional<CtcToleranceRule> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * Delete rule
     */
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    /**
     * Find matching rule for a specific projet, type, and height
     * Returns the first matching rule based on priority
     */
    public CtcToleranceRule findMatchingRule(String projet, String type, Double height) {
        List<CtcToleranceRule> rules = repository.findMatchingRulesForHeight(projet, type, height);
        return rules.isEmpty() ? null : rules.get(0);
    }

    /**
     * Get tolerance values for a pattern based on projet, type, and height
     */
    public double[] getTolerances(String projet, String type, Double height) {
        CtcToleranceRule rule = findMatchingRule(projet, type, height);
        if (rule != null) {
            return new double[] {
                rule.getToleranceMin1() != null ? rule.getToleranceMin1() : 0,
                rule.getToleranceMax1() != null ? rule.getToleranceMax1() : 0,
                rule.getToleranceMin2() != null ? rule.getToleranceMin2() : 0,
                rule.getToleranceMax2() != null ? rule.getToleranceMax2() : 0
            };
        }
        
        // Default tolerances based on height (fallback)
        double min1 = 0, max1 = 0;
        if (height != null) {
            if (height > 0 && height < 150) {
                min1 = -1.0; max1 = 1.0;
            } else if (height >= 150 && height < 300) {
                min1 = -1.5; max1 = 1.5;
            } else if (height >= 300 && height < 600) {
                min1 = -2.0; max1 = 2.0;
            } else if (height >= 600) {
                min1 = -3.0; max1 = 3.0;
            }
        }
        return new double[] { min1, max1, min1, max1 };
    }

    /**
     * Get drill tolerance for a pattern
     */
    public Integer getDrillTolerance(String projet, String type, Double height) {
        CtcToleranceRule rule = findMatchingRule(projet, type, height);
        return rule != null && rule.getToleranceDrill() != null ? rule.getToleranceDrill() : 1;
    }
    
    /**
     * Apply a specific tolerance rule to CTC Files
     */
    public Map<String, Object> applyRuleToCTC(CtcToleranceRule rule) {
        Map<String, Object> result = new HashMap<>();
        int updatedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();
        
        try {
            String projet = rule.getProjet();
            String type = rule.getType();
            String laminateFilter = rule.getLaminateFilter() != null ? rule.getLaminateFilter() : "all";
            String applyOn = rule.getApplyOn() != null ? rule.getApplyOn() : "max";
            
            // Get patterns for this projet/type
            List<String> patterns = new ArrayList<>();
            if ("laminate_only".equals(laminateFilter)) {
                patterns = filesService.findPatternByProjetAndTypeAsLaminated(projet, type != null ? type : "");
            } else if ("non_laminate_only".equals(laminateFilter)) {
                patterns = filesService.findPatternByProjetAndTypeAsNotLaminated(projet, type != null ? type : "");
            } else {
                patterns = filesService.findPatternByProjetAndType(projet, type != null ? type : "");
            }
            
            for (String pattern : patterns) {
                try {
                    String filePath = pltfolder + "/" + pattern + ".plt";
                    Double dimension = getDimension(filePath, pattern, applyOn);
                    
                    if (dimension != null) {
                        // Check if dimension falls within the rule's range
                        Double minHeight = rule.getHeightMin() != null ? rule.getHeightMin() : 0.0;
                        Double maxHeight = rule.getHeightMax(); // null means infinity
                        
                        if (dimension >= minHeight && (maxHeight == null || dimension < maxHeight)) {
                            Double min1 = rule.getToleranceMin1();
                            Double max1 = rule.getToleranceMax1();


                            if (min1 != null && max1 != null) {
                                // Apply based on laminate filter
                                if ("laminate_only".equals(laminateFilter)) {
                                    System.out.println("Projet: " + projet + ", Type: " + type + ", Applying laminate-only rule to pattern: " + pattern + " with dimension: " + dimension);
                                    filesService.updatePatternByProjetAndTypeAsLaminated(projet, type, pattern, min1, max1);
                                } else if ("non_laminate_only".equals(laminateFilter)) {
                                    System.out.println("Projet: " + projet + ", Type: " + type + ", Applying non-laminate-only rule to pattern: " + pattern + " with dimension: " + dimension);
                                    filesService.updatePatternByProjetAndTypeAsNotLaminated(projet, type, pattern, min1, max1);
                                } else {
                                    System.out.println("Projet: " + projet + ", Type: " + type + ", Applying rule to pattern: " + pattern + " with dimension: " + dimension);
                                    filesService.updatePatternByProjetAndType(projet, type, pattern, min1, max1);
                                }
                                updatedCount++;
                            } else {
                                skippedCount++;
                            }

                            Double min2 = rule.getToleranceMin2();
                            Double max2 = rule.getToleranceMax2();
                            if (min2 != null && max2 != null) {
                                // Apply based on laminate filter
                                if ("laminate_only".equals(laminateFilter)) {
                                    filesService.updatePatternByProjetAndTypeAsLaminated2(projet, type, pattern, min2, max2);
                                } else if ("non_laminate_only".equals(laminateFilter)) {
                                    filesService.updatePatternByProjetAndTypeAsNotLaminated2(projet, type, pattern, min2, max2);
                                } else {
                                    filesService.updatePatternByProjetAndType2(projet, type, pattern, min2, max2);
                                }
                                updatedCount++;
                            } else {
                                skippedCount++;
                            }
                        } else {
                            skippedCount++;
                        }
                    } else {
                        skippedCount++;
                    }
                } catch (Exception e) {
                    errors.add(pattern + ": " + e.getMessage());
                }
            }
            
            result.put("success", true);
            result.put("updatedCount", updatedCount);
            result.put("skippedCount", skippedCount);
            result.put("totalPatterns", patterns.size());
            result.put("errors", errors);
            result.put("message", "Règle appliquée: " + updatedCount + " patterns mis à jour, " + skippedCount + " ignorés");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Erreur: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Apply all active tolerance rules to CTC Files
     */
    public Map<String, Object> applyAllActiveRulesToCTC() {
        Map<String, Object> result = new HashMap<>();
        List<CtcToleranceRule> activeRules = findAllActive();
        int totalUpdated = 0;
        List<Map<String, Object>> rulesResults = new ArrayList<>();
        
        for (CtcToleranceRule rule : activeRules) {
            Map<String, Object> ruleResult = applyRuleToCTC(rule);
            ruleResult.put("ruleId", rule.getId());
            ruleResult.put("projet", rule.getProjet());
            ruleResult.put("type", rule.getType());
            rulesResults.add(ruleResult);
            
            if (Boolean.TRUE.equals(ruleResult.get("success"))) {
                totalUpdated += (Integer) ruleResult.getOrDefault("updatedCount", 0);
            }
        }
        
        result.put("success", true);
        result.put("totalRulesApplied", activeRules.size());
        result.put("totalPatternsUpdated", totalUpdated);
        result.put("details", rulesResults);
        
        return result;
    }
    
    /**
     * Get dimension from PLT file based on applyOn setting.
     * Only considers design parts (PD commands with multiple coordinate pairs),
     * filtering out single-segment label markers/dots and standalone PU positions
     * (label text positions, pen park commands).
     * Auto-detects orientation: if Y range > X range, axes are swapped so that
     * width always represents the longer dimension (length) and height the shorter.
     *
     * Conversion factor: 1 PLT unit ≈ 0.02857 mm (≈ 35 units per mm)
     */
    private Double getDimension(String pltFile, String pattern, String applyOn) {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean hasDesignCoords = false;

        try (BufferedReader br = new BufferedReader(new FileReader(pltFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(";");
                for (String token : tokens) {
                    token = token.trim();
                    // Only process PD (Pen Down) commands — these represent actual drawing
                    // Skip PU (Pen Up) commands — these are just positioning moves
                    // (label positions, pen park, etc.) and don't draw anything
                    if (token.startsWith("PD")) {
                        String coords = token.substring(2);
                        if (!coords.isEmpty()) {
                            String[] values = coords.split(",");
                            // Filter: only include PD commands with more than 1 coordinate pair
                            // (i.e., more than 2 values). Single coordinate pairs (2 values)
                            // are label leader lines / marking dots, not design geometry.
                            if (values.length > 2) {
                                for (int i = 0; i < values.length - 1; i += 2) {
                                    try {
                                        double x = Double.parseDouble(values[i].trim());
                                        double y = Double.parseDouble(values[i + 1].trim());
                                        if (x < minX) minX = x;
                                        if (x > maxX) maxX = x;
                                        if (y < minY) minY = y;
                                        if (y > maxY) maxY = y;
                                        hasDesignCoords = true;
                                    } catch (NumberFormatException e) {
                                        // Ignore parsing errors
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            return null;
        }

        if (!hasDesignCoords) {
            return null;
        }

        // Conversion coefficient: PLT units to mm
        // Calibrated: 1 PLT unit ≈ 1/35 mm (0.02857 mm)
        double conversionFactor = 1.0 / 39.37;

        double xRange = (maxX - minX) * conversionFactor;
        double yRange = (maxY - minY) * conversionFactor;

        // Auto-detect orientation: the longer range is width (length),
        // the shorter range is height. Some PLT files have the piece
        // rotated so that Y is the longer axis.
        double width, height;
        if (xRange >= yRange) {
            width = xRange;
            height = yRange;
        } else {
            width = yRange;
            height = xRange;
        }

        System.out.println("Pattern: " + pattern + ", Width: " + width + " mm, Height: " + height + " mm (" + (maxX - minX) + " : " + (maxY - minY) + ")");

        if ("width".equals(applyOn)) {
            return width;
        } else if ("height".equals(applyOn)) {
            return height;
        } else {
            // Default: max (same as width after orientation detection)
            return Math.max(width, height);
        }
    }
}
