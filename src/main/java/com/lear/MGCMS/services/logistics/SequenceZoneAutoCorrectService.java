package com.lear.MGCMS.services.logistics;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.lear.MGCMS.domain.CuttingRequest.ReleaseZoneSource;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestDataRepository;
import com.lear.MGCMS.repositories.CuttingRequest.data.CuttingRequestSerieDataRepository;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.services.OrdonnancementService;
import com.lear.MGCMS.services.dispatcher.LockResolver;
import com.lear.MGCMS.services.dispatcher.WorkbenchCacheService;

/**
 * Infers the real zone of {@code STARTED} / {@code COMPLETED} sequences from
 * where their series were physically cut, and writes it to
 * {@code CuttingRequest.releaseZone} (source {@code AUTO}).
 *
 * <p>Why: the logistics picklist is not in real use yet, so most sequences
 * reach production without a released zone — per-zone stats (box occupancy on
 * /logisticsRelease, the floor view, the rectification screen) then count them
 * nowhere or in the wrong zone. Until the picklist owns the zone, the best
 * truth is physical: the table that worked the <b>last</b> serie, when that
 * table sits in a {@code STRICT} zone (same rule as the dispatcher's implicit
 * lock — {@link LockResolver}; series on SHARED tables are ignored).</p>
 *
 * <p>Locks: a zone written by the logistics release ({@code LOGISTICS}) or by
 * a chef ({@code CHEF}) is never overwritten — see
 * {@link ReleaseZoneSource#isAutoCorrectable(String)} plus the same guard
 * inside {@link CuttingRequestDataRepository#applyAutoZone}. Once
 * /logisticsRelease is in real use its zones stay authoritative and this job
 * naturally stops touching them; it can also be disabled with
 * {@code mgcms.sequence.zoneAutofix.enabled=false}.</p>
 */
@Service
public class SequenceZoneAutoCorrectService {

    private static final Logger log = LoggerFactory.getLogger(SequenceZoneAutoCorrectService.class);

    /** Max ids per IN (...) batch — stays under SQL Server's 2100-parameter cap. */
    private static final int BATCH_SIZE = 1000;

    @Value("${mgcms.sequence.zoneAutofix.enabled:true}")
    private boolean enabled;

    /** How far back (planningDate) the job looks for sequences to correct. */
    @Value("${mgcms.sequence.zoneAutofix.days:14}")
    private int windowDays;

    @Autowired private CuttingRequestDataRepository requestDataRepository;
    @Autowired private CuttingRequestSerieDataRepository serieDataRepository;
    @Autowired private ProductionTableRepository productionTableRepository;
    @Autowired(required = false) private WorkbenchCacheService workbenchCacheService;
    @Autowired(required = false) private OrdonnancementService ordonnancementService;

    /** Scheduled entry point — every 15 minutes, first run 3 minutes after startup. */
    @Scheduled(fixedRate = 1000L * 60 * 15, initialDelay = 1000L * 60 * 3)
    public void scheduledRun() {
        if (!enabled) {
            return;
        }
        try {
            runOnce();
        } catch (RuntimeException ex) {
            // Never let a bad run kill the scheduler thread; the next tick retries.
            log.error("SequenceZoneAutoCorrect failed", ex);
        }
    }

    /**
     * One correction pass. Also the manual ops trigger
     * ({@code POST /api/sequence/zone-autofix}).
     *
     * @return {@code success}, {@code examined} (auto-correctable candidates),
     *         {@code corrected}, {@code locked} (skipped LOGISTICS/CHEF zones),
     *         {@code noSignal} (no serie cut on a STRICT-zone table yet)
     */
    public Map<String, Object> runOnce() {
        LocalDate cutoff = LocalDate.now().minusDays(Math.max(1, windowDays));
        List<Object[]> candidates = requestDataRepository.findZoneAutofixCandidates(cutoff);

        Map<String, String> currentZoneBySeq = new LinkedHashMap<>();
        int locked = 0;
        for (Object[] row : candidates) {
            String sequence = (String) row[0];
            if (sequence == null) continue;
            if (!ReleaseZoneSource.isAutoCorrectable((String) row[2])) {
                locked++;
                continue;
            }
            currentZoneBySeq.put(sequence, (String) row[1]);
        }

        Map<String, List<LockResolver.SerieLockInput>> inputsBySeq =
                loadLockInputs(new ArrayList<>(currentZoneBySeq.keySet()));
        Map<String, LockResolver.TableZoneInfo> tableToZone = loadTableZones(inputsBySeq);

        int corrected = 0;
        int noSignal = 0;
        for (Map.Entry<String, String> entry : currentZoneBySeq.entrySet()) {
            String sequence = entry.getKey();
            Optional<LockResolver.LockResult> lock = LockResolver.resolve(
                    null, null, inputsBySeq.get(sequence), tableToZone);
            if (!lock.isPresent()) {
                noSignal++;
                continue;
            }
            String zone = lock.get().getLockZoneNom();
            if (zone.equals(entry.getValue())) {
                continue; // already correct
            }
            int applied = requestDataRepository.applyAutoZone(sequence, zone);
            if (applied > 0) {
                corrected++;
                log.info("Zone auto-correct: {} zone {} -> {} (serie {} on {})",
                        sequence, entry.getValue(), zone,
                        lock.get().getLockingSerieId(), lock.get().getLockingTableNom());
            }
        }

        if (corrected > 0) {
            invalidateDerivedCaches();
        }
        log.info("Zone auto-correct pass: {} examined, {} corrected, {} locked, {} without STRICT signal",
                currentZoneBySeq.size(), corrected, locked, noSignal);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("examined", currentZoneBySeq.size());
        result.put("corrected", corrected);
        result.put("locked", locked);
        result.put("noSignal", noSignal);
        return result;
    }

    /** sequence -&gt; lock inputs (series with cutting activity on a known table), batched. */
    private Map<String, List<LockResolver.SerieLockInput>> loadLockInputs(List<String> sequences) {
        Map<String, List<LockResolver.SerieLockInput>> out = new HashMap<>();
        for (int i = 0; i < sequences.size(); i += BATCH_SIZE) {
            List<String> batch = sequences.subList(i, Math.min(i + BATCH_SIZE, sequences.size()));
            for (Object[] row : serieDataRepository.findLockInputsBySequences(batch)) {
                String sequence = (String) row[0];
                out.computeIfAbsent(sequence, k -> new ArrayList<>())
                        .add(new LockResolver.SerieLockInput(
                                (String) row[1], (String) row[2], (String) row[3],
                                (LocalDateTime) row[4]));
            }
        }
        return out;
    }

    /** tableNom -&gt; (zone, category) for every table referenced by the lock inputs. */
    private Map<String, LockResolver.TableZoneInfo> loadTableZones(
            Map<String, List<LockResolver.SerieLockInput>> inputsBySeq) {
        List<String> tables = new ArrayList<>();
        for (List<LockResolver.SerieLockInput> inputs : inputsBySeq.values()) {
            for (LockResolver.SerieLockInput input : inputs) {
                if (input.tableCoupe != null && !tables.contains(input.tableCoupe)) {
                    tables.add(input.tableCoupe);
                }
            }
        }
        Map<String, LockResolver.TableZoneInfo> out = new HashMap<>();
        for (int i = 0; i < tables.size(); i += BATCH_SIZE) {
            List<String> batch = tables.subList(i, Math.min(i + BATCH_SIZE, tables.size()));
            for (Object[] row : productionTableRepository.findZoneInfoByTableNoms(batch)) {
                String tableNom = (String) row[0];
                String zoneNom = (String) row[1];
                Object catObj = row[2];
                Zone.Category category = catObj instanceof Zone.Category
                        ? (Zone.Category) catObj
                        : Zone.Category.valueOf(String.valueOf(catObj));
                out.put(tableNom, new LockResolver.TableZoneInfo(zoneNom, category));
            }
        }
        return out;
    }

    /** Mirror of the other sequenceStatus writers — refresh the derived views. */
    private void invalidateDerivedCaches() {
        try {
            if (ordonnancementService != null) {
                ordonnancementService.invalidateTimelineCache();
            }
            if (workbenchCacheService != null) {
                workbenchCacheService.invalidateAll();
            }
        } catch (Exception ignored) {
            // Derived views refresh on the next poll if this eager refresh fails.
        }
    }
}
