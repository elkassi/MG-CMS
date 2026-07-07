package com.lear.MGCMS.services;

import com.lear.MGCMS.domain.CapaciteInstallee;
import com.lear.MGCMS.domain.CapaciteInstalleeRule;
import com.lear.MGCMS.repositories.CapaciteInstalleeRepository;
import com.lear.MGCMS.repositories.CapaciteInstalleeRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CapaciteInstalleeService {

    private static final long CACHE_TTL_MS = 60_000;

    @Autowired
    private CapaciteInstalleeRepository capaciteInstalleeRepository;

    @Autowired
    private CapaciteInstalleeRuleRepository capaciteInstalleeRuleRepository;

    /** Cross-request cache for {@link #getEffective} — keyed by "date|shift|groupe". */
    private final Map<String, Optional<CapaciteInstallee>> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();

    public List<CapaciteInstallee> findAll() {
        return capaciteInstalleeRepository.findAll();
    }

    public Page<CapaciteInstallee> findAll(Pageable pageable) {
        return capaciteInstalleeRepository.findAll(pageable);
    }

    public Optional<CapaciteInstallee> findById(Long id) {
        return capaciteInstalleeRepository.findById(id);
    }

    public List<CapaciteInstallee> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return capaciteInstalleeRepository.findByDateProductionBetween(startDate, endDate);
    }

    public Optional<CapaciteInstallee> findByDateShiftGroupe(LocalDate date, Integer shift, String groupe) {
        List<CapaciteInstallee> list = capaciteInstalleeRepository.findByDateProductionAndShiftNumberAndGroupe(date, shift, groupe);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public CapaciteInstallee save(CapaciteInstallee capaciteInstallee) {
        clearCache();
        return capaciteInstalleeRepository.save(capaciteInstallee);
    }

    public void deleteById(Long id) {
        clearCache();
        capaciteInstalleeRepository.deleteById(id);
    }

    public void clearCache() {
        cache.clear();
        cacheTimestamps.clear();
    }

    private String cacheKey(LocalDate date, Integer shift, String groupe) {
        return (date != null ? date.toString() : "null") + "|" + (shift != null ? shift : "null") + "|" + groupe;
    }

    /**
     * Get the capacité installée for a given date/shift/groupe.
     *
     * <p>Resolution precedence:</p>
     * <ol>
     *   <li>explicit {@link CapaciteInstallee} row for the exact (date, shift, groupe);</li>
     *   <li>best-matching {@link CapaciteInstalleeRule} (interval + day/shift/groupe, layered);</li>
     *   <li>legacy null-date/null-shift default row;</li>
     *   <li>{@code null} (callers apply their own hardcoded fallback).</li>
     * </ol>
     *
     * <p>With no rules configured this is identical to the previous behaviour
     * (explicit row → null-default row), so existing consumers are unaffected.
     * Results are cached for 60 seconds to avoid repeated DB hits on the
     * dispatcher hot path.</p>
     */
    public CapaciteInstallee getEffective(LocalDate date, Integer shift, String groupe) {
        String key = cacheKey(date, shift, groupe);
        Long cachedAt = cacheTimestamps.get(key);
        if (cachedAt != null && System.currentTimeMillis() - cachedAt < CACHE_TTL_MS) {
            Optional<CapaciteInstallee> hit = cache.get(key);
            return hit != null ? hit.orElse(null) : null;
        }

        CapaciteInstallee result = resolveEffective(date, shift, groupe);
        cache.put(key, Optional.ofNullable(result));
        cacheTimestamps.put(key, System.currentTimeMillis());
        return result;
    }

    /**
     * Uncached resolution (see {@link #getEffective}). An explicit row is returned
     * as-is (managed entity, carries its id); when none exists the rule and
     * null-default values are merged field-by-field into a transient
     * {@link CapaciteInstallee} (id stays null), rule values taking precedence.
     */
    private CapaciteInstallee resolveEffective(LocalDate date, Integer shift, String groupe) {
        List<CapaciteInstallee> specific = capaciteInstalleeRepository
                .findByDateProductionAndShiftNumberAndGroupe(date, shift, groupe);
        if (!specific.isEmpty()) {
            return specific.get(0);
        }

        CapaciteInstalleeRule ruleVals = resolveRuleValues(date, shift, groupe);
        List<CapaciteInstallee> defaults = capaciteInstalleeRepository.findDefaultByGroupe(groupe);
        CapaciteInstallee def = defaults.isEmpty() ? null : defaults.get(0);

        if (ruleVals == null && def == null) {
            return null;
        }

        CapaciteInstallee merged = new CapaciteInstallee();
        merged.setDateProduction(date);
        merged.setShiftNumber(shift);
        merged.setGroupe(groupe);
        merged.setCapaciteInstallee(firstNonNull(
                ruleVals != null ? ruleVals.getCapaciteInstallee() : null,
                def != null ? def.getCapaciteInstallee() : null));
        merged.setTempsTotalParMachine(firstNonNull(
                ruleVals != null ? ruleVals.getTempsTotalParMachine() : null,
                def != null ? def.getTempsTotalParMachine() : null));
        merged.setEfficienceTarget(firstNonNull(
                ruleVals != null ? ruleVals.getEfficienceTarget() : null,
                def != null ? def.getEfficienceTarget() : null));
        return merged;
    }

    /**
     * Layer all rules matching (date, shift, groupe) into a single value holder,
     * or null when none match. More-specific rules (more non-null conditions;
     * then later dateDebut; then higher id) override earlier ones field-by-field,
     * so a narrow "Friday + shift 2" rule can set only tempsTotalParMachine while
     * inheriting the rest from a broad baseline rule.
     */
    private CapaciteInstalleeRule resolveRuleValues(LocalDate date, Integer shift, String groupe) {
        List<CapaciteInstalleeRule> candidates = capaciteInstalleeRuleRepository.findCandidates(date, groupe);
        if (candidates.isEmpty()) {
            return null;
        }
        int dow = date.getDayOfWeek().getValue();
        List<CapaciteInstalleeRule> matching = new ArrayList<>();
        for (CapaciteInstalleeRule r : candidates) {
            boolean dayOk = r.getDayOfWeek() == null || r.getDayOfWeek().equals(dow);
            boolean shiftOk = r.getShiftNumber() == null || r.getShiftNumber().equals(shift);
            if (dayOk && shiftOk) {
                matching.add(r);
            }
        }
        if (matching.isEmpty()) {
            return null;
        }
        matching.sort((a, b) -> {
            int c = Integer.compare(specificity(a), specificity(b));
            if (c != 0) return c;
            LocalDate da = a.getDateDebut() == null ? LocalDate.MIN : a.getDateDebut();
            LocalDate db = b.getDateDebut() == null ? LocalDate.MIN : b.getDateDebut();
            c = da.compareTo(db);
            if (c != 0) return c;
            long ia = a.getId() == null ? 0L : a.getId();
            long ib = b.getId() == null ? 0L : b.getId();
            return Long.compare(ia, ib);
        });
        CapaciteInstalleeRule out = new CapaciteInstalleeRule();
        for (CapaciteInstalleeRule r : matching) {
            if (r.getCapaciteInstallee() != null) out.setCapaciteInstallee(r.getCapaciteInstallee());
            if (r.getTempsTotalParMachine() != null) out.setTempsTotalParMachine(r.getTempsTotalParMachine());
            if (r.getEfficienceTarget() != null) out.setEfficienceTarget(r.getEfficienceTarget());
        }
        return out;
    }

    private static int specificity(CapaciteInstalleeRule r) {
        int s = 0;
        if (r.getDayOfWeek() != null) s++;
        if (r.getShiftNumber() != null) s++;
        if (r.getGroupe() != null) s++;
        return s;
    }

    private static <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }

    /**
     * Resolve the effective capacity for every (date, shift, groupe) in a range.
     * Each entry carries dateProduction/shiftNumber/groupe set to the requested
     * slot and the explicit row's id when one exists (null otherwise), so the
     * frontend can both display the resolved values and edit/create the explicit
     * override. Groups covered: Coupe, Laser.
     */
    public List<CapaciteInstallee> getEffectiveForRange(LocalDate startDate, LocalDate endDate) {
        List<CapaciteInstallee> out = new ArrayList<>();
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return out;
        }
        String[] groupes = {"Coupe", "Laser"};
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            for (int shift = 1; shift <= 3; shift++) {
                for (String g : groupes) {
                    CapaciteInstallee eff = getEffective(d, shift, g);
                    if (eff == null) {
                        continue;
                    }
                    CapaciteInstallee dto = new CapaciteInstallee();
                    dto.setId(eff.getId());
                    dto.setDateProduction(d);
                    dto.setShiftNumber(shift);
                    dto.setGroupe(g);
                    dto.setCapaciteInstallee(eff.getCapaciteInstallee());
                    dto.setTempsTotalParMachine(eff.getTempsTotalParMachine());
                    dto.setEfficienceTarget(eff.getEfficienceTarget());
                    out.add(dto);
                }
            }
        }
        return out;
    }

    // ----------------------------------------------------------------- rules CRUD

    public List<CapaciteInstalleeRule> findAllRules() {
        return capaciteInstalleeRuleRepository.findAll();
    }

    public Optional<CapaciteInstalleeRule> findRuleById(Long id) {
        return capaciteInstalleeRuleRepository.findById(id);
    }

    public CapaciteInstalleeRule saveRule(CapaciteInstalleeRule rule) {
        clearCache();
        return capaciteInstalleeRuleRepository.save(rule);
    }

    public void deleteRule(Long id) {
        clearCache();
        capaciteInstalleeRuleRepository.deleteById(id);
    }

    /**
     * Ensure entries exist for the next 2 days (today + tomorrow), all 3 shifts, both groups.
     * Called by the scheduled task every 6 hours. Values are sourced via the same
     * precedence as {@link #getEffective} (rules first, then the null-default row),
     * falling back to hardcoded defaults.
     */
    public void ensureNextTwoDays() {
        LocalDate today = LocalDate.now();
        String[] groupes = {"Coupe", "Laser"};
        int[] defaultCapacite = {5, 1}; // default machines for Coupe and Laser

        for (int dayOffset = 0; dayOffset <= 1; dayOffset++) {
            LocalDate date = today.plusDays(dayOffset);
            for (int shift = 1; shift <= 3; shift++) {
                for (int g = 0; g < groupes.length; g++) {
                    List<CapaciteInstallee> existingList = capaciteInstalleeRepository
                            .findByDateProductionAndShiftNumberAndGroupe(date, shift, groupes[g]);
                    if (existingList.isEmpty()) {
                        // Source values from rules first, then the legacy null-default row,
                        // then hardcoded fallbacks (see resolveEffective precedence).
                        CapaciteInstallee resolved = resolveEffective(date, shift, groupes[g]);
                        CapaciteInstallee entry = new CapaciteInstallee();
                        entry.setDateProduction(date);
                        entry.setShiftNumber(shift);
                        entry.setGroupe(groupes[g]);
                        entry.setCapaciteInstallee(
                                resolved != null && resolved.getCapaciteInstallee() != null
                                        ? resolved.getCapaciteInstallee() : defaultCapacite[g]);
                        entry.setTempsTotalParMachine(
                                resolved != null && resolved.getTempsTotalParMachine() != null
                                        ? resolved.getTempsTotalParMachine() : 460.0);
                        entry.setEfficienceTarget(
                                resolved != null && resolved.getEfficienceTarget() != null
                                        ? resolved.getEfficienceTarget() : 90.0);
                        capaciteInstalleeRepository.save(entry);
                    }
                }
            }
        }
        clearCache();
    }
}
