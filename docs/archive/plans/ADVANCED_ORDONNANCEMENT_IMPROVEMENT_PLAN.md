# Advanced Ordonnancement — Improvement Plan (v2)

> **Project:** MG-CMS (Spring Boot + React + SQL Server) + CMS-Prod —
> Lear Corp. Trim 1, Tangier TFZ.
> **Plant scale used as design input:** ~30 production tables, ~400 series / shift,
> serie duration 5–90 min, 3 shifts / day.
> **Authoritative scheduling docs (the only ones we keep):**
> [md/Advanced_Ordonancement.md](../md/Advanced_Ordonancement.md) and
> [md/PLAN_DE_CHARGE.md](../md/PLAN_DE_CHARGE.md).
> Every other md file describing ordonnancement / scheduling entities is
> obsolete and must be deleted (see §11).

---

## 0. Core idea in one paragraph

> ⚠️ **Second of two engines.** This plan covers the **Ordonnancement
> Engine** — serie ↔ machine ↔ time assignment *within a confirmed
> zone*. The first engine (sequence → STRICT zone) is the **Dispatching
> Engine** described in `plans/SEQUENCE_DISPATCHER_PLAN.md §13`. The two
> are decoupled: the Ordonnancement Engine reads `dispatchedZone` /
> `zoneAcceptanceStatus = ACCEPTED` as a hard input and never moves
> sequences across zones. They have separate state machines, separate
> threads, separate REST surfaces, separate WebSocket topics, and
> **separate UIs**: dispatcher config lives on `ProcessDispatcher.js`;
> ordonnancement config lives on `AdvancedOrdonnancement.js` and is
> gated to `ROLE_PROCESS` only. The Dispatching Engine ships first; this
> plan is implemented after that work is shipped and stable.

We keep the current `Advanced_Ordonancement.md` model (MachineQueue,
four views, SCG greedy baseline, collision-free estimator) and the current
`PLAN_DE_CHARGE.md` model (EtatMachineHistorique, ShiftLoadCalculation,
machine maintenance windows). On top of them we add **one new moving part**:
a **Continuous Optimization Engine** that keeps thinking on the server,
reacts to every change in the cutting area, and constantly tries to produce
a better plan — where *better* is measured by the **box-duration KPI**
(sequence span ÷ number of boxes). The engine can be started and stopped
from the `AdvancedOrdonnancement` component, and the UI shows a live
improvement curve so anyone can see that the plant is converging on a
lower max / median / variance of box duration.

The engine **runs one optimiser per STRICT zone in parallel** (user
answer §14 Q10) — each one constrained to its zone's machines plus the
SHARED-zone overflow defined in `SEQUENCE_DISPATCHER_PLAN §3.8`. This
keeps each loop's search space small enough for sub-second perturbations
and isolates a sick zone from poisoning the global plan. A single
coordination thread merges the per-zone solutions into one
`MachineQueue` write batch.

The engine is the **downstream** consumer of the Sequence Dispatcher
(plans/SEQUENCE_DISPATCHER_PLAN.md). Inputs it must respect:
- `CuttingRequest.dispatchedZone` (sequence → STRICT zone) and
  `zoneAcceptanceStatus = ACCEPTED` (chef has confirmed).
- `ShiftZoneConfirmationMachine` (which machines are actually live this
  shift, per the chef).
- `CuttingTimeCalculator` bean (single source for cutting time —
  contract C1 in dispatcher §8.2 — joins CMS-DB `TimingModel` by
  placement and applies LASER × nbrCouche / Gerber × 2 / 90 %
  efficiency multiplier).
- `CapaciteInstallee.efficienceTarget` (single source for efficiency —
  contract C5).
- `SerieTableAssigner` bean (single source for "best table for serie" —
  reused, never re-implemented; see dispatcher §3.9).

Everything else in this plan (spreading admission control, missing-date
self-healing, KPI dashboard, documentation cleanup) is in service of that
engine.

---

## 1. What's already in place — build on, don't rebuild

| Capability | Where it lives | Verdict |
|---|---|---|
| Four views (Réel / Planifié Coupe / Planifié Matelassage / Table) | `AdvancedOrdonnancement.js` + `OrdonnancementService` | Keep as-is |
| `MachineQueue` (next-N series per machine) | `md/Advanced_Ordonancement.md` §Data Model | Keep — becomes the write target of the engine |
| Greedy SCG + SPT/LPT/EDF/CR/WSPT/ATC/MATERIAL_GROUP strategies | `OrdonnancementService` | Keep — the engine uses SCG as warm-start |
| Composite objective `max × median × variance` of `span/num_series` | `md/Advanced_Ordonancement.md` L284, L308–317 | **Change N to num_boxes** (see §4.1) |
| Machine state (M / A / P / O / R / MS / AD / ADM / MD / PN) + PM windows | `EtatMachineHistorique` in `PLAN_DE_CHARGE.md` | Keep — the engine reads from here for feasibility & maintenance |
| Shift times + working calendar | `PLAN_DE_CHARGE.md` §Shift Schedule | Extend with a `WorkCalendar` override table (see §3.1) |
| Missing `dateFinCoupe` inference from Lectra logs | `CMS-Prod ScheduledTask.reviewSerieWaiting()` L1112–1186 (`@Scheduled` — currently commented-out on L42) + `CoupeMachineHistoryRepository.findBySerieNoExtra()` L34–38 | **Re-enable** and extend (see §5) |
| `ScanRouleau` — rolls currently on the cutting racks | existing | Read-only input for feasibility filter |

We do **not** introduce new `SchedulingPlan` / `ScheduledSerieAssignment` /
`ZoneBinding` entities. That proposal in v1 of this plan is withdrawn —
it duplicated `MachineQueue` and added weight. Instead we add two small
tables that only the engine needs (see §3).

---

## 2. The Continuous Optimization Engine — architecture

### 2.0 Multi-zone parallelism (per user §14 Q10)

The engine is **not** one monolithic optimiser over the whole plant.
It is a small set of per-STRICT-zone optimisers running in parallel,
plus one coordination thread that merges their proposals.

```
┌──────────────────────────────────────────────────────────────────┐
│ ZoneOptimizerThread (one per STRICT zone — e.g. First Article,   │
│                       Serie, Prototype)                          │
│   inputs  : sequences with dispatchedZone = myZone AND           │
│             zoneAcceptanceStatus = ACCEPTED                      │
│             machines with m.zone = myZone (active)               │
│   outputs : per-machine queue proposal for myZone                │
│             plus a list of shared-zone serie requests            │
│             (LASER-DXF / LASER-LSR overflow)                     │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ SharedZoneArbiterThread (one per SHARED zone)                    │
│   inputs  : shared-zone serie requests collected from all        │
│             ZoneOptimizerThreads                                 │
│             machines with m.zone = shared zone (active)          │
│   outputs : per-machine queue proposal for the shared zone       │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ CoordinationThread (single)                                      │
│   merges ZoneOptimizer + SharedZoneArbiter proposals into        │
│   one MachineQueue write batch (saveAll, batch_size = 100)       │
│   publishes one WebSocket update per touched machine             │
└──────────────────────────────────────────────────────────────────┘
```

Why parallel:
- Each STRICT zone's search space is small (≈ 80 series, ≈ 5 machines)
  → sub-millisecond perturbations.
- A sick zone (bad data, all machines down) cannot poison the global
  plan — its thread stalls, the others keep improving.
- Mirrors the chef's mental model: each chef owns their zone and the
  KPIs visible on their `/production/chefDeZone` page.

Bounded budget:
- Each `ZoneOptimizerThread` gets ≤ 1 s of CPU per 10 s of wall time
  (= 100 ms / s). With ≤ 5 STRICT zones the total stays ≤ 50 % of one
  core.
- The `CoordinationThread` writes at most every 2 s — single transaction
  per write window.



```
                                ┌──────────────────────────────────────┐
                                │  ContinuousOptimizerService (Spring) │
  ┌─── events ─────────────►    │  state: IDLE | WARMING | IMPROVING   │
  │  serie status change         │         | PAUSED | STOPPED          │
  │  machine status change       │                                     │
  │  plan-de-charge edit         │  improvement loop (async thread):   │
  │  rack scan                   │    1. snapshot = current floor state │
  │  user start/stop             │    2. warm-start = SCG baseline     │
  │                              │    3. loop forever while RUNNING:   │
  │  ◄── WebSocket push ─────    │         neighbour = perturb(best)   │
  │     /topic/ordo/engine       │         score   = evaluate(neigh.)  │
  │                              │         if better → best = neigh.  │
  │                              │         publish (throttled, 2 s)   │
  │                              │         record indicator sample     │
  │                              └──────────────────────────────────────┘
                                          │                    │
                                          ▼                    ▼
                                  MachineQueue table    OptimizerIndicatorSample
                                  (best-so-far is        (indicator time series,
                                  written here, and      fed to the live UI chart)
                                  served to all
                                  operator screens)
```

### 2.1 Lifecycle
- **IDLE** — engine boot but no plan yet.
- **WARMING** — building the first SCG solution, publishing it to `MachineQueue`.
- **IMPROVING** — perturbation loop; every improved solution overwrites the
  best-so-far and pushes a WebSocket event.
- **PAUSED** — loop stopped; last best remains served; can resume instantly.
- **STOPPED** — engine off; `MachineQueue` keeps last state until the next run.

### 2.2 Triggers that invalidate the current best
- `statusCoupe` or `statusMatelassage` change on any serie → snapshot refresh.
- Machine state change (`M ↔ A/P/O/R/...`) → snapshot refresh.
- `PLAN_DE_CHARGE` row edit (PM window added, operator count changed) → snapshot refresh.
- `ScanRouleau` update → feasibility re-check.
- New sequence / serie created in Preparation → incorporated on the next loop.

The loop is **stateful but restartable**: on snapshot refresh we keep the
current best if it's still feasible; otherwise we re-warm-start from SCG.

### 2.3 Perturbation moves (in order of cheapness)
1. **Swap** — swap two adjacent series on the same machine.
2. **Relocate** — move one serie from machine A to machine B (sequence-affinity
   aware; only valid machines per sequence zone — see §3.2).
3. **Sequence block move** — move a whole sub-block of a sequence to another
   machine (reduces `span(seq)` directly).
4. **2-opt on a machine** — reverse a slice of the queue.
5. **Kick** — perform 3 random relocates then re-anneal (escape local optima).

Cheap moves run hundreds of times per second; kicks are rare. The budget
is bounded: no more than **1 s of CPU per 10 s of wall time** so the
engine never starves the web tier.

### 2.4 Objective function
See §4.1. The engine is objective-agnostic — the function is a
`ToDoubleFunction<Schedule>` bean so we can A/B it.

### 2.5 Feasibility guards (hard — never violated)

These are now defined exhaustively by `SEQUENCE_DISPATCHER_PLAN §8.4`.
The engine MUST read those rules verbatim — do not duplicate. Recap of
what HARD means here:

- Collision-free per machine (reuse `findEarliestSlot`).
- `dateDebutCoupe ≥ dateFinMatelassage` on the same serie (except LASER-DXF parallel).
- Serie executed only on a machine listed `active` in
  `ShiftZoneConfirmationMachine` for `(date, shift)` — falls back to
  `EtatMachineHistorique.status ∈ {M, MS, MD, R}` only when no chef
  confirmation exists (`ActiveMachineResolver` bean — contract C2).
- **Machine type match.** A serie `s` runs only on a `ProductionTable`
  `m` where `m.machineType.nom = s.machine` (the serie's `machine`
  column carries the machine-type NAME, not a machine ID — see
  dispatcher §3.6).
- **Zone binding HARD inside the accepted scope.** A serie may run on
  `m` only if either:
    1. `m.zone = sequence.dispatchedZone` AND
       `sequence.zoneAcceptanceStatus = ACCEPTED`, OR
    2. `m.zone.category = SHARED` AND the serie's `machineType` is
       **not** present in `sequence.dispatchedZone` (overflow), OR
    3. `sequence.dispatchedZone IS NULL` AND
       `m.zone.category = SHARED` (purely SHARED sequence).
  Anything else is a hard violation — *not* a SOFT penalty. The earlier
  v1 stance ("SOFT zone binding everywhere") is **superseded** by this
  rule per the user's clarification on §14 Q2: total boxes per zone are
  bounded (≈ 16 × N_machines), so we cannot let a serie spill onto a
  different STRICT zone the chef has not accepted.
- Serie requires a material roll currently present on the rack of the
  target zone (join with `ScanRouleau` — user answer #6).
- Working calendar (§3.1): by default Sunday shifts 1, 2, 3 are closed;
  engine still schedules into them if a sequence has `dueDate` + `dueShift`
  falling there (user answer #1).

### 2.5.1 Soft penalties (allowed but discouraged)

- **SHARED-overflow temptation** (cost `w_zone`): the type IS present in
  the sequence's `dispatchedZone` but the engine is tempted to push the
  serie onto a SHARED zone anyway. Discouraged but not forbidden.
- **Late-add penalty** (cost `w_latecapacity`): a machine added more
  than 2 h after T0 (`ZoneMachineToggledEvent.whenDuringShift > T0+2h`)
  loses some of its weight in the score because operators need warm-up.
- **End-stack penalty** (cost `w_endstack`): a machine whose queue is
  past 90 % of its shift capacity. Same formula as
  `SEQUENCE_DISPATCHER_PLAN §3.9` — reused bean.

---

## 3. Minimal new data model (only two small tables)

No large new persistence layer. Just two helpers.

### 3.1 `WorkCalendar` — which shifts do we work?
```
id                  BIGINT IDENTITY PK
shift_date          DATE NOT NULL
shift_number        INT NOT NULL      -- 1, 2, 3
working             BIT NOT NULL      -- 0 = closed by default
override_reason     NVARCHAR(256) NULL
UNIQUE (shift_date, shift_number)
```

Default: a DB constraint / init script seeds `working = 0` for every
Sunday shift 1/2/3 going forward. Any other day/shift → `working = 1`.
UI allows Process to open/close individual shifts manually.

**Engine rule.** A shift is usable if `working = 1` OR if any sequence
with `dueDate = shift_date AND dueShift = shift_number` has at least one
serie still to cut (user answer #1). In that case the engine treats it
as working for those series only.

### 3.2 `OptimizerRun` + `OptimizerIndicatorSample` — the improvement trace
```
OptimizerRun
  id                 BIGINT IDENTITY PK
  started_at         DATETIME2 NOT NULL
  started_by         BIGINT FK User
  stopped_at         DATETIME2 NULL
  stopped_by         BIGINT NULL
  algo_config_json   NVARCHAR(MAX)   -- weights, budget, seed

OptimizerIndicatorSample
  id                 BIGINT IDENTITY PK
  run_id             BIGINT FK OptimizerRun
  ts                 DATETIME2 NOT NULL
  iterations         INT
  box_dur_max        DECIMAL(10,2)
  box_dur_median     DECIMAL(10,2)
  box_dur_variance   DECIMAL(12,4)
  wip_sequences      INT
  plant_idle_ratio   DECIMAL(5,4)
  first_box_ready_p50 DECIMAL(10,2)
  score              DECIMAL(18,4)    -- composite
  INDEX (run_id, ts)
```

This is what feeds the live chart in the UI. No history of actual *plans*
is persisted — the plan **is** `MachineQueue`, which already exists.

### 3.3 What we intentionally do NOT add
- No `SchedulingPlan` header table.
- No `ScheduledSerieAssignment` line table.
- No separate `ZoneBinding` table — the zone constraint is the persisted
  column `CuttingRequest.dispatchedZone` (added by
  `SEQUENCE_DISPATCHER_PLAN §3.2`) joined against `ProductionTable.zone`.
  The earlier "violations are SOFT penalties" stance is superseded by
  the HARD rules in `SEQUENCE_DISPATCHER_PLAN §8.4` — see this plan's
  §2.5.
- No `MachineWorklistCache` — `MachineQueue` already plays that role.
- No `ZoneEfficiency` table — efficiency is read from the existing
  `CapaciteInstallee.efficienceTarget` keyed by
  `(date, shift, groupe)` (`Coupe` for Lectra/Lectra IP6/Gerber,
  `Laser` for LASER-DXF/LSR — see `SEQUENCE_DISPATCHER_PLAN §3.4`).
  Contract C5.
- No `User.defaultZone` column — chef-to-zone scope is the
  `UserZone` many-to-many table introduced by
  `SEQUENCE_DISPATCHER_PLAN §3.3` (contract C6). Engine doesn't need
  it directly, but every "who can pause / unpin / force-dispatch"
  guard checks `UserZoneService.zonesForUser`.
- No `CuttingSpeedEmpirical` table in v2 — the time estimator continues
  to use the **single** `CuttingTimeCalculator` bean (dispatcher §2.6 /
  contract C1):
    1. `validatedCuttingtimeTimingModel` from CMS-DB `TimingModel`
       (joined by placement, NOT a column on
       `CuttingRequestSerie` / `CuttingRequestSerieData`),
    2. else `realCuttingtimeTimingModel` from the same row,
    3. else `CuttingRequestSerie.tempsDeCoupe`,
    4. then × `nbrCouche` for LASER-DXF, × 2 for Gerber,
    5. then divided by `CapaciteInstallee.efficienceTarget / 100` to
       reflect the effective production rate (per user §14 Q18).
  We can add empirical speeds later if the engine's idle time shows
  room — until then, the `efficienceTarget` knob is the lever Process
  uses to scale predictions.

---

## 4. The objective function — box duration first

### 4.1 Primary KPI: box duration
User-defined (the one that matters for sewing-supply smoothness):

$$
\text{boxDur}(seq) = \frac{\max_{s\in seq} \text{dateFinCoupe}_s \;-\; \min_{s\in seq} \text{dateDebutMatelassage}_s}{N_{boxes}(seq)}
$$

This differs from `Advanced_Ordonancement.md` L308–317 in one line:
**we divide by number of boxes, not number of series**. A sequence that
produces 16 boxes in the same span as one that produces 4 is 4× more
efficient for sewing supply and should score 4× better.

### 4.2 Composite score (minimize)
```
score = w_max       * max(boxDur)
      + w_med       * median(boxDur)
      + w_var       * variance(boxDur)
      + w_zone      * zone_overflow_penalty   -- SOFT only: SHARED-overflow temptation §2.5.1
      + w_due       * due_shift_miss_penalty  -- finish after dueShift
      + w_idle      * plant_idle_minutes      -- sum of gaps on active machines
      + w_endstack  * Σ_m endStackPenalty(m)  -- §4.6 / dispatcher §3.9
      + w_latecap   * late_machine_minutes    -- machines added > T0+2h, §2.5.1
```

Default weights `(w_max, w_med, w_var, w_zone, w_due, w_idle, w_endstack, w_latecap) =
(3, 2, 1, 5, 4, 0.5, 5, 1)` — `w_endstack` lowered from the earlier
draft of 6 to 5 (= w_zone) per user §14 Q12 (configurable later).
Stored in `OptimizerRun.algo_config_json` so Process can tune them
without code change.

Note: cross-zone HARD violations are no longer in the score — they are
filtered by `FeasibilityGuard` per §2.5 / dispatcher §8.4. The
`w_zone` term covers only the "engine could keep this serie inside its
strict zone but is tempted to push it to SHARED" case.

### 4.3 Suggested supplementary KPIs (visible on the engine dashboard)
The user asked for better indicators to consider — these are the ones I'd
propose. They are **displayed** on the dashboard but only the first three
are built into the objective by default:

| KPI | Why it matters | How to measure |
|---|---|---|
| **First-Box-Ready time** per sequence | Sewing wants *any* box early, not all boxes together. P50 over active sequences. | `min_s(dateFinCoupe) − sequence.createdAt` |
| **Sewing-supply smoothness** | Sewing hates spikes and gaps. Std dev of box-completion count per 15-min bucket. | Standard dev over rolling 2 h |
| **Work-in-progress (WIP)** | Too many open sequences = floor chaos | Count of sequences with ≥1 serie started and ≥1 serie not cut |
| **Due-shift hit rate** | How many sequences finished in or before `dueShift` | Fraction |
| **Plant idle ratio** | Honest measure of wasted capacity on active machines | `idle_min / active_min` |
| **Intra-zone type fairness** *(per STRICT zone)* | Catches the user's pain point — "Lectra over 100 % while Lectra IP6 sits at 35 %" inside the same zone. Lower is better. | `max{zoneLoad%(t,z)} − min{zoneLoad%(t,z)}` for each STRICT zone *z*, using the dispatcher's `(type, zone)` formula (§4 of dispatcher) |
| **Inter-zone STRICT spread** | Catches the global imbalance — one zone hot, another cold. | `max(zoneLoad%) − min(zoneLoad%)` across STRICT zones |
| **SHARED-zone offload share** | When Lectra IP6 is shared, how much work spills to it? Should match planned overflow, not be a leak. | `series_routed_to_SHARED / total_series` per shift |
| **Replan churn** | How many series changed machine between t and t+Δ. Auto-throttle above 5 % per minute (user §14 Q6). | Rolling count |

Replan churn is particularly important: when the engine bounces a serie
between machines every few seconds, operators lose confidence. We show it
so we can tune the perturbation moves.

---

## 4.5 Big-DB posture — "load one thing at a time"

The optimizer is the hottest reader of the DB in the whole MG-CMS
stack. The rules below mirror `SEQUENCE_DISPATCHER_PLAN §2.5` and
`PLAN_DE_CHARGE_IMPROVEMENT_PLAN §2`. Breaking any of them degrades
the whole shift.

### 4.5.1 Snapshot is built from projections only
The engine's in-memory "snapshot" (§2.3) is a set of Java records
created from repository projections. Never a `CuttingRequestSerie`
@Entity. Concretely:

| Slice | Source | Projection DTO |
|---|---|---|
| Sequences in scope | `findRelevantSequences(now−8h, now)` (repo L80–86) | `List<String>` |
| Series in those sequences | `findSeriesBySequencesLight(seqIds)` (repo L91–96) — note `serie.machine` is the machine-type NAME, not a machine ID | `List<CuttingRequestSerieDataLight>` |
| Resolved cutting time per serie | `CuttingTimeCalculator.resolveMinutesBatch(seriesLight)` — one CMS-DB call to `TimingModel`, results cached for the snapshot lifetime (contract C1) | `Map<String, Double>` keyed by placement |
| Active machines | `ActiveMachineResolver.activeMachines(date, shift)` — wraps `ShiftZoneConfirmationMachine` with the `EtatMachineHistorique` fallback (contract C2) | `List<ProductionTableLight>` (new DTO — id, nom, machineType, zone, tableLength) |
| Current queues | `MachineQueueRepository.snapshot()` (new, one `SELECT * FROM MachineQueue`) | `List<MachineQueueLight>` |
| Accepted sequences | `CuttingRequestRepository.findAcceptedLight(zone, horizon)` — server filter `zoneAcceptanceStatus = ACCEPTED OR (dispatchedZone IS NULL AND has_only_shared_types)` | `List<CuttingRequestLight>` (id, dispatchedZone, zoneAcceptanceStatus, pinnedByChef, dueDate, dueShift) |
| Capacity & efficiency | `CapaciteInstalleeService.getEffective(date, shift, groupe)` (contract C5) | `CapaciteInstalleeLight` per `(date, shift, groupe)` |

Six queries (one of which goes to the CMS DB). No JOIN FETCH on
`@OneToMany`. No LAZY traversal at runtime (would fetch the world
mid-evaluation).

### 4.5.2 Chunked refresh
When a partial event arrives (e.g. `SerieFinishedEvent`), the engine
refreshes only the affected slice — one machine's queue + one serie
row — not the full snapshot. A full snapshot refresh runs at most
every 60 s or on `ShiftZoneConfirmedEvent` /
`SequenceAcceptedEvent` / `ZoneMachineToggledEvent`.

### 4.5.3 Write batches, not one-row inserts
`MachineQueue` rewrites for a whole zone can churn 300–600 rows. Wrap
them in a single transaction + use
`saveAll(List<MachineQueue>)` with `hibernate.jdbc.batch_size=100`.
Equivalent savings for `OptimizerIndicatorSample` writes (they fire
every second when IMPROVING).

### 4.5.4 No round-trip for hot loops
The perturbation loop (swap, relocate, 2-opt, kick) runs **entirely
in memory** against the snapshot. The DB sees nothing until the
engine decides to commit an improved plan. This is the single biggest
performance line in the whole design — do not bend it.

---

## 4.6 Virtual-table assignment — each serie knows "its" table

This is the contract with `SEQUENCE_DISPATCHER_PLAN §3.7–§3.9`. The
engine uses — but does not re-implement — the `SerieTableAssigner`
bean introduced by the dispatcher plan.

Behaviour:

1. Every `MachineQueue` row written by the engine carries a
   **virtual-table flag**: `MachineQueue.assignedBy =
   'OPTIMIZER_VIRTUAL'` until the operator actually starts the serie
   (first `Form.js` POST sets
   `CuttingRequestSerieData.tableCoupe` + `tableIsVirtual = false`).
2. The engine is the sole writer of `MachineQueue` for Waiting
   series. Its output is a **complete per-machine plan** — so every
   active machine has at least position 1 (or is explicitly idle with
   a reason in `OptimizerRun.last_move`).
3. When the engine cannot place a serie (no machine of the right
   type in the chosen zone, all machines overloaded past the
   end-stack threshold, roll infeasibility), it **does not silently
   drop the serie**. It writes a row into `UnassignableSerie` (see
   `SEQUENCE_DISPATCHER_PLAN §3.7`) with the reason, and moves on.
   The dashboard surfaces the count; Process can reopen the
   confirmation or force a different zone.
4. The **end-stack penalty** from dispatcher §3.9 is also in the
   engine's objective function — same formula, reused bean:
   ```
   endStackPenalty(m) = 0                       if queueLoad(m)/cap(m) ≤ 0.9
                      = α·(load/cap − 0.9)²·cap if > 0.9
   ```
   This keeps the last hour of every machine breathable — directly
   addresses the user ask *"without having a stacking too much box
   in the end of the zone tables"*.

Composite score update (amends §4.2):

```
score += w_endstack * Σ_m endStackPenalty(m)
```

Default `w_endstack = 6` — higher than zone deviation, because a
jammed machine at end-of-shift blocks the next shift from starting.

### 4.6.1 What the engine writes on a queue row

```
MachineQueue
  machineNom          = m.nom                       (from SerieTableAssigner)
  queuePosition       = 1..N in planned order
  serie / sequenceId  = from the serie record
  estimatedCuttingTime= CuttingTimeCalculator.resolveMinutes(serie)
                                                     -- bean C1; reads CMS-DB
                                                        TimingModel by placement,
                                                        applies LASER × nbrCouche,
                                                        Gerber × 2, then divides by
                                                        CapaciteInstallee.efficienceTarget/100
                                                        (per user §14 Q18)
  estimatedStartTime  = running t cursor per machine
  estimatedEndTime    = estimatedStartTime + estimatedCuttingTime
  assignedBy          = 'OPTIMIZER_VIRTUAL'         (operator flip to 'OPERATOR')
  assignedAt          = now()
  version             = prev+1                      (see FORM_INTEGRATION_PLAN §3)
  rouleauHint         = RollFeasibilityService.bestCandidate(serie).id  (nullable)
```

The serie's `machine` column is the machine-type NAME (e.g. `"Lectra"`,
`"Lectra IP6"`, `"LASER-DXF"`); `m.nom` is the chosen `ProductionTable`
name. The engine resolves `serie → m` only by joining
`m.machineType.nom = serie.machine` (per dispatcher §3.6).

`version` and `rouleauHint` are new columns introduced by the Form
integration plan — the engine populates them, the kiosk reads them.

### 4.6.2 Handover to CMS-Prod   

When the engine commits a plan, operators learn via the **next-serie
banner** described in `FORM_INTEGRATION_PLAN §2–§3`. No direct
engine → kiosk coupling: the kiosk polls `/api/machineQueue/next` +
the version endpoint, which the engine happens to keep fresh.

---

## 5. Missing-date self-healing — reuse, don't rebuild

User answer #3: *"as you can see in CMS-Prod we have the automatique
filling of date based on the history on the CoupeMachineHistory entity"*.

This is already built in `CMS-Prod/ScheduledTask.reviewSerieWaiting()`
(lines 1112–1186) using `CoupeMachineHistoryRepository.findBySerieNoExtra()`
(lines 34–38). The `@Component` annotation on line 42 is commented out —
**the job is currently disabled**.

### Plan
1. **Re-enable** the `@Component` on `ScheduledTask` (or factor the
   `reviewSerieWaiting()` method out to its own `@Service` so we don't
   re-enable the other loops).
2. **Reduce the schedule** from 50 min to 10 min — the data is already
   written by `fillLectraReport()` every 15 s, so the reconciliation has
   no reason to wait 50 min.
3. **Expand the 8-h matching window** to a configurable `reconciliation.windowHours`
   property (default 12) — some machines take longer on weekends.
4. **Add a `DateInferenceAudit`** row every time a date is back-filled
   (serie, machine, inferredDebut, inferredFin, sourceFileReport, ind,
   writtenAt, writtenByJob). Two reasons: (a) traceability for Quality /
   Process disputes, (b) we can measure "how many series needed self-healing
   last shift?" — that's a KPI on its own (call it **forget-to-close rate**).
5. **Feed the engine**: whenever `reviewSerieWaiting()` writes a new
   `dateFinCoupe`, fire an application event so the engine refreshes its
   snapshot immediately.
6. **Surface the audit** in a "Données corrigées" panel inside the
   AdvancedOrdonnancement page (no separate page needed) — one table with
   serie, machine, inferred dates, source Lectra line, "ok / rollback"
   buttons. Rollback only for `ROLE_PROCESS` / `ROLE_ADMIN`.

### `DateInferenceAudit` schema
```
id                 BIGINT IDENTITY PK
serie              VARCHAR(64) NOT NULL
machine            VARCHAR(64) NOT NULL
inferred_debut     DATETIME2 NULL
inferred_fin       DATETIME2 NULL
source_file        NVARCHAR(256) NULL
source_ind         INT NULL
written_at         DATETIME2 NOT NULL
written_by_job     VARCHAR(64) NOT NULL
rolled_back_at     DATETIME2 NULL
rolled_back_by     BIGINT NULL
```

---

## 6. Spreading admission control — revised

The v1 stance was "SOFT zone binding everywhere". The user's later
clarification (dispatcher §11 Q9, this plan §14 Q2) tightened that:
zone binding is **HARD** inside the chef's accepted scope, because total
boxes per zone are bounded (≈ 16 boxes × N_machines). Admission control
follows the same line.

Implementation:

- `POST /api/scheduling/admit` returns:
    - `200 OK` with `{ok:true}` if `(serie, machine)` is the current
      `MachineQueue` top-N for that machine **AND** the machine is in
      the serie's resolved zone (strict or shared per dispatcher §3.8).
    - `200 OK` with `{warnings:[...]}` when the serie is on the right
      zone but not in the top-N (e.g. operator wants to start serie #3
      first because roll #2 isn't ready).
    - `409 Conflict` with `{reason: "ZONE_NOT_ACCEPTED"}` when the
      sequence is on a STRICT zone the chef has not yet accepted, OR
      the machine is in a different STRICT zone the dispatcher / chef
      did not assign. The form must surface this clearly: *"Cette série
      n'est pas attribuée à votre zone — demander à Process une
      ré-attribution"*.
    - `409 Conflict` with `{reason: "MACHINE_TYPE_MISMATCH"}` when
      `machine.machineType.nom ≠ serie.machine`.
- Form.js / FormCoupeNew.js / FormMix.js show:
    - a yellow **warning banner** with `override_reason` + Continue
      button when the response is `200 + warnings` (e.g. wrong queue
      position),
    - a red **block modal** when the response is `409` — Continue is
      not offered; only `[Demander à Process]` and `[Annuler]`.
- The override (warnings path only) is written to
  `AdmissionOverrideAudit(serie, machine, expectedMachine, reason,
  userId, ts)`.
- Hard-block events (409 path) are written to
  `AdmissionBlockedAudit(serie, machine, reason, userId, ts)` — Process
  reviews the trail in the dispatcher's audit panel.
- Engine picks up the override / block event, recomputes the penalty
  (override) or the `UnassignableSerie` row (block), and the `score`
  jumps on the indicator chart — people *see* the cost of deviation.

This still honours user answer #5 ("flexible on the full cutting") for
the *queue order* dimension — only zone-and-type boundaries are hard.

---

## 7. UI — what operators and Process see

### 7.1 `AdvancedOrdonnancement.js` additions
- **Engine control bar** at the top:
  ```
  [●  IMPROVING    |  Box-Dur P50: 46 min  ▼0.4 min/min  |  [Pause] [Stop]]
  ```
  State dot colours: grey IDLE, amber WARMING, green IMPROVING, yellow PAUSED, red STOPPED.
- **Live indicator chart** (Recharts line chart): x-axis = time, lines =
  `boxDurMax`, `boxDurMedian`, `sqrt(variance)`, score. Visible trend line so
  everyone can see improvement.
- **"Apply suggestion" button** per machine row — manual override already
  in the spec; now writes an `AdmissionOverrideAudit` entry with reason
  "manual dispatch by user X".
- **"Données corrigées" expander** — audit from §5.

### 7.2 Start / Stop / Pause API
```
POST /api/ordo/engine/start     body: { weights?, seed? }
POST /api/ordo/engine/pause
POST /api/ordo/engine/resume
POST /api/ordo/engine/stop
GET  /api/ordo/engine/state     → { state, currentScore, lastImprovementTs, runId }
GET  /api/ordo/engine/indicators?runId=... → time series for the chart
WS   /topic/ordo/engine         → state + latest sample + MachineQueue delta
```
All guarded by `ROLE_PROCESS` (or `ROLE_ADMIN`).

### 7.3 Live replan visibility
When the engine overwrites `MachineQueue`, the existing WebSocket push
(already planned in `md/Advanced_Ordonancement_Recommendations.md`) carries
the delta so all screens update without reload.

---

## 8. Integration with Plan de Charge

User answers #7 and #9: escalation and maintenance are the responsibility
of **Plan de Charge**, not a separate mechanism in the engine.

Concrete consequences:
- The engine **reads**: `EtatMachineHistorique` (PM / breakdown / status
  windows) and `ShiftLoadCalculation.available_time`.
- The engine **writes nothing** to Plan de Charge — it only reads.
- Infeasibility (e.g. a sequence cannot fit in any working shift before
  its `dueShift`) is surfaced on the engine dashboard as a red badge on
  that sequence. It does not create a `QualityNotice`, send mail, or page
  anyone — Process decides what to do.
- When Plan de Charge changes (a machine put in PM, operators count edited),
  an application event triggers engine snapshot refresh.

---

## 9. Phases (revised — slimmer)

### Phase 1 — Engine skeleton + box-duration objective — **HIGH**
> Depends on `SEQUENCE_DISPATCHER_PLAN` Phase 1 + 2 + 2.5 being merged
> first (the engine reads `dispatchedZone`, `zoneAcceptanceStatus`,
> `ShiftZoneConfirmationMachine`, the `CuttingTimeCalculator` bean and
> the `CapaciteInstallee` efficience target — all introduced by the
> dispatcher).

| Task | File(s) |
|---|---|
| `ContinuousOptimizerService` with start/stop/pause, WARMING using SCG baseline | `services/ContinuousOptimizerService.java` (new) |
| `ZoneOptimizerThread` per STRICT zone + `SharedZoneArbiterThread` per SHARED zone + single `CoordinationThread` (§2.0) | `services/scheduling/ZoneOptimizerThread.java`, `SharedZoneArbiterThread.java`, `CoordinationThread.java` |
| Extract `Schedule` model + `ObjectiveFunction` interface from `OrdonnancementService` | `services/scheduling/*.java` |
| Implement box-duration composite score with intra-zone fairness terms (§4.2) | `services/scheduling/BoxDurationObjective.java` |
| Wire the existing `CuttingTimeCalculator` bean (dispatcher §2.6) — engine **must not** re-implement the priority chain | use of `services/CuttingTimeCalculator.java` |
| Wire the existing `ActiveMachineResolver` bean (dispatcher §3.5 / contract C2) | use of `services/ActiveMachineResolver.java` |
| Wire the existing `CapaciteInstalleeService` (dispatcher §3.4 / contract C5) for efficiency | use of `services/CapaciteInstalleeService.java` |
| `OptimizerRun` + `OptimizerIndicatorSample` tables + JPA, retention job (7 days per §10.2 #8) | `domain/OptimizerRun.java`, `OptimizerIndicatorSample.java`, `services/OptimizerSampleRetentionJob.java` |
| REST + WebSocket endpoints §7.2 (start/stop/pause guarded `ROLE_PROCESS` only) | `controller/OrdoEngineController.java` |
| Engine control bar + indicator chart in AdvancedOrdonnancement | `components/Layout/AdvancedOrdonnancement.js` |

### Phase 2 — Self-healing reactivation & audit — **HIGH**
| Task | File(s) |
|---|---|
| Re-enable `reviewSerieWaiting()` as a dedicated `@Service` with 10-min schedule | split from `CMS-Prod/ScheduledTask.java` |
| Add `DateInferenceAudit` entity, write on every inference | `CMS-Prod/domain/DateInferenceAudit.java` |
| "Données corrigées" panel | `AdvancedOrdonnancement.js` |
| App event bus so engine refreshes on inference | use Spring `ApplicationEventPublisher` |

### Phase 3 — Admission control (HARD zone, SOFT order) — **MEDIUM**
> Reflects the §6 revision: zone & machine-type mismatches return 409;
> wrong queue position returns 200 + warnings.

| Task | File(s) |
|---|---|
| `POST /api/scheduling/admit` (200 OK / 200 + warnings / 409 ZONE_NOT_ACCEPTED / 409 MACHINE_TYPE_MISMATCH) | `controller/SchedulingController.java` |
| Patch `Form.js`, `FormCoupeNew.js`, `FormMix.js` to call admit; surface warning modal (yellow) on 200+warnings, block modal (red) on 409 — see `FORM_INTEGRATION_PLAN §5` for the toast component this reuses | `CMS-Prod/src/Form*.js` |
| `AdmissionOverrideAudit` entity (warnings path only) | `domain/AdmissionOverrideAudit.java` |
| `AdmissionBlockedAudit` entity (409 path) — surfaced in dispatcher audit panel | `domain/AdmissionBlockedAudit.java` |

### Phase 4 — Perturbation moves + feasibility guards — **HIGH**
| Task | File(s) |
|---|---|
| Swap / Relocate / Sequence-block-move / 2-opt / Kick moves | `services/scheduling/moves/*.java` |
| Feasibility guards: collision, machine state, rack rolls, working calendar | `services/scheduling/FeasibilityGuard.java` |
| `WorkCalendar` table + CRUD + default Sunday seed | `domain/WorkCalendar.java`, `controller/WorkCalendarController.java`, `src/main/js/components/Layout/WorkCalendar.js` |

### Phase 5 — Objective tuning & richer KPIs — **MEDIUM**
| Task | File(s) |
|---|---|
| Add first-box-ready, sewing-supply smoothness, WIP, due-shift hit rate to indicator sample | same |
| Dashboard tab for historical runs (compare runs) | `components/Layout/OrdoRunsHistory.js` |
| UI slider to tune weights (persists to `OptimizerRun.algo_config_json`) | `AdvancedOrdonnancement.js` |

### Phase 6 — Engine resilience — **LOW**
| Task | File(s) |
|---|---|
| Save engine checkpoint every N minutes so restart is warm | in-memory + JSON column |
| Backpressure: if SQL write latency > 200 ms, skip `MachineQueue` publish | service |
| Watchdog thread: if engine hasn't improved in 30 min, auto-kick | service |

---

## 10. Answers you gave — now baked into the plan

### 10.1 v1 answers (kept, with one revision)

1. **Horizon.** Default **closed** on Sunday shifts 1, 2, 3 via `WorkCalendar`;
   engine still schedules series whose `dueDate`+`dueShift` falls in a
   closed slot (user answer #1). → §3.1.
2. **Zone binding.** *(REVISED)* The v1 "SOFT everywhere" was
   superseded by the dispatcher contract HARD-inside-the-accepted-scope
   rule (`SEQUENCE_DISPATCHER_PLAN §8.4` + this plan §2.5 / §6). SOFT
   penalties are kept only for SHARED-overflow temptation and end-stack
   pressure.
3. **Missing dateFinCoupe.** Re-enable and extend existing
   `CoupeMachineHistory` reconciliation; do not build a new daemon
   (user answer #3). → §5. Gerber/Lectra logs are the only inference
   source — no quality-control delay assumption (per §14 Q4).
4. **Scheduling entities cleanup.** Drop new entities; keep only
   `Advanced_Ordonancement.md` + `PLAN_DE_CHARGE.md` as the model
   (user answer #4). → §11.
5. **Plan flexibility.** No FROZEN/HARD/SOFT window. Plan is fully
   re-optimisable at any moment **except** for sequences with
   `pinnedByChef = 1` or `zoneAcceptanceStatus = ACCEPTED` whose
   series have started (frozen sets defined in
   `SEQUENCE_DISPATCHER_PLAN §5.4`). → §2.2.
6. **Roll reservation.** No reservation; feasibility reads from
   `ScanRouleau` racks only (user answer #6). → §2.5.
7. **Escalation.** Owned by Plan de Charge, not by the engine
   (user answer #7). → §8.
8. **Operator continuity vs speed.** Priority = minimise box duration
   and finish boxes quickly to feed sewing; machine affinity only as a
   tie-breaker; no operator pinning (user answer #8). → §4.
9. **Empirical speeds & maintenance.** Maintenance data comes from
   Plan de Charge (user answer #9). Empirical-speed table deferred. → §3.3.

### 10.2 v2 answers (from §14)

1. **Engine lifecycle owner.** Only `ROLE_PROCESS` can start / pause /
   stop. No auto-start at shift boundaries. Once started, the engine
   keeps running across shift boundaries unless stopped manually.
   → §7.2.
2. **Default weights.** Confirmed `w_zone ≥ w_due` because zone
   capacity is bounded (≈ 16 boxes × N_machines). The HARD zone-binding
   rule in §2.5 makes this even stronger — cross-STRICT-zone moves are
   not negotiable. → §4.2.
3. **What counts as a box.** `N_boxes(seq) = count of CuttingRequestBox
   rows`. LASER-DXF and DIE work with all zones — they cut and
   distribute pieces back, no special box weighting needed. → §4.1.
4. **First-Box-Ready.** Quality control runs in parallel with cutting,
   so sewing supply order ≈ `dateFinCoupe` order. KPI stands. → §4.3.
5. **WorkCalendar seeding.** Sundays only; Moroccan holidays not
   required because if a shift is missed the next shift simply works
   the oldest serie first. → §3.1.
6. **Replan-churn auto-throttle.** Threshold ≤ 5 % series-changing-machine
   per minute. Above that, engine pauses kick / sequence-block-move
   for one cycle. → §4.3, §6.
7. **Engine between shifts.** Continue optimising non-stop into the
   incoming shift. → §2.1.
8. **Indicator retention.** 7 days for `OptimizerIndicatorSample`,
   then archive table. → §3.2.
9. **Replan on self-heal.** No instant refresh on `reviewSerieWaiting`.
   Either an explicit *"Heal this shift"* button OR end-of-shift
   audit + auto-fix from `CoupeMachineHistory`. → §5.
10. **Multi-zone parallelism.** Per-STRICT-zone optimiser threads in
    parallel, with shared-zone allocation arbitrated by the
    coordination thread. → §0, §2.0 (new).
11. **Snapshot cadence.** 60 s full refresh max. → §4.5.2.
12. **End-stack weight.** Start at `w_endstack = 5` (= w_zone), tune
    later. → §4.2.
13. **OPTIMIZER_VIRTUAL vs operator commit.** Keep the system
    recommendation as advisory; operator overrides allowed (§6 admit
    flow). → §4.6.
14. **Hibernate batch size.** Open — keep the 100 placeholder; will
    confirm with the SQL Server JDBC driver in prod. → §4.5.3.
15. **`MachineQueue.version` scope.** Open — per-machine in current
    draft, awaiting confirmation.
16. **Snapshot debounce on `SequenceAcceptedEvent`.** 1 refresh / 5 s.
    Open — accepted as draft.
17. **`UnassignableSerie` ownership.** Dispatcher creates; engine
    only updates `resolved_at` on next successful placement. Confirmed.
    → §4.6 / dispatcher §3.7.
18. **PdC-actuals feedback into time estimates.** Engine applies
    `× 1 / efficienceTarget` from `CapaciteInstallee` (90 % default,
    Gerber-only override = 50 % i.e. ×2 multiplier already baked into
    `CuttingTimeCalculator`). No automatic per-machine drift; Process
    tunes `efficienceTarget` if a zone trends slow. → §3.3 / §4.6.1.

---

## 11. Documentation cleanup — files to delete

Per user answer #4, only `md/Advanced_Ordonancement.md` and
`md/PLAN_DE_CHARGE.md` remain authoritative for scheduling. The following
md files describe scheduling entities / plans that were never built or
are superseded — they must be **deleted** to avoid future confusion:

| File | Reason |
|---|---|
| `md/Advanced_Ordonancement_Recommendations.md` | Recommendations now folded into this plan |
| `md/OPTIMAL_SCHEDULING_MASTERPLAN.md` | Parallel scheduling masterplan — never implemented |
| `md/OrdonancementPlan.md` | Earlier V3 multi-zone draft — superseded |
| `md/SCHEDULING_OPTIMIZATION_GUIDE.md` | Greedy-with-async guide — duplicated by Advanced_Ordonancement.md |

Files to **keep** untouched:
- `md/Advanced_Ordonancement.md` (authoritative)
- `md/PLAN_DE_CHARGE.md` (authoritative)
- `plans/PREPARATION_IMPROVEMENT_PLAN.md` (different scope)
- `plans/LOCAL_LLM_INTEGRATION_PLAN.md` (different scope)
- `plans/CAD_PIECE_WEIGHT_CALCULATION.md` (different scope)
- this file and `plans/GENERAL_IMPROVEMENT_PLAN.md`.

After deletion, **update any cross-references inside
`md/Advanced_Ordonancement.md`** that point to the deleted docs.

---

## 12. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Engine perturbations cause `MachineQueue` churn that confuses operators | Throttle publish to max 1 update / 2 s per machine; show "Replan churn" KPI; auto-throttle above 5 % churn / min (per §10.2 #6); let Process pause the engine with one click |
| 400 series × 30 machines × continuous loop → CPU hog | Bound CPU to 10 % per `ZoneOptimizerThread`, ≤ 50 % total; move perturbation to per-zone daemon threads with a semaphore; measure & log per-iteration time |
| Self-healing backfills a wrong date (two jobs on same machine, same placement) | `DateInferenceAudit` + human "rollback" button + KPI "forget-to-close rate" trend; if it rises, flag the operator |
| Admission 409 surfaces too often → operators frustrated | Track `AdmissionBlockedAudit` rows by reason; if `ZONE_NOT_ACCEPTED` is the top reason, the dispatcher's chef-acceptance gate (§7.2 of dispatcher) is too slow — Process gets a banner and can switch a zone to auto-accept |
| `CuttingTimeCalculator` drift between engine and dispatcher | Single bean (contract C1) + unit test: same fixture → same total in both callers |
| Per-zone optimiser thread crashes → that zone stops improving | Watchdog supervises each `ZoneOptimizerThread`; on crash, restart from last good `MachineQueue` snapshot; alert Process via dashboard banner |
| Engine ignores newly-pinned sequence (chef-pull from dispatcher §5.4.1) | `SequenceAcceptedEvent` fires on pin too; the `ZoneOptimizerThread` picks it up on next snapshot and treats `pinnedByChef = 1` as immune to relocate moves |
| `WorkCalendar` not maintained → engine silently refuses Sunday series | On every new sequence whose `dueDate`+`dueShift` is closed, surface a banner; admin override one-click |

---

## 13. What to ship first

**Phase 1 + Phase 2 together as v1.** Phase 1 gives the engine and the
box-duration KPI; Phase 2 stops NULL `dateFinCoupe` from polluting it.
Without both, the KPI lies.

Phase 3 (admission control) only makes sense once the engine has run for
a week and we have a baseline for override rate.

Phase 4 is where the real optimisation value lives — but it needs
Phase 1's evaluation framework to be in place.

---

## 14. Open questions — resolved table

The 18 v2 open questions have been answered (inline in §10.2). They
are kept here as a changelog so anyone re-reading the plan sees what
the original draft proposed vs what was decided.

| # | Question | Decision |
|---|---|---|
| 1 | Engine lifecycle owner / auto-start | `ROLE_PROCESS` only. No auto-start. Continues across shift boundaries unless manually stopped. |
| 2 | Objective weight ordering | `w_zone ≥ w_due` confirmed (zone capacity is bounded ≈ 16 × N_machines). Cross-STRICT-zone moves now HARD per dispatcher §8.4 — they are not in the score. |
| 3 | What counts as a box | `count(CuttingRequestBox)` for the sequence. LASER-DXF / DIE distribute pieces back to all zones — no special weighting. |
| 4 | First-Box-Ready assumption | Quality control runs in parallel with cutting, so sewing supply order ≈ `dateFinCoupe` order. KPI valid as-is. |
| 5 | WorkCalendar seeding (holidays) | Sundays only. If a shift is missed, next shift simply works the oldest serie first — no holiday list needed yet. |
| 6 | Replan-churn auto-throttle | ≤ 5 % series-changing-machine per minute. |
| 7 | Engine between shifts | Continue optimising into the incoming shift, non-stop. |
| 8 | `OptimizerIndicatorSample` retention | 7 days, then archive. |
| 9 | Replan on self-heal | No instant refresh. Either explicit *Heal this shift* button OR end-of-shift audit + auto-fix from `CoupeMachineHistory`. |
| 10 | Multi-zone parallel engines | **Yes — per-STRICT-zone optimisers in parallel** + a coordination thread. Folded into §0 + §2.0. Reason: shared-zone machines have to be planned alongside the strict zones, so one merger thread arbitrates the SHARED queue. |
| 11 | Snapshot cadence | 60 s full refresh max. |
| 12 | End-stack weight | Start at `w_endstack = 5` (= w_zone), tune later. |
| 13 | OPTIMIZER_VIRTUAL vs operator commit | Keep as advisory; operator override allowed. |
| 14 | `hibernate.jdbc.batch_size` | Keep 100 placeholder; confirm in prod with SQL Server JDBC driver. *(still open — confirmation only)* |
| 15 | `MachineQueue.version` scope | Per-machine counter (current draft). *(still open — confirmation only)* |
| 16 | Snapshot debounce on `SequenceAcceptedEvent` | 1 refresh / 5 s. *(still open — confirmation only)* |
| 17 | `UnassignableSerie` ownership | Dispatcher creates; engine only updates `resolved_at` on next successful placement. |
| 18 | PdC-actuals feedback | Engine applies `× 1 / efficienceTarget` from `CapaciteInstallee` (90 % default; Gerber-specific: ×2 multiplier already in `CuttingTimeCalculator`). No automatic per-machine drift — Process tunes the efficience target if a zone trends slow. |

### 14.1 Still-open items (confirmation only)

These are answered in spirit but the precise number / behaviour needs
operator confirmation in production:

- Q14 — final batch size for the SQL Server JDBC batch insert.
- Q15 — `MachineQueue.version` per-machine vs global. Per-machine in
  current draft.
- Q16 — `SequenceAcceptedEvent` debounce window. 5 s in current draft.

---

## 15. What I recommend

1. Merge this plan, delete the obsolete md files in §11, and update the
   cross-refs in `Advanced_Ordonancement.md`.
2. Ship **Phase 1 + Phase 2**. Measure the current box-duration baseline
   for 1 week with engine in pure WARMING mode (no perturbation) — this
   is your honest floor.
3. Turn on IMPROVING. Watch the curve. Let Process tune the weights
   through the UI; don't hard-code a "winner".
4. Once the curve plateaus, ship Phase 3 (admission control) so operator
   deviations become visible in the KPI rather than silent.
5. Phase 4 moves expand the search space; Phase 5 adds richer KPIs;
   Phase 6 is only needed if the engine stability becomes a concern.
