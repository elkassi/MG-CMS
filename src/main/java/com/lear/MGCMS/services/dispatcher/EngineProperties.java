package com.lear.MGCMS.services.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bound to {@code mgcms.engine.*}. Feature-flag block for the Phase 7
 * zone-aware engine surface; kept separate from {@link DispatcherProperties}
 * because the engine can be turned on/off independently of the dispatcher
 * UI (e.g. running the engine in shadow mode while keeping the Process
 * page hidden).
 *
 * <p>Known keys:</p>
 * <ul>
 *   <li>{@code mgcms.engine.zoneAware} — when true, the ordonnancement
 *       engine filters series by {@code dispatched_zone} and respects
 *       {@code pinnedByChef} when re-sequencing queues.</li>
 *   <li>{@code mgcms.engine.autoTick.enabled} — master switch for the
 *       Phase 7 scheduled {@code autoDispatch} task.</li>
 *   <li>{@code mgcms.engine.autoTick.cron} — cron expression (default
 *       {@code 0 *\/5 * * * *}) — every 5 minutes.</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "mgcms.engine")
public class EngineProperties {

    private boolean zoneAware = false;
    private AutoTick autoTick = new AutoTick();
    private Optimizer optimizer = new Optimizer();

    public boolean isZoneAware() { return zoneAware; }
    public void setZoneAware(boolean zoneAware) { this.zoneAware = zoneAware; }

    public AutoTick getAutoTick() { return autoTick; }
    public void setAutoTick(AutoTick autoTick) { this.autoTick = autoTick; }

    public Optimizer getOptimizer() { return optimizer; }
    public void setOptimizer(Optimizer optimizer) { this.optimizer = optimizer; }

    public static class AutoTick {
        private boolean enabled = false;
        private String cron = "0 */5 * * * *";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }
    }

    /**
     * Tunables for {@link ContinuousDispatchOptimizerService}.
     *
     * <p>Objective weights and simulated-annealing knobs are exposed here so
     * the engine can be tuned without a redeploy. Weight defaults are aimed
     * at Option C (full scheduling with box-duration KPI); set unused weights
     * to 0 to disable that term.</p>
     */
    public static class Optimizer {
        // ---- convergence / loop control ----
        /**
         * When the engine is running indefinitely and the best cost has not
         * improved for this many consecutive iterations, the loop slows down
         * (sleeps {@link #convergedSleepMs} between iterations) instead of
         * exiting. Set to {@code 0} to disable convergence throttling.
         */
        private int convergenceIterations = 2000;
        private long convergedSleepMs = 10_000L;
        private double improvementEpsilon = 0.05;

        // ---- objective weights ----
        private double cycleAvgWeight = 0.0;
        private double cycleMaxWeight = 0.0;
        private double spreadWeight = 1.0;
        private double latenessWeight = 0.0;
        /** Mean box-duration (sequence time / box count) weight — Option C primary KPI. */
        private double boxDurationWeight = 0.0;
        /** Max box-duration weight — guards the worst-case sequence. */
        private double boxDurationMaxWeight = 0.0;
        /** Intra-zone load spread (per machine type within a zone). */
        private double intraZoneMachineLoadWeight = 0.0;
        /**
         * Per-MT spread fairness — adds {@code mtFairnessWeight * max(spread_mt)}
         * to the cost so the engine cannot fix the high-volume machine type while
         * ignoring the low-volume one. Without this, the historical formulation
         * (load-share weighted mean of per-MT spreads) lets Lectra dominate the
         * gradient and Lectra IP6 drifts free. Default 1.0 = equal weight with
         * the weighted mean.
         */
        private double mtFairnessWeight = 1.0;
        private double overloadWeight = 5.0;

        // ---- Slice 3 / Slice 4 move kinds (off by default — see plan §9) ----
        /** Slice 3: enable per-iteration serie→machine moves within the same zone. */
        private boolean level2Enabled = false;
        /** Per-iteration probability of attempting a Level-2 move when enabled. */
        private double level2MoveProbability = 0.10;
        /** Slice 4: enable per-iteration adjacent queue-swap on a machine. */
        private boolean level3Enabled = false;
        /** Per-iteration probability of attempting a Level-3 move when enabled. */
        private double level3MoveProbability = 0.10;
        /** Material-not-in-zone advisory penalty (small — never dominates). */
        private double materialAlertWeight = 0.0;

        // ---- due-date urgency curve: weight = 1 + dueDateGain / (1 + days). ----
        private double dueDateGain = 9.0;

        // ---- simulated annealing knobs ----
        private double temperatureInitial = 5.0;
        private double temperatureMin = 0.05;
        private double temperatureCooling = 0.9995;
        private int kickAfter = 75;
        private int kickMoves = 12;
        private int rebalanceEvery = 40;
        private int randomizeAfter = 120;
        private double randomizeFraction = 0.12;
        private int multiCandidateThreshold = 25;
        private int sampleEvery = 500;

        // ---- ALTERNATING-mode phase durations ----
        private long dispatchPhaseMs = 20_000L;
        private long ordonnancementPhaseMs = 40_000L;

        public int getConvergenceIterations() { return convergenceIterations; }
        public void setConvergenceIterations(int v) { this.convergenceIterations = v; }

        public long getConvergedSleepMs() { return convergedSleepMs; }
        public void setConvergedSleepMs(long v) { this.convergedSleepMs = v; }

        public double getImprovementEpsilon() { return improvementEpsilon; }
        public void setImprovementEpsilon(double v) { this.improvementEpsilon = v; }

        public double getCycleAvgWeight() { return cycleAvgWeight; }
        public void setCycleAvgWeight(double v) { this.cycleAvgWeight = v; }

        public double getCycleMaxWeight() { return cycleMaxWeight; }
        public void setCycleMaxWeight(double v) { this.cycleMaxWeight = v; }

        public double getSpreadWeight() { return spreadWeight; }
        public void setSpreadWeight(double v) { this.spreadWeight = v; }

        public double getLatenessWeight() { return latenessWeight; }
        public void setLatenessWeight(double v) { this.latenessWeight = v; }

        public double getBoxDurationWeight() { return boxDurationWeight; }
        public void setBoxDurationWeight(double v) { this.boxDurationWeight = v; }

        public double getBoxDurationMaxWeight() { return boxDurationMaxWeight; }
        public void setBoxDurationMaxWeight(double v) { this.boxDurationMaxWeight = v; }

        public double getOverloadWeight() { return overloadWeight; }
        public void setOverloadWeight(double v) { this.overloadWeight = v; }

        public double getIntraZoneMachineLoadWeight() { return intraZoneMachineLoadWeight; }
        public void setIntraZoneMachineLoadWeight(double v) { this.intraZoneMachineLoadWeight = v; }

        public double getMtFairnessWeight() { return mtFairnessWeight; }
        public void setMtFairnessWeight(double v) { this.mtFairnessWeight = v; }

        public boolean isLevel2Enabled() { return level2Enabled; }
        public void setLevel2Enabled(boolean v) { this.level2Enabled = v; }

        public double getLevel2MoveProbability() { return level2MoveProbability; }
        public void setLevel2MoveProbability(double v) { this.level2MoveProbability = v; }

        public boolean isLevel3Enabled() { return level3Enabled; }
        public void setLevel3Enabled(boolean v) { this.level3Enabled = v; }

        public double getLevel3MoveProbability() { return level3MoveProbability; }
        public void setLevel3MoveProbability(double v) { this.level3MoveProbability = v; }

        public double getMaterialAlertWeight() { return materialAlertWeight; }
        public void setMaterialAlertWeight(double v) { this.materialAlertWeight = v; }

        public double getDueDateGain() { return dueDateGain; }
        public void setDueDateGain(double v) { this.dueDateGain = v; }

        public double getTemperatureInitial() { return temperatureInitial; }
        public void setTemperatureInitial(double v) { this.temperatureInitial = v; }

        public double getTemperatureMin() { return temperatureMin; }
        public void setTemperatureMin(double v) { this.temperatureMin = v; }

        public double getTemperatureCooling() { return temperatureCooling; }
        public void setTemperatureCooling(double v) { this.temperatureCooling = v; }

        public int getKickAfter() { return kickAfter; }
        public void setKickAfter(int v) { this.kickAfter = v; }

        public int getKickMoves() { return kickMoves; }
        public void setKickMoves(int v) { this.kickMoves = v; }

        public int getRebalanceEvery() { return rebalanceEvery; }
        public void setRebalanceEvery(int v) { this.rebalanceEvery = v; }

        public int getRandomizeAfter() { return randomizeAfter; }
        public void setRandomizeAfter(int v) { this.randomizeAfter = v; }

        public double getRandomizeFraction() { return randomizeFraction; }
        public void setRandomizeFraction(double v) { this.randomizeFraction = v; }

        public int getMultiCandidateThreshold() { return multiCandidateThreshold; }
        public void setMultiCandidateThreshold(int v) { this.multiCandidateThreshold = v; }

        public int getSampleEvery() { return sampleEvery; }
        public void setSampleEvery(int v) { this.sampleEvery = v; }

        public long getDispatchPhaseMs() { return dispatchPhaseMs; }
        public void setDispatchPhaseMs(long v) { this.dispatchPhaseMs = v; }

        public long getOrdonnancementPhaseMs() { return ordonnancementPhaseMs; }
        public void setOrdonnancementPhaseMs(long v) { this.ordonnancementPhaseMs = v; }
    }
}
