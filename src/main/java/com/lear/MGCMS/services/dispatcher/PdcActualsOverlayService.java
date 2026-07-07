package com.lear.MGCMS.services.dispatcher;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lear.MGCMS.domain.MachineQueue;
import com.lear.MGCMS.repositories.MachineQueueRepository;

/**
 * Phase 6 — thin overlay service for the Plan-de-Charge view.
 *
 * <p>The plan itself is computed by {@link com.lear.MGCMS.services.OrdonnancementService};
 * this service layers "what really happened" on top. For each machine in
 * the request, it reads the current {@code machine_queue} rows and
 * produces a compact, UI-friendly view: estimated start/end, progress
 * percent, and an efficiency ratio vs. planned minutes. The React PdC
 * paints these as a colour overlay on the timeline.</p>
 *
 * <p>Intentionally read-only. The PdC page can call this independently
 * of a full recompute.</p>
 */
@Service
public class PdcActualsOverlayService {

    @Autowired
    private MachineQueueRepository machineQueueRepository;

    @Autowired
    private ShiftProperties shiftProperties;

    /** One row per machine. Ordered by machine name for deterministic rendering. */
    public static final class MachineActuals {
        public final String machineNom;
        public final int queueDepth;
        public final int plannedMinutes;
        public final int shiftEffectiveMinutes;
        public final double efficiencyRatio; // plannedMinutes / shiftEffectiveMinutes
        public final LocalDateTime earliestStart;
        public final LocalDateTime latestEnd;

        public MachineActuals(String machineNom, int queueDepth, int plannedMinutes,
                              int shiftEffectiveMinutes, LocalDateTime earliestStart,
                              LocalDateTime latestEnd) {
            this.machineNom = machineNom;
            this.queueDepth = queueDepth;
            this.plannedMinutes = plannedMinutes;
            this.shiftEffectiveMinutes = shiftEffectiveMinutes;
            this.efficiencyRatio = shiftEffectiveMinutes == 0
                    ? 0.0 : ((double) plannedMinutes) / shiftEffectiveMinutes;
            this.earliestStart = earliestStart;
            this.latestEnd = latestEnd;
        }

        public String getMachineNom()           { return machineNom; }
        public int getQueueDepth()              { return queueDepth; }
        public int getPlannedMinutes()          { return plannedMinutes; }
        public int getShiftEffectiveMinutes()   { return shiftEffectiveMinutes; }
        public double getEfficiencyRatio()      { return efficiencyRatio; }
        public LocalDateTime getEarliestStart() { return earliestStart; }
        public LocalDateTime getLatestEnd()     { return latestEnd; }
    }

    /**
     * Build the actuals overlay for the given machines. Machines with no
     * queue rows still appear with zero-valued actuals so the UI can show
     * them as idle.
     */
    @Transactional(readOnly = true)
    public List<MachineActuals> overlayFor(LocalDate date, int shift, List<String> machineNoms) {
        Map<String, List<MachineQueue>> byMachine = new LinkedHashMap<>();
        if (machineNoms != null) {
            for (String nm : machineNoms) byMachine.put(nm, new ArrayList<>());
        }
        // Single batch fetch, then group in memory — cheaper than N queries.
        Iterable<MachineQueue> all = machineQueueRepository.findAll();
        for (MachineQueue mq : all) {
            if (mq.getMachineNom() == null) continue;
            if (!byMachine.containsKey(mq.getMachineNom())) continue;
            byMachine.get(mq.getMachineNom()).add(mq);
        }
        int eff = shiftProperties.effectiveMinutes();
        List<MachineActuals> out = new ArrayList<>();
        for (Map.Entry<String, List<MachineQueue>> e : byMachine.entrySet()) {
            List<MachineQueue> rows = e.getValue();
            int planned = 0;
            LocalDateTime earliest = null;
            LocalDateTime latest = null;
            for (MachineQueue mq : rows) {
                if (mq.getEstimatedCuttingTime() != null) {
                    planned += (int) Math.round(mq.getEstimatedCuttingTime());
                }
                if (mq.getEstimatedStartTime() != null
                        && (earliest == null || mq.getEstimatedStartTime().isBefore(earliest))) {
                    earliest = mq.getEstimatedStartTime();
                }
                if (mq.getEstimatedEndTime() != null
                        && (latest == null || mq.getEstimatedEndTime().isAfter(latest))) {
                    latest = mq.getEstimatedEndTime();
                }
            }
            out.add(new MachineActuals(e.getKey(), rows.size(), planned, eff, earliest, latest));
        }
        return out;
    }

    /**
     * Cheap helper for badges: "hh:mm planifiées sur hh:mm effectives".
     * Returns "–" if either side is zero so the UI can blank it out.
     */
    public String formatEfficiencyBadge(MachineActuals a) {
        if (a == null || a.plannedMinutes <= 0 || a.shiftEffectiveMinutes <= 0) return "–";
        int plan = a.plannedMinutes;
        int cap  = a.shiftEffectiveMinutes;
        return String.format("%02d:%02d / %02d:%02d  (%.0f%%)",
                plan / 60, plan % 60, cap / 60, cap % 60,
                100.0 * plan / cap);
    }

    /** Convenience for future overlays: {@code Duration} between start/end rounded down. */
    public int elapsedMinutes(MachineActuals a, LocalDateTime at) {
        if (a == null || a.earliestStart == null || at == null) return 0;
        return (int) Math.max(0, Duration.between(a.earliestStart, at).toMinutes());
    }
}
