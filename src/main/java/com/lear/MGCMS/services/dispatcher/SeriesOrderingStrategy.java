package com.lear.MGCMS.services.dispatcher;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.lear.MGCMS.services.OrdonnancementService;

/**
 * Pluggable strategy for ordering series inside a machine queue.
 *
 * <p>The dispatcher (and the legacy ordonnancement engine) produces a
 * per-machine list of series.  How those series are ordered directly
 * impacts the box-duration KPI:
 * {@code boxDur = (max dateFinCoupe − min dateDebutMatelassage) / N_boxes}.
 *
 * <p>Implementations MUST be pure functions — they only reorder the
 * supplied list and must not mutate the DTO fields.</p>
 */
public interface SeriesOrderingStrategy {

    /**
     * Sort series whose matelassage is already complete and that are
     * waiting for a cutting slot.
     *
     * @param series mutable list of ready-to-cut series for one machine
     * @param ctx    machine context (name, type, timing lookup, etc.)
     */
    void sortReadyToCut(List<OrdonnancementService.SerieDTO> series, Context ctx);

    /**
     * Sort series whose matelassage has not started yet (or is still
     * in progress on a different table).
     *
     * @param series mutable list of waiting series for one machine
     * @param ctx    machine context
     */
    void sortWaiting(List<OrdonnancementService.SerieDTO> series, Context ctx);

    /**
     * Context object passed to every sort call.
     */
    class Context {
        /** Machine name (e.g. "LEC-01"). */
        public String machineNom;
        /** Pre-resolved placement → cutting-time map (TimingModel batch). */
        public Map<String, Double> cuttingTimeMap;
        /** Machines whose type is LASER-DXF. */
        public Set<String> laserDxfMachines;
        /** Machines whose type is Gerber. */
        public Set<String> gerberMachines;
    }
}
