package com.lear.MGCMS.services.production;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.MachineQueue;
import com.lear.MGCMS.domain.SerieRouleauTemp;
import com.lear.MGCMS.domain.Zone;
import com.lear.MGCMS.repositories.EtatMachineHistoriqueRepository;
import com.lear.MGCMS.repositories.MachineQueueRepository;
import com.lear.MGCMS.repositories.ProductionTableRepository;
import com.lear.MGCMS.repositories.ScanRouleauRepository;
import com.lear.MGCMS.repositories.ZoneRepository;
import com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestBoxInfoRepository;
import com.lear.MGCMS.services.SerieRouleauTempService;
import com.lear.MGCMS.services.dispatcher.ActiveMachineResolver;
import com.lear.MGCMS.services.dispatcher.LiveChargeDto;
import com.lear.MGCMS.services.dispatcher.LiveChargeService;
import com.lear.MGCMS.services.scheduling.ShiftClock;

/**
 * Read-only <b>production floor state</b> aggregator for the {@code /api/production/floor}
 * view. Returns the whole floor — every active zone with its machines, the roll being
 * spread on each matelassage table, the rolls staged in each rack, and the box occupancy —
 * so a UI can render the plant at a glance and auto-refresh.
 *
 * <p>This service is a pure reader: it composes existing pieces and never mutates state.
 * Nothing here re-implements business logic; it reuses:</p>
 * <ul>
 *   <li>{@link ZoneRepository#findAllActive()} for the zone slate and rack-name lists, and
 *       {@link ScanRouleauRepository#findAllLight()} for rolls in racks — racks are mapped
 *       to zones from {@code Zone.rollLocations} exactly as
 *       {@code LogisticsReleaseService} does.</li>
 *   <li>{@link ProductionTableRepository#findMachinesWithTypeInZones(List)} for the
 *       machines (and their machine types) physically in each zone.</li>
 *   <li>{@link ActiveMachineResolver#activeMachines(LocalDate, int, String)} for which
 *       machines are UP this shift.</li>
 *   <li>{@link SerieRouleauTempService#getAll()} for the roll currently spread on each
 *       table (matelassage in progress).</li>
 *   <li>{@link MachineQueueRepository#findAllOrdered()} for the per-machine cut queue.</li>
 *   <li>{@link LiveChargeService#compute()} for the in-production sequences/series — used
 *       both to find what each machine is cutting <em>now</em> (the In-progress serie whose
 *       {@code tableCoupe} is the machine) and to sum the per-zone box occupancy over the
 *       zone's locked + pending sequences.</li>
 *   <li>{@link CuttingRequestBoxInfoRepository#countBoxesBySequences(List)} for the box
 *       counts; capacity = activeMachines × 16, mirroring
 *       {@code WorkbenchSequenceFocusService.buildBoxOccupancy}.</li>
 * </ul>
 *
 * <h2>Machine cutting status</h2>
 * The cleanest cheap live source for machine availability is
 * {@link EtatMachineHistoriqueRepository#findAllCurrentStatuses(LocalDateTime)} which yields
 * {@code (machine, codeEtat)} where {@code M/MS/MD/R} mean the machine is running (same set
 * {@code PlanDeChargeService} treats as up) and anything else (typically {@code PN}) is a
 * breakdown. There is no cheap "is this machine in repérage right now" feed, so the floor
 * status is derived and the raw {@code codeEtat} is exposed for the UI to refine:
 * <ul>
 *   <li>not UP this shift (or a breakdown code) → {@code DOWN};</li>
 *   <li>UP + an In-progress serie sits on its table → {@code CUTTING} (with the serie);</li>
 *   <li>UP + a known stop code ({@code AR}/{@code S}/…) → {@code STOPPED};</li>
 *   <li>UP + a repérage code ({@code RP}/{@code REP}) → {@code REPRAGE};</li>
 *   <li>UP otherwise → {@code IDLE}.</li>
 * </ul>
 *
 * <h2>Finish ETA</h2>
 * Per machine, the ETA "if it cuts all its series" reuses the dispatcher's own per-serie
 * timing: it sums {@link MachineQueue#getEstimatedCuttingTime()} over the queue, plus the
 * remaining minutes of the In-progress serie from {@link LiveChargeService} when present.
 * Returned both as minutes-from-now and a clock time.
 */
@Service
public class FloorStateService {

    /** Spreading boxes one active machine can hold — mirrors WorkbenchSequenceFocusService. */
    private static final int BOXES_PER_MACHINE = 16;
    /** codeEtat values that mean the machine is running (same set PlanDeChargeService uses). */
    private static final Set<String> RUNNING_CODES = new LinkedHashSet<>(Arrays.asList("M", "MS", "MD", "R"));
    /** codeEtat values that mean the machine is up but in repérage. */
    private static final Set<String> REPRAGE_CODES = new LinkedHashSet<>(Arrays.asList("RP", "REP"));
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired private ZoneRepository zoneRepository;
    @Autowired private ScanRouleauRepository scanRouleauRepository;
    @Autowired private ProductionTableRepository productionTableRepository;
    @Autowired private ActiveMachineResolver activeMachineResolver;
    @Autowired private SerieRouleauTempService serieRouleauTempService;
    @Autowired private MachineQueueRepository machineQueueRepository;
    @Autowired private LiveChargeService liveChargeService;
    @Autowired private CuttingRequestBoxInfoRepository boxInfoRepository;
    @Autowired private EtatMachineHistoriqueRepository etatMachineRepository;
    @Autowired private ShiftClock shiftClock;
    @Autowired private com.lear.MGCMS.repositories.CuttingRequest.CuttingRequestRepository cuttingRequestRepository;

    /**
     * Build the whole-floor snapshot for the (date, shift). Null params default to the
     * plant's current slot via {@link ShiftClock#currentSlot()}.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFloorState(LocalDate date, Integer shift) {
        ShiftClock.ShiftSlot slot = shiftClock.currentSlot();
        LocalDate theDate = date != null ? date : slot.date;
        int theShift = shift != null ? shift : slot.shift;
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", theDate.toString());
        out.put("shift", theShift);
        out.put("generatedAt", now.toString());

        List<Zone> zones = zoneRepository.findAllActive();
        if (zones == null) zones = Collections.emptyList();

        // --- Shared indexes (built once, sliced per zone) ---
        Map<String, String> locationToZone = buildLocationToZone(zones);
        Map<String, ZoneRolls> rollsByZone = buildRollsByZone(locationToZone);

        // machine -> (zoneNom, machineType)
        List<String> zoneNoms = new ArrayList<>();
        for (Zone z : zones) if (z.getNom() != null) zoneNoms.add(z.getNom());
        Map<String, List<MachineRef>> machinesByZone = buildMachinesByZone(zoneNoms);

        // machine -> ordered cut queue
        Map<String, List<MachineQueue>> queueByMachine = buildQueueByMachine();

        // current spreading roll per table (matelassage in progress)
        Map<String, List<SerieRouleauTemp>> spreadingByTable = buildSpreadingByTable();

        // machine -> raw codeEtat probed inside the requested shift (or now() for the live slot)
        boolean currentSlot = theDate.equals(slot.date) && theShift == slot.shift;
        Map<String, String> codeEtatByMachine = etatMachineHistorique(theDate, theShift, currentSlot, now);

        // LiveCharge: in-progress serie per machine + per-zone box occupancy.
        LiveChargeDto liveCharge = liveChargeService.compute();
        Map<String, CurrentCut> currentCutByMachine = buildCurrentCutByMachine(liveCharge);
        BoxIndex boxIndex = buildBoxIndex(liveCharge);

        List<Map<String, Object>> zoneRows = new ArrayList<>();
        int machineCount = 0;
        int cuttingCount = 0;
        int stoppedCount = 0;
        int spreadingCount = 0;
        int rollsInRacks = 0;

        for (Zone zone : zones) {
            String zoneNom = zone.getNom();
            if (zoneNom == null) continue;

            Set<String> upMachines = activeMachineResolver.activeMachines(theDate, theShift, zoneNom);

            // --- Machines ---
            List<Map<String, Object>> machineRows = new ArrayList<>();
            for (MachineRef m : machinesByZone.getOrDefault(zoneNom, Collections.emptyList())) {
                boolean up = upMachines.contains(m.nom);
                String codeEtat = codeEtatByMachine.get(m.nom);
                CurrentCut cut = currentCutByMachine.get(m.nom);
                List<MachineQueue> queue = queueByMachine.getOrDefault(m.nom, Collections.emptyList());

                String status = deriveStatus(up, codeEtat, cut);
                double etaMinutes = finishEtaMinutes(queue, cut);

                Map<String, Object> mr = new LinkedHashMap<>();
                mr.put("nom", m.nom);
                mr.put("machineType", m.machineType);
                mr.put("groupe", groupeOf(m.machineType));
                mr.put("up", up);
                mr.put("status", status);
                mr.put("codeEtat", codeEtat);
                mr.put("currentSerie", cut != null ? cut.serie : null);
                mr.put("currentSequence", cut != null ? cut.sequence : null);
                mr.put("currentMaterial", cut != null ? cut.material : null);
                mr.put("finishEtaMinutes", etaMinutes > 0 ? Math.round(etaMinutes) : null);
                mr.put("finishEtaClock", etaMinutes > 0 ? now.plusMinutes(Math.round(etaMinutes)).format(CLOCK) : null);
                mr.put("queue", queueRows(queue));
                machineRows.add(mr);

                machineCount++;
                if ("CUTTING".equals(status)) cuttingCount++;
                else if ("STOPPED".equals(status)) stoppedCount++;
            }

            // --- Spreading tables (matelassage in progress) in this zone ---
            List<Map<String, Object>> spreadingRows = new ArrayList<>();
            for (MachineRef m : machinesByZone.getOrDefault(zoneNom, Collections.emptyList())) {
                for (SerieRouleauTemp roll : spreadingByTable.getOrDefault(m.nom, Collections.emptyList())) {
                    spreadingRows.add(spreadingRow(roll, currentCutByMachine.get(m.nom)));
                    spreadingCount++;
                }
            }

            // --- Racks ---
            ZoneRolls zr = rollsByZone.get(zoneNom);
            List<Map<String, Object>> rackRows = new ArrayList<>();
            if (zr != null) {
                for (RackRolls rack : zr.byRack.values()) {
                    rackRows.add(rackRow(rack));
                    rollsInRacks += rack.rolls.size();
                }
            }

            // --- Boxes ---
            int occupied = boxIndex.occupied(zoneNom);
            int capacity = upMachines.size() * BOXES_PER_MACHINE;
            Map<String, Object> boxes = new LinkedHashMap<>();
            boxes.put("occupied", occupied);
            boxes.put("capacity", capacity);
            boxes.put("pct", capacity > 0 ? round2(occupied * 100.0 / capacity) : 0.0);

            Map<String, Object> zoneRow = new LinkedHashMap<>();
            zoneRow.put("zone", zoneNom);
            zoneRow.put("category", zone.getCategory() != null ? zone.getCategory().name() : null);
            zoneRow.put("machines", machineRows);
            zoneRow.put("spreadingTables", spreadingRows);
            zoneRow.put("racks", rackRows);
            zoneRow.put("boxes", boxes);
            zoneRow.put("sequences", boxIndex.sequenceRows.getOrDefault(zoneNom, Collections.emptyList()));
            zoneRows.add(zoneRow);
        }

        out.put("zones", zoneRows);

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("zoneCount", zoneRows.size());
        totals.put("machineCount", machineCount);
        totals.put("cuttingCount", cuttingCount);
        totals.put("stoppedCount", stoppedCount);
        totals.put("spreadingCount", spreadingCount);
        totals.put("rollsInRacks", rollsInRacks);
        out.put("totals", totals);
        return out;
    }

    // ------------------------------------------------------------------ machines

    private Map<String, List<MachineRef>> buildMachinesByZone(List<String> zoneNoms) {
        Map<String, List<MachineRef>> out = new LinkedHashMap<>();
        if (zoneNoms.isEmpty()) return out;
        // columns: 0=zoneNom, 1=machineNom, 2=machineTypeName
        for (Object[] row : productionTableRepository.findMachinesWithTypeInZones(zoneNoms)) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) continue;
            String zoneNom = String.valueOf(row[0]);
            String machineNom = String.valueOf(row[1]);
            String type = row.length > 2 && row[2] != null ? String.valueOf(row[2]) : null;
            out.computeIfAbsent(zoneNom, k -> new ArrayList<>()).add(new MachineRef(machineNom, type));
        }
        for (List<MachineRef> list : out.values()) {
            list.sort(Comparator.comparing(m -> m.nom != null ? m.nom : "", String.CASE_INSENSITIVE_ORDER));
        }
        return out;
    }

    private Map<String, List<MachineQueue>> buildQueueByMachine() {
        Map<String, List<MachineQueue>> out = new LinkedHashMap<>();
        for (MachineQueue q : machineQueueRepository.findAllOrdered()) {
            if (q == null || q.getMachineNom() == null) continue;
            out.computeIfAbsent(q.getMachineNom(), k -> new ArrayList<>()).add(q);
        }
        return out;
    }

    private List<Map<String, Object>> queueRows(List<MachineQueue> queue) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MachineQueue q : queue) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("serie", q.getSerie());
            r.put("sequence", q.getSequenceId());
            r.put("material", q.getPartNumberMaterial());
            r.put("longueur", q.getLongueur());
            r.put("estimatedCuttingTime", q.getEstimatedCuttingTime());
            r.put("position", q.getQueuePosition());
            rows.add(r);
        }
        return rows;
    }

    /** Derived floor status — see class javadoc. */
    private String deriveStatus(boolean up, String codeEtat, CurrentCut cut) {
        if (!up) return "DOWN";
        String code = codeEtat != null ? codeEtat.trim().toUpperCase(Locale.ROOT) : null;
        // A breakdown code while the resolver still considers it UP: treat as DOWN.
        if (code != null && !RUNNING_CODES.contains(code) && !REPRAGE_CODES.contains(code)
                && !code.isEmpty()) {
            // Known stop/breakdown codes. PN = panne (breakdown).
            if ("PN".equals(code)) return "DOWN";
            return "STOPPED";
        }
        if (code != null && REPRAGE_CODES.contains(code)) return "REPRAGE";
        if (cut != null) return "CUTTING";
        return "IDLE";
    }

    /**
     * ETA, in minutes from now, to finish all work currently on the machine: the
     * In-progress serie's remaining minutes (from LiveCharge) plus the sum of the
     * queue's estimated cutting times.
     */
    private double finishEtaMinutes(List<MachineQueue> queue, CurrentCut cut) {
        double minutes = cut != null ? Math.max(0.0, cut.remainingMinutes) : 0.0;
        for (MachineQueue q : queue) {
            if (q.getEstimatedCuttingTime() != null) minutes += Math.max(0.0, q.getEstimatedCuttingTime());
        }
        return minutes;
    }

    // ------------------------------------------------------------------ live cutting state

    /**
     * Index the In-progress serie sitting on each physical machine. In this plant a serie's
     * {@code tableCoupe} holds the machine name (see {@code OrdonnancementService}), so the
     * In-progress serie whose {@code tableCoupe} equals a machine is what that machine is
     * cutting now. Walks every zone's locked + pending sequences from LiveCharge.
     */
    private Map<String, CurrentCut> buildCurrentCutByMachine(LiveChargeDto liveCharge) {
        Map<String, CurrentCut> out = new LinkedHashMap<>();
        if (liveCharge == null || liveCharge.getZones() == null) return out;
        for (LiveChargeDto.ZoneChargeDto zone : liveCharge.getZones()) {
            if (zone == null) continue;
            indexCuts(out, zone.getLockedSequences());
            indexCuts(out, zone.getPendingSequences());
        }
        return out;
    }

    private void indexCuts(Map<String, CurrentCut> out, List<LiveChargeDto.SequenceDto> sequences) {
        if (sequences == null) return;
        for (LiveChargeDto.SequenceDto seq : sequences) {
            if (seq == null || seq.getSeries() == null) continue;
            for (LiveChargeDto.SerieDto serie : seq.getSeries()) {
                if (serie == null) continue;
                if (!"In progress".equalsIgnoreCase(trim(serie.getStatusCoupe()))) continue;
                String machine = trim(serie.getTableCoupe());
                if (machine == null) continue;
                // Keep the first In-progress serie seen for a machine.
                out.putIfAbsent(machine, new CurrentCut(
                        serie.getSerie(), seq.getSequence(),
                        serie.getRefTissus(), serie.getRemainingMinutes()));
            }
        }
    }

    // ------------------------------------------------------------------ spreading

    private Map<String, List<SerieRouleauTemp>> buildSpreadingByTable() {
        Map<String, List<SerieRouleauTemp>> out = new LinkedHashMap<>();
        List<SerieRouleauTemp> rolls = serieRouleauTempService.getAll();
        if (rolls == null) return out;
        for (SerieRouleauTemp roll : rolls) {
            if (roll == null || roll.getTableMatelassage() == null) continue;
            out.computeIfAbsent(roll.getTableMatelassage(), k -> new ArrayList<>()).add(roll);
        }
        return out;
    }

    private Map<String, Object> spreadingRow(SerieRouleauTemp roll, CurrentCut cut) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("table", roll.getTableMatelassage());
        r.put("idRouleau", roll.getIdRouleau());
        r.put("reftissu", roll.getReftissu());
        r.put("lot", roll.getLot());
        r.put("estimationRest", roll.getEstimationRest());
        r.put("serie", cut != null ? cut.serie : null);
        r.put("since", roll.getDate() != null ? roll.getDate().toString() : null);
        return r;
    }

    // ------------------------------------------------------------------ racks

    private Map<String, String> buildLocationToZone(List<Zone> zones) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Zone zone : zones) {
            if (zone == null || zone.getNom() == null) continue;
            for (String loc : parseLocations(zone.getRollLocations())) {
                out.put(norm(loc), zone.getNom());
            }
        }
        return out;
    }

    /** Rolls in racks, grouped zone → rack, from the light ScanRouleau projection. */
    private Map<String, ZoneRolls> buildRollsByZone(Map<String, String> locationToZone) {
        Map<String, ZoneRolls> out = new LinkedHashMap<>();
        // columns: 0=serialId, 1=reftissu, 2=quantite, 3=emplacement(rack), 4=lot, 5=metrage
        for (Object[] row : scanRouleauRepository.findRackLight()) {
            if (row == null || row.length < 6) continue;
            String serialId = value(row[0]);
            String reftissu = value(row[1]);
            String rack = value(row[3]);
            String lot = value(row[4]);
            double meters = number(row[5]);
            if (meters <= 0) meters = number(row[2]); // quantite fallback
            String zoneNom = zoneForLocation(locationToZone, rack);
            if (zoneNom == null) continue; // roll not in any active zone's rack — skip
            ZoneRolls zr = out.computeIfAbsent(zoneNom, k -> new ZoneRolls());
            String rackKey = rack != null ? rack : "";
            RackRolls rr = zr.byRack.computeIfAbsent(rackKey, k -> new RackRolls(rack));
            rr.meters += meters;
            Map<String, Object> rollRow = new LinkedHashMap<>();
            rollRow.put("serialId", serialId);
            rollRow.put("reftissu", reftissu);
            rollRow.put("metrage", round2(meters));
            rollRow.put("lot", lot);
            rr.rolls.add(rollRow);
        }
        return out;
    }

    private Map<String, Object> rackRow(RackRolls rack) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("rack", rack.rack);
        r.put("rollCount", rack.rolls.size());
        r.put("meters", round2(rack.meters));
        r.put("rolls", rack.rolls);
        return r;
    }

    /** Exact rack match first, then containment — mirrors LogisticsReleaseService.zoneForLocation. */
    private String zoneForLocation(Map<String, String> locationToZone, String location) {
        String key = norm(location);
        if (key.isEmpty()) return null;
        String exact = locationToZone.get(key);
        if (exact != null) return exact;
        for (Map.Entry<String, String> e : locationToZone.entrySet()) {
            if (!e.getKey().isEmpty() && key.contains(e.getKey())) return e.getValue();
        }
        return null;
    }

    // ------------------------------------------------------------------ boxes

    /**
     * Per-zone occupied box count = Σ boxCount over the zone's in-production (locked +
     * pending) sequences. Capacity is computed per zone from active machines × 16 at the
     * call site. Mirrors {@code WorkbenchSequenceFocusService.buildBoxOccupancy} /
     * {@code LogisticsReleaseService.buildBoxIndex}.
     */
    private BoxIndex buildBoxIndex(LiveChargeDto liveCharge) {
        BoxIndex index = new BoxIndex();
        if (liveCharge == null || liveCharge.getZones() == null) return index;
        Map<String, List<String>> sequencesByZone = new LinkedHashMap<>();
        List<String> allSequences = new ArrayList<>();
        for (LiveChargeDto.ZoneChargeDto z : liveCharge.getZones()) {
            if (z == null || z.getZoneNom() == null) continue;
            List<String> seqs = new ArrayList<>();
            collectSequenceIds(seqs, z.getLockedSequences());
            collectSequenceIds(seqs, z.getPendingSequences());
            sequencesByZone.put(z.getZoneNom(), seqs);
            allSequences.addAll(seqs);
        }
        Map<String, Integer> boxCountBySeq = loadBoxCounts(allSequences);
        for (Map.Entry<String, List<String>> e : sequencesByZone.entrySet()) {
            int occupied = 0;
            for (String seq : e.getValue()) occupied += boxCountBySeq.getOrDefault(seq, 0);
            index.occupied.put(e.getKey(), occupied);
        }

        // Per-zone sequence rows for the chef rectification panel: the same
        // locked+pending set whose boxes make up `occupied`, so what the chef
        // corrects is exactly what the box bar counts.
        Map<String, String> statusBySeq = loadSequenceStatuses(allSequences);
        for (LiveChargeDto.ZoneChargeDto z : liveCharge.getZones()) {
            if (z == null || z.getZoneNom() == null) continue;
            List<Map<String, Object>> rows = new ArrayList<>();
            appendSequenceRows(rows, z.getLockedSequences(), true, boxCountBySeq, statusBySeq);
            appendSequenceRows(rows, z.getPendingSequences(), false, boxCountBySeq, statusBySeq);
            rows.sort((a, b) -> Integer.compare(
                    (Integer) b.getOrDefault("boxCount", 0),
                    (Integer) a.getOrDefault("boxCount", 0)));
            index.sequenceRows.put(z.getZoneNom(), rows);
        }
        return index;
    }

    private void appendSequenceRows(List<Map<String, Object>> out,
                                    List<LiveChargeDto.SequenceDto> sequences,
                                    boolean locked,
                                    Map<String, Integer> boxCountBySeq,
                                    Map<String, String> statusBySeq) {
        if (sequences == null) return;
        for (LiveChargeDto.SequenceDto seq : sequences) {
            if (seq == null || seq.getSequence() == null) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sequence", seq.getSequence());
            row.put("status", statusBySeq.get(seq.getSequence()));
            row.put("locked", locked);
            row.put("lockingTable", seq.getLockingTableNom());
            row.put("boxCount", boxCountBySeq.getOrDefault(seq.getSequence(), 0));
            row.put("dueDate", seq.getDueDate() != null ? seq.getDueDate().toString() : null);
            row.put("remainingMinutes", round2(seq.getTotalRemainingMinutes()));
            out.add(row);
        }
    }

    private Map<String, String> loadSequenceStatuses(List<String> sequences) {
        Map<String, String> out = new LinkedHashMap<>();
        if (sequences.isEmpty()) return out;
        for (Object[] row : cuttingRequestRepository.findStatusBySequences(sequences)) {
            if (row == null || row.length < 2 || row[0] == null) continue;
            out.put(String.valueOf(row[0]), row[1] != null ? String.valueOf(row[1]) : null);
        }
        return out;
    }

    private void collectSequenceIds(List<String> out, List<LiveChargeDto.SequenceDto> sequences) {
        if (sequences == null) return;
        for (LiveChargeDto.SequenceDto seq : sequences) {
            if (seq != null && seq.getSequence() != null) out.add(seq.getSequence());
        }
    }

    private Map<String, Integer> loadBoxCounts(List<String> sequences) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (sequences.isEmpty()) return out;
        for (Object[] row : boxInfoRepository.countBoxesBySequences(sequences)) {
            if (row == null || row.length < 2 || row[0] == null) continue;
            int count = row[1] != null ? ((Number) row[1]).intValue() : 0;
            out.put(String.valueOf(row[0]), count);
        }
        return out;
    }

    // ------------------------------------------------------------------ machine status source

    /**
     * Current codeEtat per machine, probed at a time that actually falls inside the
     * requested shift, via the batch status query. Probing at a hardcoded 12:00 returned
     * the noon state for night/afternoon shifts. We use a shift-representative hour —
     * shift 1 (night) → 04:00, shift 2 (morning) → 12:00, shift 3 (afternoon) → 20:00 —
     * each comfortably mid-shift. When the requested (date,shift) is the plant's live slot,
     * we probe at {@code now} instead so the board reflects the live moment.
     */
    private Map<String, String> etatMachineHistorique(LocalDate date, int shift, boolean currentSlot, LocalDateTime now) {
        Map<String, String> out = new LinkedHashMap<>();
        LocalDateTime probe = currentSlot ? now : date.atTime(shiftHour(shift), 0);
        for (Object[] row : etatMachineRepository.findAllCurrentStatuses(probe)) {
            if (row == null || row.length < 2 || row[0] == null) continue;
            out.put(String.valueOf(row[0]), row[1] != null ? String.valueOf(row[1]) : null);
        }
        return out;
    }

    /** Mid-shift hour for probing machine status: 1=night→04, 2=morning→12, 3=afternoon→20. */
    private static int shiftHour(int shift) {
        switch (shift) {
            case 1: return 4;
            case 3: return 20;
            default: return 12; // shift 2 (morning) and any unexpected value
        }
    }

    // ------------------------------------------------------------------ helpers

    private static String groupeOf(String machineType) {
        if (machineType == null) return "Coupe";
        String t = machineType.trim();
        if (t.equalsIgnoreCase("LASER-DXF") || t.equalsIgnoreCase("LASER-LSR")
                || t.equalsIgnoreCase("LASER")) {
            return "Laser";
        }
        return "Coupe";
    }

    private List<String> parseLocations(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (String s : raw.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private String trim(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        return v.isEmpty() ? null : v;
    }

    private String norm(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    private String value(Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }

    private double number(Object raw) {
        if (raw == null) return 0.0;
        if (raw instanceof Number) return ((Number) raw).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // ------------------------------------------------------------------ inner carriers

    private static final class MachineRef {
        final String nom;
        final String machineType;
        MachineRef(String nom, String machineType) {
            this.nom = nom;
            this.machineType = machineType;
        }
    }

    /** What a machine is cutting right now (its In-progress serie). */
    private static final class CurrentCut {
        final String serie;
        final String sequence;
        final String material;
        final double remainingMinutes;
        CurrentCut(String serie, String sequence, String material, double remainingMinutes) {
            this.serie = serie;
            this.sequence = sequence;
            this.material = material;
            this.remainingMinutes = remainingMinutes;
        }
    }

    /** Rolls in racks for one zone, grouped by rack name. */
    private static final class ZoneRolls {
        final Map<String, RackRolls> byRack = new LinkedHashMap<>();
    }

    private static final class RackRolls {
        final String rack;
        double meters;
        final List<Map<String, Object>> rolls = new ArrayList<>();
        RackRolls(String rack) {
            this.rack = rack;
        }
    }

    /** Per-zone occupied box count (capacity computed at the call site from active machines). */
    private static final class BoxIndex {
        final Map<String, Integer> occupied = new LinkedHashMap<>();
        /** Per zone: the locked+pending sequence rows whose boxes sum to {@code occupied}. */
        final Map<String, List<Map<String, Object>>> sequenceRows = new LinkedHashMap<>();
        int occupied(String zone) {
            return occupied.getOrDefault(zone, 0);
        }
    }
}
