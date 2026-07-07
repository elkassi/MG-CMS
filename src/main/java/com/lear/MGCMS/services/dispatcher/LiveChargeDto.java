package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Wire shape for {@code GET /api/dispatcher/liveCharge}. Returns the
 * current physical state of every active sequence: which zone owns it,
 * how much remaining work it carries, and (for each serie) the full math
 * behind that remaining-time number.
 *
 * <p>Designed so the React UI can render the formulas directly:
 * capacity = activeMachines × shiftMinutes × efficiencePct/100,
 * remainingMinutes per serie depending on statusCoupe, etc. Every input
 * to those formulas is exposed as a separate field on this DTO.</p>
 */
public final class LiveChargeDto {

    private final LocalDateTime asOf;
    private final LocalDate date;
    private final int shift;
    /** Shift duration (minutes) used in capacity formula across the response. */
    private final int shiftMinutes;
    private final TotalsDto totals;
    private final List<ZoneChargeDto> zones;
    /** Sequences without an effectiveZone (no STRICT zone can host them right now). */
    private final List<SequenceDto> unassigned;

    public LiveChargeDto(LocalDateTime asOf, LocalDate date, int shift,
                         int shiftMinutes, TotalsDto totals,
                         List<ZoneChargeDto> zones, List<SequenceDto> unassigned) {
        this.asOf = asOf;
        this.date = date;
        this.shift = shift;
        this.shiftMinutes = shiftMinutes;
        this.totals = totals;
        this.zones = zones;
        this.unassigned = unassigned;
    }

    public LocalDateTime getAsOf()         { return asOf; }
    public LocalDate getDate()             { return date; }
    public int getShift()                  { return shift; }
    public int getShiftMinutes()           { return shiftMinutes; }
    public TotalsDto getTotals()           { return totals; }
    public List<ZoneChargeDto> getZones()  { return zones; }
    public List<SequenceDto> getUnassigned(){ return unassigned; }

    // -------------------------------------------------------------------

    public static final class TotalsDto {
        private final int totalSequences;
        private final int lockedSequences;
        private final int pendingSequences;
        private final int unassignedSequences;
        private final double totalRemainingMinutes;
        private final double totalCapacityMinutes;

        public TotalsDto(int totalSequences, int lockedSequences,
                         int pendingSequences, int unassignedSequences,
                         double totalRemainingMinutes, double totalCapacityMinutes) {
            this.totalSequences = totalSequences;
            this.lockedSequences = lockedSequences;
            this.pendingSequences = pendingSequences;
            this.unassignedSequences = unassignedSequences;
            this.totalRemainingMinutes = totalRemainingMinutes;
            this.totalCapacityMinutes = totalCapacityMinutes;
        }

        public int getTotalSequences()         { return totalSequences; }
        public int getLockedSequences()        { return lockedSequences; }
        public int getPendingSequences()       { return pendingSequences; }
        public int getUnassignedSequences()    { return unassignedSequences; }
        public double getTotalRemainingMinutes() { return totalRemainingMinutes; }
        public double getTotalCapacityMinutes()  { return totalCapacityMinutes; }
    }

    // -------------------------------------------------------------------

    public static final class ZoneChargeDto {
        private final String zoneNom;
        private final String category;             // STRICT | SHARED
        private final List<MachineTypeChargeDto> byMachineType;
        private final List<SequenceDto> lockedSequences;
        private final List<SequenceDto> pendingSequences;
        /** Sum across machineType cells. */
        private final double totalRemainingMinutes;
        private final double totalCapacityMinutes;
        private final double overallLoadPct;

        public ZoneChargeDto(String zoneNom, String category,
                             List<MachineTypeChargeDto> byMachineType,
                             List<SequenceDto> lockedSequences,
                             List<SequenceDto> pendingSequences,
                             double totalRemainingMinutes,
                             double totalCapacityMinutes,
                             double overallLoadPct) {
            this.zoneNom = zoneNom;
            this.category = category;
            this.byMachineType = byMachineType;
            this.lockedSequences = lockedSequences;
            this.pendingSequences = pendingSequences;
            this.totalRemainingMinutes = totalRemainingMinutes;
            this.totalCapacityMinutes = totalCapacityMinutes;
            this.overallLoadPct = overallLoadPct;
        }

        public String getZoneNom()        { return zoneNom; }
        public String getCategory()       { return category; }
        public List<MachineTypeChargeDto> getByMachineType()  { return byMachineType; }
        public List<SequenceDto> getLockedSequences()         { return lockedSequences; }
        public List<SequenceDto> getPendingSequences()        { return pendingSequences; }
        public double getTotalRemainingMinutes()              { return totalRemainingMinutes; }
        public double getTotalCapacityMinutes()               { return totalCapacityMinutes; }
        public double getOverallLoadPct()                     { return overallLoadPct; }
    }

    // -------------------------------------------------------------------

    /**
     * Per-(zone, machineType) bucket. The capacity math is fully exposed:
     * {@code capacityMinutes = activeMachines × shiftMinutes × efficiencePct / 100}.
     */
    public static final class MachineTypeChargeDto {
        private final String machineType;
        private final String groupe;            // "Coupe" or "Laser" — picks efficience target
        private final int activeMachines;
        private final int shiftMinutes;
        private final double efficiencePct;
        private final double capacityMinutes;
        private final double lockedRemainingMinutes;
        private final double pendingRemainingMinutes;
        private final double totalRemainingMinutes;
        private final double loadPct;

        public MachineTypeChargeDto(String machineType, String groupe,
                                    int activeMachines, int shiftMinutes,
                                    double efficiencePct, double capacityMinutes,
                                    double lockedRemainingMinutes,
                                    double pendingRemainingMinutes,
                                    double totalRemainingMinutes,
                                    double loadPct) {
            this.machineType = machineType;
            this.groupe = groupe;
            this.activeMachines = activeMachines;
            this.shiftMinutes = shiftMinutes;
            this.efficiencePct = efficiencePct;
            this.capacityMinutes = capacityMinutes;
            this.lockedRemainingMinutes = lockedRemainingMinutes;
            this.pendingRemainingMinutes = pendingRemainingMinutes;
            this.totalRemainingMinutes = totalRemainingMinutes;
            this.loadPct = loadPct;
        }

        public String getMachineType()              { return machineType; }
        public String getGroupe()                   { return groupe; }
        public int getActiveMachines()              { return activeMachines; }
        public int getShiftMinutes()                { return shiftMinutes; }
        public double getEfficiencePct()            { return efficiencePct; }
        public double getCapacityMinutes()          { return capacityMinutes; }
        public double getLockedRemainingMinutes()   { return lockedRemainingMinutes; }
        public double getPendingRemainingMinutes()  { return pendingRemainingMinutes; }
        public double getTotalRemainingMinutes()    { return totalRemainingMinutes; }
        public double getLoadPct()                  { return loadPct; }
    }

    // -------------------------------------------------------------------

    public static final class SequenceDto {
        private final String sequence;
        /** Preferred zone from {@code CuttingRequest.zone.nom}. */
        private final String zoneFix;
        /** Raw {@code dispatchedZone} from the DB. */
        private final String dispatchedZone;
        /** Where the engine + UI consider this sequence to live (priority: lock > engine proposal > dispatchedZone > zoneFix). */
        private final String effectiveZone;
        /**
         * Where {@code effectiveZone} came from. UI uses this to label each
         * sequence: LOCKED, ENGINE_PROPOSED, DISPATCHED, PREFERRED, NONE.
         */
        private final String zoneSource;
        /** True when {@code effectiveZone != dispatchedZone} — overlay/lock override is in play. */
        private final boolean zoneMismatch;
        private final boolean locked;
        /** EXPLICIT_ACCEPTED | IMPLICIT_TABLE_STRICT | null when not locked. */
        private final String lockReason;
        /** For IMPLICIT_TABLE_STRICT — which serie triggered the lock. */
        private final String lockingSerieId;
        private final String lockingTableNom;
        private final String lockingStatusCoupe;
        private final boolean pinnedByChef;
        private final String zoneAcceptanceStatus;
        private final double totalRemainingMinutes;
        private final List<SerieDto> series;
        // Phase 8 fields
        private final java.time.LocalDate dueDate;
        private final double boxCycleTimeMinutes;
        private final String materialStatus;

        public SequenceDto(String sequence, String zoneFix, String dispatchedZone,
                           String effectiveZone, String zoneSource,
                           boolean zoneMismatch, boolean locked,
                           String lockReason, String lockingSerieId,
                           String lockingTableNom, String lockingStatusCoupe,
                           boolean pinnedByChef, String zoneAcceptanceStatus,
                           double totalRemainingMinutes, List<SerieDto> series) {
            this(sequence, zoneFix, dispatchedZone, effectiveZone, zoneSource,
                 zoneMismatch, locked, lockReason, lockingSerieId,
                 lockingTableNom, lockingStatusCoupe, pinnedByChef,
                 zoneAcceptanceStatus, totalRemainingMinutes, series,
                 null, 0.0, null);
        }

        public SequenceDto(String sequence, String zoneFix, String dispatchedZone,
                           String effectiveZone, String zoneSource,
                           boolean zoneMismatch, boolean locked,
                           String lockReason, String lockingSerieId,
                           String lockingTableNom, String lockingStatusCoupe,
                           boolean pinnedByChef, String zoneAcceptanceStatus,
                           double totalRemainingMinutes, List<SerieDto> series,
                           java.time.LocalDate dueDate, double boxCycleTimeMinutes,
                           String materialStatus) {
            this.sequence = sequence;
            this.zoneFix = zoneFix;
            this.dispatchedZone = dispatchedZone;
            this.effectiveZone = effectiveZone;
            this.zoneSource = zoneSource;
            this.zoneMismatch = zoneMismatch;
            this.locked = locked;
            this.lockReason = lockReason;
            this.lockingSerieId = lockingSerieId;
            this.lockingTableNom = lockingTableNom;
            this.lockingStatusCoupe = lockingStatusCoupe;
            this.pinnedByChef = pinnedByChef;
            this.zoneAcceptanceStatus = zoneAcceptanceStatus;
            this.totalRemainingMinutes = totalRemainingMinutes;
            this.series = series;
            this.dueDate = dueDate;
            this.boxCycleTimeMinutes = boxCycleTimeMinutes;
            this.materialStatus = materialStatus;
        }

        public String getSequence()          { return sequence; }
        public String getZoneFix()           { return zoneFix; }
        public String getDispatchedZone()    { return dispatchedZone; }
        public String getEffectiveZone()     { return effectiveZone; }
        public String getZoneSource()        { return zoneSource; }
        public boolean isZoneMismatch()      { return zoneMismatch; }
        public boolean isLocked()            { return locked; }
        public String getLockReason()        { return lockReason; }
        public String getLockingSerieId()    { return lockingSerieId; }
        public String getLockingTableNom()   { return lockingTableNom; }
        public String getLockingStatusCoupe(){ return lockingStatusCoupe; }
        public boolean isPinnedByChef()      { return pinnedByChef; }
        public String getZoneAcceptanceStatus()   { return zoneAcceptanceStatus; }
        public double getTotalRemainingMinutes()  { return totalRemainingMinutes; }
        public List<SerieDto> getSeries()    { return series; }
        public java.time.LocalDate getDueDate()          { return dueDate; }
        public double getBoxCycleTimeMinutes()           { return boxCycleTimeMinutes; }
        public String getMaterialStatus()                { return materialStatus; }
    }

    // -------------------------------------------------------------------

    public static final class SerieDto {
        private final String serie;
        private final String machine;       // machineType (e.g. "Lectra", "Gerber")
        private final Double tempsDeCoupe;  // raw column value (estimate, minutes)
        private final double validatedMinutes; // resolved by CuttingTimeCalculator
        /** VALIDATED | REAL | TEMPS_DE_COUPE | NONE — which source the time came from. */
        private final String timeSource;
        private final String statusCoupe;
        private final String statusMatelassage;
        private final String tableCoupe;
        private final String tableMatelassage;
        private final LocalDateTime dateDebutCoupe;
        private final LocalDateTime dateFinCoupe;
        private final LocalDateTime dateDebutMatelassage;
        private final LocalDateTime dateFinMatelassage;
        /** For In-progress series only: minutes since dateDebutCoupe. 0.0 otherwise. */
        private final double elapsedMinutes;
        /**
         * Remaining work attributable to this serie:
         *   Waiting     → validatedMinutes
         *   In progress → max(0, validatedMinutes - elapsedMinutes)
         *   Complete    → 0
         *   else        → validatedMinutes (treated as Waiting)
         */
        private final double remainingMinutes;
        /**
         * Where this serie's load actually goes. Resolved from the sequence's
         * effectiveZone if it hosts the machine type, else a SHARED zone that
         * does (DIE / Gerber / LASER-DXF case), else any STRICT zone hosting
         * it. Null when no zone hosts the machine type at all.
         */
        private final String targetZoneNom;
        /** STRICT or SHARED — paired with targetZoneNom. */
        private final String targetZoneCategory;
        // Phase 8 fields
        private final String refTissus;
        private final String materialStatus;
        private final double tableLengthRequired;

        public SerieDto(String serie, String machine, Double tempsDeCoupe,
                        double validatedMinutes, String timeSource,
                        String statusCoupe, String statusMatelassage,
                        String tableCoupe, String tableMatelassage,
                        LocalDateTime dateDebutCoupe, LocalDateTime dateFinCoupe,
                        LocalDateTime dateDebutMatelassage, LocalDateTime dateFinMatelassage,
                        double elapsedMinutes, double remainingMinutes,
                        String targetZoneNom, String targetZoneCategory) {
            this(serie, machine, tempsDeCoupe, validatedMinutes, timeSource,
                 statusCoupe, statusMatelassage, tableCoupe, tableMatelassage,
                 dateDebutCoupe, dateFinCoupe, dateDebutMatelassage, dateFinMatelassage,
                 elapsedMinutes, remainingMinutes, targetZoneNom, targetZoneCategory,
                 null, null, 0.0);
        }

        public SerieDto(String serie, String machine, Double tempsDeCoupe,
                        double validatedMinutes, String timeSource,
                        String statusCoupe, String statusMatelassage,
                        String tableCoupe, String tableMatelassage,
                        LocalDateTime dateDebutCoupe, LocalDateTime dateFinCoupe,
                        LocalDateTime dateDebutMatelassage, LocalDateTime dateFinMatelassage,
                        double elapsedMinutes, double remainingMinutes,
                        String targetZoneNom, String targetZoneCategory,
                        String refTissus, String materialStatus,
                        double tableLengthRequired) {
            this.serie = serie;
            this.machine = machine;
            this.tempsDeCoupe = tempsDeCoupe;
            this.validatedMinutes = validatedMinutes;
            this.timeSource = timeSource;
            this.statusCoupe = statusCoupe;
            this.statusMatelassage = statusMatelassage;
            this.tableCoupe = tableCoupe;
            this.tableMatelassage = tableMatelassage;
            this.dateDebutCoupe = dateDebutCoupe;
            this.dateFinCoupe = dateFinCoupe;
            this.dateDebutMatelassage = dateDebutMatelassage;
            this.dateFinMatelassage = dateFinMatelassage;
            this.elapsedMinutes = elapsedMinutes;
            this.remainingMinutes = remainingMinutes;
            this.targetZoneNom = targetZoneNom;
            this.targetZoneCategory = targetZoneCategory;
            this.refTissus = refTissus;
            this.materialStatus = materialStatus;
            this.tableLengthRequired = tableLengthRequired;
        }

        public String getSerie()                 { return serie; }
        public String getMachine()               { return machine; }
        public Double getTempsDeCoupe()          { return tempsDeCoupe; }
        public double getValidatedMinutes()      { return validatedMinutes; }
        public String getTimeSource()            { return timeSource; }
        public String getStatusCoupe()           { return statusCoupe; }
        public String getStatusMatelassage()     { return statusMatelassage; }
        public String getTableCoupe()            { return tableCoupe; }
        public String getTableMatelassage()      { return tableMatelassage; }
        public LocalDateTime getDateDebutCoupe()       { return dateDebutCoupe; }
        public LocalDateTime getDateFinCoupe()         { return dateFinCoupe; }
        public LocalDateTime getDateDebutMatelassage() { return dateDebutMatelassage; }
        public LocalDateTime getDateFinMatelassage()   { return dateFinMatelassage; }
        public double getElapsedMinutes()        { return elapsedMinutes; }
        public double getRemainingMinutes()      { return remainingMinutes; }
        public String getTargetZoneNom()         { return targetZoneNom; }
        public String getTargetZoneCategory()    { return targetZoneCategory; }
        public String getRefTissus()             { return refTissus; }
        public String getMaterialStatus()        { return materialStatus; }
        public double getTableLengthRequired()   { return tableLengthRequired; }
    }
}
