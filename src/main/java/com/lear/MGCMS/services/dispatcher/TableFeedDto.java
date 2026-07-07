package com.lear.MGCMS.services.dispatcher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Wire shape for {@code GET /api/dispatcher/tableFeed}. A read-only advisor
 * that tells the chef, for every matelassage (spreading) table about to go
 * idle, which serie to mount next so the downstream CNC cutter never starves.
 *
 * <h2>Shape</h2>
 * <pre>
 *   TableFeedDto
 *     └─ zones[]            (one per active zone + shift)
 *          └─ tables[]      (one per UP table in the zone)
 *               ├─ timeToIdleMinutes  — when the table runs dry (see formula below)
 *               └─ candidates[]       — ordered top-N series to mount next
 *                      └─ reasons[]   — why this serie scored where it did
 * </pre>
 *
 * <h2>time-to-idle</h2>
 * For each UP table we blend two runways and take the EARLIER (the table
 * starves whenever the first of the two runs dry):
 * <ul>
 *   <li><b>spreadingRunway</b> — {@code SerieRouleauTemp.date + estimationRest}
 *       for the roll currently mounted on that table ({@code estimationRest}
 *       treated as remaining minutes). Null when no roll is mounted.</li>
 *   <li><b>cutQueueRunway</b> — the latest {@code MachineQueue.estimatedEndTime}
 *       across the table's loaded FIFO queue (when its cut backlog empties).
 *       Null when the queue is empty.</li>
 * </ul>
 * {@code timeToIdleMinutes = minutesFromNow(min(spreadingIdleAt, cutQueueIdleAt))};
 * 0 when both are null (idle now / unknown — flagged {@code idleNow}).
 *
 * <p>Read-only: never mutates state.</p>
 */
public final class TableFeedDto {

    private final LocalDateTime asOf;
    private final LocalDate date;
    private final int shift;
    private final int topN;
    private final List<ZoneFeedDto> zones;

    public TableFeedDto(LocalDateTime asOf, LocalDate date, int shift,
                        int topN, List<ZoneFeedDto> zones) {
        this.asOf = asOf;
        this.date = date;
        this.shift = shift;
        this.topN = topN;
        this.zones = zones;
    }

    public LocalDateTime getAsOf()        { return asOf; }
    public LocalDate getDate()            { return date; }
    public int getShift()                 { return shift; }
    public int getTopN()                  { return topN; }
    public List<ZoneFeedDto> getZones()   { return zones; }

    // -------------------------------------------------------------------

    public static final class ZoneFeedDto {
        private final String zoneNom;
        private final String category;          // STRICT | SHARED
        private final List<TableRowDto> tables;

        public ZoneFeedDto(String zoneNom, String category, List<TableRowDto> tables) {
            this.zoneNom = zoneNom;
            this.category = category;
            this.tables = tables;
        }

        public String getZoneNom()           { return zoneNom; }
        public String getCategory()          { return category; }
        public List<TableRowDto> getTables() { return tables; }
    }

    // -------------------------------------------------------------------

    public static final class TableRowDto {
        private final String tableNom;
        private final String machineType;
        private final Double tableLength;
        /** reftissu of the roll mounted on the table right now (affinity anchor). */
        private final String mountedRefTissu;
        /** When the spreading roll runs dry (date + estimationRest). Null if none mounted. */
        private final LocalDateTime spreadingIdleAt;
        /** When the cut queue empties (max estimatedEndTime). Null if queue empty. */
        private final LocalDateTime cutQueueIdleAt;
        /** Minutes from now to the earlier of the two horizons. 0 when idle now / unknown. */
        private final double timeToIdleMinutes;
        /** True when neither horizon is known — treat as needs-feed-now. */
        private final boolean idleNow;
        private final List<CandidateDto> candidates;

        public TableRowDto(String tableNom, String machineType, Double tableLength,
                           String mountedRefTissu, LocalDateTime spreadingIdleAt,
                           LocalDateTime cutQueueIdleAt, double timeToIdleMinutes,
                           boolean idleNow, List<CandidateDto> candidates) {
            this.tableNom = tableNom;
            this.machineType = machineType;
            this.tableLength = tableLength;
            this.mountedRefTissu = mountedRefTissu;
            this.spreadingIdleAt = spreadingIdleAt;
            this.cutQueueIdleAt = cutQueueIdleAt;
            this.timeToIdleMinutes = timeToIdleMinutes;
            this.idleNow = idleNow;
            this.candidates = candidates;
        }

        public String getTableNom()               { return tableNom; }
        public String getMachineType()            { return machineType; }
        public Double getTableLength()            { return tableLength; }
        public String getMountedRefTissu()        { return mountedRefTissu; }
        public LocalDateTime getSpreadingIdleAt() { return spreadingIdleAt; }
        public LocalDateTime getCutQueueIdleAt()  { return cutQueueIdleAt; }
        public double getTimeToIdleMinutes()      { return timeToIdleMinutes; }
        public boolean isIdleNow()                { return idleNow; }
        public List<CandidateDto> getCandidates() { return candidates; }
    }

    // -------------------------------------------------------------------

    public static final class CandidateDto {
        private final String serie;
        private final String sequence;
        private final String refTissus;
        private final String machine;          // machineType of the serie
        private final String effectiveZone;
        private final String statusCoupe;
        private final String statusMatelassage;
        private final String sequenceStatus;
        private final boolean ready;           // legacy compatibility; false for Waiting/Waiting next-mount recommendations
        private final boolean sameRefTissuMounted;
        private final boolean materialInZone;
        private final boolean completesLockedSequence;
        private final boolean fitsTableLength;
        private final Double longueur;
        private final Integer nbrCouche;
        private final double requiredLength;   // total fabric need: longueur x nbrCouche
        private final LocalDate dueDate;
        private final double validatedMinutes;
        private final double score;
        private final List<String> reasons;

        public CandidateDto(String serie, String sequence, String refTissus,
                            String machine, String effectiveZone, String statusCoupe,
                            String statusMatelassage, String sequenceStatus, boolean ready,
                            boolean sameRefTissuMounted, boolean materialInZone,
                            boolean completesLockedSequence, boolean fitsTableLength,
                            Double longueur, Integer nbrCouche,
                            double requiredLength, LocalDate dueDate,
                            double validatedMinutes, double score, List<String> reasons) {
            this.serie = serie;
            this.sequence = sequence;
            this.refTissus = refTissus;
            this.machine = machine;
            this.effectiveZone = effectiveZone;
            this.statusCoupe = statusCoupe;
            this.statusMatelassage = statusMatelassage;
            this.sequenceStatus = sequenceStatus;
            this.ready = ready;
            this.sameRefTissuMounted = sameRefTissuMounted;
            this.materialInZone = materialInZone;
            this.completesLockedSequence = completesLockedSequence;
            this.fitsTableLength = fitsTableLength;
            this.longueur = longueur;
            this.nbrCouche = nbrCouche;
            this.requiredLength = requiredLength;
            this.dueDate = dueDate;
            this.validatedMinutes = validatedMinutes;
            this.score = score;
            this.reasons = reasons;
        }

        public String getSerie()                    { return serie; }
        public String getSequence()                 { return sequence; }
        public String getRefTissus()                { return refTissus; }
        public String getMachine()                  { return machine; }
        public String getEffectiveZone()            { return effectiveZone; }
        public String getStatusCoupe()              { return statusCoupe; }
        public String getStatusMatelassage()        { return statusMatelassage; }
        public String getSequenceStatus()           { return sequenceStatus; }
        public boolean isReady()                    { return ready; }
        public boolean isSameRefTissuMounted()      { return sameRefTissuMounted; }
        public boolean isMaterialInZone()           { return materialInZone; }
        public boolean isCompletesLockedSequence()  { return completesLockedSequence; }
        public boolean isFitsTableLength()          { return fitsTableLength; }
        public Double getLongueur()                 { return longueur; }
        public Integer getNbrCouche()               { return nbrCouche; }
        public double getRequiredLength()           { return requiredLength; }
        public LocalDate getDueDate()               { return dueDate; }
        public double getValidatedMinutes()         { return validatedMinutes; }
        public double getScore()                    { return score; }
        public List<String> getReasons()            { return reasons; }
    }
}
