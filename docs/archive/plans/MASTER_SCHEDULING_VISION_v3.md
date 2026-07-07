# MG-CMS — Master Scheduling Vision (v3)

> **Project:** MG-CMS (Spring Boot + React + SQL Server) — Lear Corp.
> Trim 1, Tangier TFZ.
> **Why this document exists.** The three earlier plans
> (`FULL_GUIDE_IMPLEMENTATION.md`, `SEQUENCE_DISPATCHER_PLAN.md`,
> `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md`) shipped a narrow
> dispatcher gate. The richer designs in the source plans — load
> heatmap, best-fit table assignment, Continuous Optimization Engine,
> box-duration KPI, perturbation moves, intra-zone fairness — were
> deferred. This file sequences them into one shippable program.
> **Rule of precedence.** Where this file disagrees with an earlier
> plan, this file wins. Where this file is silent, the earlier source
> plan applies (`SEQUENCE_DISPATCHER_PLAN §X` or
> `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §X`).

## 0.1 Implementation status

| Phase | Status | Notes |
|---|---|---|
| Phase A — Heatmap & ZoneLoad + DispatchAudit + extended endpoints | ⏳ IN PROGRESS | This commit |
| Phase B — SerieTableAssigner | ⬜ TODO | |
| Phase C — Zone admin & ownership | ⬜ TODO | |
| Phase D — Confirmation gate lifecycle (T-60 / T0) | ⬜ TODO | |
| Phase E — Continuous Optimization Engine, skeleton | ⬜ TODO | |
| Phase F — Engine perturbation moves | ⬜ TODO | |
| Phase G — Multi-zone parallelism | ⬜ TODO | |
| Phase H — Self-healing audit (DateInferenceAudit) | ⬜ TODO | |
| Phase I — Admission warnings (200+warnings + override audit) | ⬜ TODO | |
| Phase J — KPI dashboard + War Room + baseline comparison | ⬜ TODO | depends on §18 baseline mechanism |
| Phase K — Cleanup of v1 dead code | ⬜ TODO | |

Marker convention (matches `FULL_GUIDE_IMPLEMENTATION.md`): ⬜ TODO,
⏳ IN PROGRESS, ✅ DONE. Update this table when a phase merges.

---

## 0. The vision in one paragraph

A plant supervisor opens MG-CMS and sees, in one click, three things
that today live in three different mental models:

1. **What's happening now** — every machine, every series, every box,
   live, with the chef's confirmation overlay and the operator's
   actuals overlay drawn on the same canvas.
2. **What we *plan* to happen** — a continuously-optimised plan whose
   convergence curve moves down on screen (max / median / variance of
   box duration), with a per-zone heatmap that points at the exact
   `(machineType, zone)` pair that's hot.
3. **Why** — a single explanation panel that walks any cell back to
   its cause: which serie, which sequence, which roll, which chef
   confirmed it, which event invalidated the previous plan, which
   feasibility rule blocked it.

The system never blocks the floor. Every gate has a fallback. Every
inference has an audit row. Every change has a WebSocket event.

---

## 1. Information architecture — the menu after this plan

Today's `Process` submenu is flat (Plan de Charge / Dispatcher /
Ordonnancement Avancé / Contrôle Moteur / 2× config / Capacité). After
this plan, the menu is reorganised by **role** and **layer**:

```
Process (visible to ROLE_PROCESS, ROLE_CHEF_EQUIPE, ROLE_ADMIN)
├── Plan de Charge                  (existing — extended in §3)
├── Sequence Dispatching            (existing — extended in §4)
├── Optimizer Console               NEW — engine start/pause + live chart §5
├── Optimizer History               NEW — compare past runs §5.6
├── Work Calendar                   NEW — Sunday/holidays editor §5.4
├── Inassignables                   NEW — drawer + dedicated page §4.5
├── Date Inference Audit            NEW — back-filled fin-coupe rows §6
├── Admission Audit                 NEW — blocked + warned starts §7
├── Zone Admin                      NEW — STRICT/SHARED + invariants §4.7
├── Zone Ownership (chefs↔zones)    NEW — matrix editor §4.8
├── Capacité Installée              (existing)
├── Part Number Material Config     (existing)
└── Part Number Material Config Data (existing)

Production
├── Chef de Zone — Confirmation     (existing — minor extensions §4.9)
├── Chef de Zone — Supervision      EXTENDED — full live dashboard §4.10
├── Operator Kiosk Banner           (existing — extensions §8)
└── Floor War Room                  NEW — read-only TV dashboard §9

Ordonnancement (existing top-level)
├── Ordonnancement Avancé           EXTENDED — engine bar + indicator chart §5.5
└── KPI Dashboard                   NEW — boxDur P50/P95/var, churn §10
```

11 new menu items. Every new page lives under an existing top-level
section so no new icon/role is needed in `Dashboard.js`.

---

## 2. Single-source-of-truth contracts (extended)

The C1–C6 contracts from `SEQUENCE_DISPATCHER_PLAN §8.2` stand. Three
more are added by this plan:

| # | Contract | Shared code | Enforced by |
|---|---|---|---|
| **C7** | Zone load % keyed by `(machineType, zone)` — single formula in §4 of dispatcher plan | `ZoneLoadService.computeMatrix(date, shift)` (NEW §4.1) | unit test: dispatcher heatmap, optimizer fairness term, PdC zone overlay all read this bean |
| **C8** | Best-fit virtual table for a serie | `SerieTableAssigner.bestTable(serie, date, shift)` (NEW §4.4) | unit test: dispatcher, kiosk banner and engine all call this bean — never re-implement |
| **C9** | Box-duration KPI value `boxDur(seq) = (max dateFinCoupe − min dateDebutMatelassage) / N_boxes(seq)` | `BoxDurationCalculator.compute(snapshot)` (NEW §5.3) | unit test: optimizer score, KPI dashboard, history compare page all share this bean |

Every numeric value the operator sees on screen must trace back to one
of C1–C9 or it is a bug.

---

## 3. Layer A — Plan de Charge polish

`PlanDeCharge.js` already shows shift load, status grid, recalculer.
Today it answers *"how full is each shift?"*. After this plan it
answers *"how full, how confident, and how is reality tracking?"*.

### 3.1 Actuals overlay (already promised in FULL_GUIDE Phase 6 §6)

- Add a column "Réalisé" alongside "Planifié" in the load grid.
  Pulls from `CuttingRequestSerieData` where
  `dateFinCoupe IS NOT NULL` for the shift in question.
- Each cell shows `planifié / réalisé (delta %)`. Coloured green when
  realised ≥ 95 % of planned, amber 80–95 %, red < 80 %.
- Backed by `PdcActualsOverlayService` — already in code, but only the
  endpoint is wired. The UI consumption is missing.

### 3.2 Predictive load lookahead (NEW)

A second tab on the page: "Charge prévue 7 jours". Shows the same
grid for D+1..D+7 using sequences with `dueDate` in window — built
from the dispatcher's preview, not from manual entry. Cells turn red
if predicted load > 100 % so Process sees the bottleneck **before** it
hits.

### 3.3 Scenario simulator (NEW)

A "What if?" panel:
- Slider: shift duration 380 → 540 min.
- Slider: efficience target 60 → 110 %.
- Toggle: include Saturday extras / open Sunday shift 1.
- Live: every load cell rerenders without writing to the DB.

Backed by a stateless `POST /api/planDeCharge/simulate` endpoint that
takes the override knobs and returns the same payload shape as
`/calculate`. No persistence — pure projection.

### 3.4 Visual upgrades

- Replace the current numeric load grid with a **heatmap** rendered as
  SVG: rows = zones, columns = `(date, shift, machineType)`. Same
  data as today, but coloured 0–120 %. Click → drill-down modal.
- **Sparklines** in each cell showing the last 7 days' load %.
- **Sticky compare** — a checkbox "Comparer avec semaine dernière"
  overlays a faint dashed line for the equivalent shift one week ago.

### 3.5 New pages — none

PdC stays one page. The two new tabs (Actuals + 7-day) and the
"What if?" panel live inside `PlanDeCharge.js`.

---

## 4. Layer B — Sequence Dispatching, full version

`ProcessDispatcher.js` today is a 303-line preview/publish form. The
plan needs it to be the page Process *lives on* during the shift.

### 4.1 `ZoneLoadService` — the missing service (Contract C7)

```java
@Service
public class ZoneLoadService {
    /** Returns one row per (machineType, zone) for the given window. */
    public List<ZoneLoadCellDto> computeMatrix(LocalDate date, int shift);

    /** Same but per zone — sum across types. Used by PdC overlay. */
    public List<ZoneLoadRowDto> computeRows(LocalDate date, int shift);
}

record ZoneLoadCellDto(
    String zoneNom,
    String machineType,
    double plannedMinutes,
    double capacityMinutes,
    double loadPct,
    int activeMachines,
    int sequencesCount,
    LocalDateTime lastSerieEta
) {}
```

Reads via `CuttingTimeCalculator.resolveMinutesBatch` (C1),
`ActiveMachineResolver.activeMachines` (C2),
`CapaciteInstalleeService.getEffective` (C5). Five queries
total — same posture as `SEQUENCE_DISPATCHER_PLAN §2.5`.

### 4.2 Heatmap panel (Process Dispatcher, top third)

```
┌──────────────────────────────────────────────────────────────────┐
│ [Date] [Shift ▼] [Réquilibrer]              Inassignables: 4 ◉  │
├──────────────────────────────────────────────────────────────────┤
│ ZONE \ TYPE       Lectra   Lectra IP6   Gerber   LASER-DXF       │
│                                                                  │
│ First Article     ▓▓▓ 78%  ▓▓▓▓ 102%   ░░ 23%      —             │
│ Serie             ▓▓░ 64%  ▓▓░ 58%      —          —             │
│ Prototype         ░░ 31%   ░░ 28%       —          —             │
│ ── SHARED ──                                                     │
│ Laser Pool         —        —           —      ▓▓▓ 81%           │
└──────────────────────────────────────────────────────────────────┘
```

- Each cell tooltips: planned/capacity minutes, active machines, ETA
  of last serie, count of sequences contributing.
- Click → drilldown side panel listing the sequences in that cell with
  a `[Forcer ailleurs]` action.
- Colour: green < 80 %, amber 80–100 %, red > 100 %, grey "no
  machines of this type in this zone".

### 4.3 Sequence list panel — full version (`SEQUENCE_DISPATCHER_PLAN §6.2 Panel 2`)

Today's table just shows preview rows. Add columns: machine types,
load % impact, chef pin indicator, actions row with `[Accepter]`,
`[Forcer…]`, `[Pin]`, `[Voir séries]`. Drag-to-reorder between zones.
Bulk actions across selected rows.

### 4.4 `SerieTableAssigner` — best-fit table (Contract C8)

```java
@Service
public class SerieTableAssigner {
    public TableAssignment bestTable(SerieDispatchInfo serie,
                                      LocalDate date, int shift);

    /** Score model from SEQUENCE_DISPATCHER_PLAN §3.9. */
    private double score(ProductionTable m, SerieDispatchInfo s) {
        double q = α * queueLoadMin(m);
        double l = β * lengthMismatch(m, s);
        double e = γ * endStackPenalty(m, s);
        double t = δ * typeSpecialisationBonus(m, s);
        return q + l + e − t;
    }
}
```

Weights `α, β, γ, δ` exposed in `application.properties`:
```
mgcms.assigner.weight.queue=1.0
mgcms.assigner.weight.length=10.0
mgcms.assigner.weight.endstack=5.0
mgcms.assigner.weight.specialisation=2.0
mgcms.assigner.endstack-threshold=0.9
```

Outputs:
- Writes virtual `tableCoupe` + new column `tableIsVirtual = true` on
  the projection-backed copy of the serie.
- If no fit: writes `UnassignableSerie` with a reason — adds the
  three missing reasons (`ROLL_MISSING`, `NO_TIMING`, `ZONE_OVERLOAD`)
  to the existing enum.

Schema delta — additive on `CuttingRequestSerieData` *projection only*
(no new column on `@Entity` `CuttingRequestSerie`). The flag travels
in the DTO; persistence remains unchanged. *(This is a deliberate
deviation from the source plan, which proposed a real column. Keep
the flag in the projection unless we end up needing it queryable.)*

### 4.5 Inassignables drawer (NEW page + drawer)

Two surfaces for the same data:
- **Drawer** on the dispatcher page (the "Inassignables: 4 ◉"
  badge). Slides over from the right. Lists series grouped by reason
  (`reason → count → expand`). For each row: serie, sequence, machine
  type, suggested fix (open zone X, add LECTRA-04, force to zone Y).
- **Dedicated page** `/process/inassignables` for the long view —
  filter by date range, reason, zone, sortable, CSV export.

Backend: `GET /api/dispatcher/unassignable?since=&reason=&zone=`.

### 4.6 `DispatchAudit` table (NEW)

```sql
CREATE TABLE DispatchAudit (
    id            BIGINT IDENTITY PRIMARY KEY,
    sequence      VARCHAR(64)   NOT NULL,
    from_zone     VARCHAR(64)   NULL,
    to_zone       VARCHAR(64)   NOT NULL,
    reason        NVARCHAR(512) NULL,
    trigger       VARCHAR(32)   NOT NULL,  -- AUTO | MANUAL | REBALANCE
                                            -- | CHEF_PIN | UNPIN
                                            -- | MID_SHIFT_REJECT | ENGINE_TICK
    user_id       BIGINT        NOT NULL,
    matricule     VARCHAR(32)   NULL,
    created_at    DATETIME2     NOT NULL,
    INDEX IX_DispatchAudit_seq (sequence, created_at DESC),
    INDEX IX_DispatchAudit_at  (created_at)
);
```
Migration: `V12_01__dispatch_audit.sql`.

Surfaced as Panel 3 of the dispatcher page (collapsible, latest 50)
**and** as a column-level sparkline in the heatmap (cell tooltip
shows the last 5 dispatch events for that zone).

### 4.7 Zone Admin page — `/admin/zones` (NEW)

Extends the existing Zone CRUD with three columns:
- **Catégorie** (STRICT / SHARED) — inline dropdown. Save-time
  validation that no machine type appears in both a STRICT and a
  SHARED zone (the §3.1 invariant in the source plan). On violation,
  block the save with a clear French error.
- **Types présents** — read-only count badges per `MachineType.nom`.
- **Chefs** — read-only chips listing the `UserZone.user` rows with
  `revoked_at IS NULL`. Click → switches to the Zone Ownership page.
- **Capacité & efficience** — read-only link to the existing
  `CapaciteInstallee` editor for the zone's groupe.

### 4.8 Zone Ownership page — `/process/zoneOwnership` (NEW)

The matrix editor missing from FULL_GUIDE Phase 5. Two-pane layout:

```
┌─────────────────┬─────────────────────────────────────────────────┐
│ Zones           │ Users with ROLE_CHEF_DE_ZONE                    │
├─────────────────┼─────────────────────────────────────────────────┤
│ ☑ First Article │ [✓] A. Amine     ○ par défaut                   │
│ ☐ Serie         │ [✓] K. Yassine   ●                              │
│ ☐ Prototype     │ [ ] M. Hassan                                   │
│ ── SHARED ──    │ [ ] B. Karim                                    │
│ ☐ Laser Pool    │                                                 │
└─────────────────┴─────────────────────────────────────────────────┘
```

- Pick a zone on the left → check users on the right.
- Radio button per user marks that zone as their default zone (one
  default per user — partial unique index on
  `UserZone(user_id) WHERE is_default = 1 AND revoked_at IS NULL`).
- Soft delete (`revoked_at`), audit row in `UserZone` columns
  (already present).
- Endpoints already exist (`/api/userZone/assign`, `/revoke`). Add
  `PUT /api/userZone/{userId}/default/{zoneNom}`.

Replaces the minimal `UserZoneAdmin.js` — keeps the route, rewrites
the layout.

### 4.9 Confirmation gate extensions

The two-step modal (composition + acceptance) ships. Add:
- **Auto-accept job at T0** for `PENDING` sequences — writes
  `zoneAcceptedBy = 'SYSTEM_AUTO'`, surfaces an amber row in the
  audit. Schedule: cron `0 */1 * * * *` (every minute) checks if the
  current shift's T0 is reached.
- **Pre-dispatch job at T-60** — same lifecycle, reverse direction:
  for the *upcoming* shift, run dispatcher in preview mode and write
  `dispatchedZone` + `zoneAcceptanceStatus=PENDING` so Step 2 is ready
  when the chef logs in.
- **`added_after_start` flag** — ShiftZoneConfirmationMachine table
  has the column already; toggleMachine endpoint should set it to 1
  when called after the shift's T0.

### 4.10 Chef de Zone Supervision page — full version

`ChefDeZonePage.js` today is 143 lines (accept / reject + 15 s
polling). Replace with the design from
`SEQUENCE_DISPATCHER_PLAN §7.4`:

```
┌──────────────────────────────────────────────────────────────────┐
│ Zone: First Article  S1 (06:00–14:00)  Composition confirmée 05:48 │
├──────────────────────────────────────────────────────────────────┤
│ MACHINE STRIP                                                    │
│ [LECTRA-01 78%]   [LECTRA-03 64%]   [LECTRA-IP6-01 92%]          │
│  ●Marche en cours  ●Marche en cours  ◐ Pause                     │
│  Serie #4521 23m   Serie #4538 17m    Serie #4541 12m            │
│  Next: 4522, 4523  Next: 4539         Next: 4542, 4543           │
├──────────────────────────────────────────────────────────────────┤
│ ACCEPTED SEQUENCES (4)                                           │
│ ▷ 2026-04-20-12  18 série  due S1  ETA 11:42  ●Pin               │
│ ▷ 2026-04-20-13  6 série   due S1  ETA 09:18                     │
├──────────────────────────────────────────────────────────────────┤
│ PENDING / FROM OTHER ZONES (2) — [Pour ma zone]                  │
│ ▷ 2026-04-20-15  Lectra IP6 only — Zone Serie can't take it      │
├──────────────────────────────────────────────────────────────────┤
│ ALERTS                                                           │
│ ⚠ Serie #4521 en retard sur médiane Lectra (47 min vs P50 32)    │
│ ⚠ Sequence 12 risque retard de 18 min sur dueShift               │
└──────────────────────────────────────────────────────────────────┘
```

- Live updates via WebSocket `/topic/zone/{zoneName}` (need to
  publish from the dispatcher / engine — the topic name pattern is
  defined now but not yet emitted).
- Pin / unpin toggle on accepted rows (writes `pinnedByChef`).
- "Pour ma zone" pulls a SHARED-overflow sequence into the chef's
  STRICT zone — endpoint `POST /api/dispatcher/sequence/{seq}/pull`
  body `{zoneNom, reason}`. Validates that the chef owns `zoneNom`.

### 4.11 Force / Pin / Rebalance endpoints

Currently the dispatcher only has `preview`, `publish`,
`setAcceptance`. Add:

```
POST /api/dispatcher/rebalance?date&shift   — full re-greedy
POST /api/dispatcher/sequence/{seq}/force   — body {zoneNom, reason}
POST /api/dispatcher/sequence/{seq}/pin     — body {reason}
POST /api/dispatcher/sequence/{seq}/unpin   — admin/process only
POST /api/dispatcher/sequence/{seq}/pull    — chef pulls a sequence
POST /api/dispatcher/recompute              — re-runs SerieTableAssigner only
```

Every one of these writes a `DispatchAudit` row.

---

## 5. Layer C — Continuous Optimization Engine

**The biggest gap.** The user-stated centerpiece. Below is what
ships, file-by-file. Mirrors
`ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §2`.

### 5.1 New domain entities

```sql
-- V13_01__work_calendar.sql
CREATE TABLE WorkCalendar (
    id                 BIGINT IDENTITY PRIMARY KEY,
    shift_date         DATE        NOT NULL,
    shift_number       INT         NOT NULL,
    working            BIT         NOT NULL,
    override_reason    NVARCHAR(256) NULL,
    updated_by         BIGINT      NULL,
    updated_at         DATETIME2   NULL,
    UNIQUE (shift_date, shift_number)
);
-- Default Sunday shifts 1/2/3 = 0, every other = 1, seeded for the
-- next 90 days by an init script.

-- V13_02__optimizer_run.sql
CREATE TABLE OptimizerRun (
    id                 BIGINT IDENTITY PRIMARY KEY,
    started_at         DATETIME2   NOT NULL,
    started_by         BIGINT      NOT NULL,
    stopped_at         DATETIME2   NULL,
    stopped_by         BIGINT      NULL,
    status             VARCHAR(16) NOT NULL,  -- WARMING|IMPROVING|PAUSED|STOPPED
    algo_config_json   NVARCHAR(MAX) NOT NULL,
    final_score        DECIMAL(18,4) NULL,
    iterations         INT          NULL
);

-- V13_03__optimizer_indicator_sample.sql
CREATE TABLE OptimizerIndicatorSample (
    id                  BIGINT IDENTITY PRIMARY KEY,
    run_id              BIGINT       NOT NULL,
    ts                  DATETIME2    NOT NULL,
    iterations          INT          NOT NULL,
    box_dur_max         DECIMAL(10,2) NOT NULL,
    box_dur_median      DECIMAL(10,2) NOT NULL,
    box_dur_variance    DECIMAL(12,4) NOT NULL,
    wip_sequences       INT          NOT NULL,
    plant_idle_ratio    DECIMAL(5,4) NOT NULL,
    first_box_ready_p50 DECIMAL(10,2) NOT NULL,
    intra_zone_spread   DECIMAL(7,4) NOT NULL,
    inter_zone_spread   DECIMAL(7,4) NOT NULL,
    replan_churn_pct    DECIMAL(7,4) NOT NULL,
    score               DECIMAL(18,4) NOT NULL,
    INDEX IX_OIS_run_ts (run_id, ts)
);

-- V13_04__date_inference_audit.sql
CREATE TABLE DateInferenceAudit (
    id              BIGINT IDENTITY PRIMARY KEY,
    serie           VARCHAR(64)  NOT NULL,
    machine         VARCHAR(64)  NOT NULL,
    inferred_debut  DATETIME2    NULL,
    inferred_fin    DATETIME2    NULL,
    source_file     NVARCHAR(256) NULL,
    source_ind      INT          NULL,
    written_at      DATETIME2    NOT NULL,
    written_by_job  VARCHAR(64)  NOT NULL,
    rolled_back_at  DATETIME2    NULL,
    rolled_back_by  BIGINT       NULL,
    INDEX IX_DIA_serie (serie, written_at DESC)
);

-- V13_05__admission_override_audit.sql
CREATE TABLE AdmissionOverrideAudit (
    id           BIGINT IDENTITY PRIMARY KEY,
    serie        VARCHAR(64)   NOT NULL,
    machine      VARCHAR(64)   NOT NULL,
    expected_machine VARCHAR(64) NULL,
    reason       NVARCHAR(512) NULL,
    matricule    VARCHAR(32)   NOT NULL,
    created_at   DATETIME2     NOT NULL,
    INDEX IX_AOA_serie (serie, created_at DESC)
);
```

### 5.2 Service layout

```
services/scheduling/optimizer/
├── ContinuousOptimizerService.java     (lifecycle, IDLE → … → STOPPED)
├── ZoneOptimizerThread.java            (one per STRICT zone)
├── SharedZoneArbiterThread.java        (one per SHARED zone)
├── CoordinationThread.java             (merges, writes MachineQueue)
├── BoxDurationCalculator.java          (Contract C9)
├── BoxDurationObjective.java           (composite score §5.4)
├── FeasibilityGuard.java               (§2.5 of source plan)
├── moves/
│   ├── SwapMove.java
│   ├── RelocateMove.java
│   ├── SequenceBlockMove.java
│   ├── TwoOptMove.java
│   └── KickMove.java
└── snapshot/
    ├── ScheduleSnapshot.java           (in-memory model)
    ├── SnapshotBuilder.java            (5-query loader §4.5.1 of source)
    └── SnapshotRefresher.java          (chunked refresh on events)
```

### 5.3 BoxDuration calculator (Contract C9)

```java
public final class BoxDurationCalculator {
    public record Result(
        double max, double median, double variance,
        Map<String, Double> bySequence) {}

    public static Result compute(ScheduleSnapshot s) {
        Map<String, Double> bySeq = s.sequences().stream()
            .collect(toMap(
                Sequence::id,
                seq -> {
                    int n = seq.boxesCount();           // C9 numerator: N_boxes, NOT N_series
                    if (n == 0) return 0.0;
                    LocalDateTime mn = seq.series().stream()
                        .map(Serie::dateDebutMatelassage)
                        .filter(Objects::nonNull).min(naturalOrder()).orElse(null);
                    LocalDateTime mx = seq.series().stream()
                        .map(Serie::dateFinCoupe)
                        .filter(Objects::nonNull).max(naturalOrder()).orElse(null);
                    if (mn == null || mx == null) return 0.0;
                    return Duration.between(mn, mx).toMinutes() / (double) n;
                }));
        // … max / median / variance computed from bySeq.values()
    }
}
```

The most important line in this whole plan: the divisor is
`N_boxes(seq)`, not `N_series(seq)`. This was an explicit user
correction over the existing `md/Advanced_Ordonancement.md` formula.

### 5.4 Composite objective (default weights)

```
score = w_max     · max(boxDur)            // 3
      + w_med     · median(boxDur)         // 2
      + w_var     · variance(boxDur)       // 1
      + w_zone    · zone_overflow_penalty  // 5
      + w_due     · due_shift_miss_penalty // 4
      + w_idle    · plant_idle_minutes     // 0.5
      + w_endst   · Σ_m endStackPenalty(m) // 5
      + w_late    · late_machine_minutes   // 1
      + w_intra   · intra_zone_spread      // 3 (new)
      + w_churn   · replan_churn_pct       // 2 (new, throttle above 5%/min)
```

Weights live in `OptimizerRun.algo_config_json` so Process can A/B
tune without redeploy.

### 5.5 Optimizer Console page — `/process/optimizerConsole` (NEW)

The page the user said they wanted.

```
┌──────────────────────────────────────────────────────────────────┐
│ ENGINE  ●IMPROVING   Run #847   started 05:32  iter 12 408       │
│ Box-Dur P50: 46.3 min  ▼0.4 min/min   |   [⏸ Pause]  [■ Stop]    │
├──────────────────────────────────────────────────────────────────┤
│ CONVERGENCE CHART (Recharts line chart, 30 min window)           │
│   ─── boxDurMax            ─── boxDurMedian                      │
│   ─── sqrt(variance)        ─── score (right axis)               │
│ [zoom 1m / 5m / 30m / live]                                      │
├─────────────────────────┬────────────────────────────────────────┤
│ INTRA-ZONE FAIRNESS     │ MOVES PER SECOND                       │
│  bar chart per zone     │  swap/relocate/blockmove/2opt/kick     │
│  green if < 10%         │  rolling 60 s, throttled if churn>5%   │
├─────────────────────────┴────────────────────────────────────────┤
│ WEIGHT SLIDERS                       [Apply (writes algo_config)]│
│  w_max [▭▭▭▭▭▭▭□□□] 3   w_med [▭▭▭▭▭□□□□□] 2   ...               │
└──────────────────────────────────────────────────────────────────┘
```

Endpoints:
```
POST /api/ordo/engine/start    body {weights?, seed?}
POST /api/ordo/engine/pause
POST /api/ordo/engine/resume
POST /api/ordo/engine/stop
GET  /api/ordo/engine/state
GET  /api/ordo/engine/indicators?runId&since
PUT  /api/ordo/engine/weights  body {weights}
WS   /topic/ordo/engine        — push state + last sample
```

All guarded by `ROLE_PROCESS` (`@PreAuthorize`).

### 5.6 Optimizer History page — `/process/optimizerHistory` (NEW)

- List of past `OptimizerRun` rows, paginated, filter by date / user.
- Multi-select up to 4 runs → side-by-side convergence comparison
  (overlaid Recharts chart).
- Per-run detail: starting/ending boxDur, total iterations, weight
  config used, churn ratio, top 5 moves accepted.

### 5.7 Work Calendar page — `/process/workCalendar` (NEW)

Calendar grid (3-row Sunday-style heatmap) for the next 90 days. Cells
green if working, grey if closed, amber if "force-open" override.
Click a cell to flip + mandatory `override_reason` field.

`POST /api/workCalendar`, `GET /api/workCalendar?from&to`.

Engine reads it through a `WorkCalendarService.isWorking(date, shift)`
helper called by `FeasibilityGuard`.

---

## 6. Layer D — Self-healing & data quality

The `reviewSerieWaiting()` job is in **CMS-Prod** and currently
disabled. The MG-CMS side needs the audit table + the audit page so
the inference is visible and reversible.

### 6.1 `DateInferenceAudit` (table in §5.1, page below)

`/process/dateInferenceAudit` (NEW):
- Table with serie, machine, inferred dates, source Lectra line,
  written_by_job, written_at.
- Filter: date range, machine, has been rolled back yes/no.
- Per-row `[Annuler]` button (RoleGate: `ROLE_PROCESS` /
  `ROLE_ADMIN`) → POST `/api/dateInference/{id}/rollback` writes
  `rolled_back_at` / `rolled_back_by` and resets the original
  `dateFinCoupe = NULL`.
- KPI: "**forget-to-close rate**" — count of inferences in the last
  shift / total fin-coupes that shift. Shown as a small bar at the
  top.
- Backed by a Spring `ApplicationEventPublisher` event
  `DateInferredEvent` fired by the CMS-Prod job on every back-fill;
  consumed by the engine snapshot refresher.

### 6.2 `SelfHealService` extension

The current service only logs stuck `PENDING` requests. Extend it:
- For each stuck request, fire a `DispatchAuditService.write` row
  with `trigger=SYSTEM_AUTO_HEAL`.
- Optionally re-trigger the dispatcher for that (date, shift) — gated
  by a new flag `mgcms.engine.self-heal.auto-redispatch=false` so we
  observe before letting it self-correct.

---

## 7. Layer E — Admission control polish

`AdmissionService` ships HARD 409. Add SOFT path.

### 7.1 SOFT 200+warnings

Today `/api/admission/check` returns 200 advisory or 409 block. Add a
third state: 200 with `warnings: [...]`. Reasons in the warnings array:

- `WRONG_QUEUE_POSITION` — operator wants serie #3 but #1 is the
  head-of-queue; allow with reason.
- `END_STACK_BREACH` — placing on a machine already past 90 % shift
  capacity (uses `endStackPenalty`).
- `ROLL_BORDERLINE` — `ScanRouleau` shows the right roll on the rack
  but not in the closest position.

### 7.2 `AdmissionOverrideAudit` (table in §5.1)

Every warning that the operator dismisses with "Continue" writes a
row. Surfaced in the new page below.

### 7.3 Admission Audit page — `/process/admissionAudit` (NEW)

Single page with two tabs:
- **Bloquées (409)** — reads `AdmissionBlockedAudit`.
- **Forcées (200+warnings)** — reads `AdmissionOverrideAudit`.

Both share the same filter bar: machine, zone, reason, date range,
matricule. Clickable rows drill into the serie context (sequence,
zone, dispatched_zone, accepted_at).

Visual: each tab has a top-line bar chart "by reason, last 7 days"
that lets Process see if a particular reason is spiking.

---

## 8. Layer F — Operator kiosk extensions

Already shipped: `KioskBanner.js` polls version, fetches next serie.
Extensions:

### 8.1 Roll-feasibility hint

Add `rouleauHint` on `NextSerieDto`. Backend computes via
`ScanRouleauRepository.findCandidatesForSerie(serie)` — picks the
roll matching `partNumberMaterial` and `longueur`, scoped to the
machine's zone rack. Operator sees the rack location in the banner.

### 8.2 Toast on version bump

`KioskBanner.js` already polls. Add a 5 s flash toast when the
version changes while the operator is idle, with a "Voir nouveau"
button that scrolls to the changed row.

### 8.3 Banner-driven admission

The "Démarrer" button on Form.js / FormCoupeNew.js / FormMix.js
calls `/api/admission/check` first:
- 200 → start.
- 200+warnings → yellow modal, operator types reason, override row
  written.
- 409 → red `AdmissionBlockModal.js` (already exists).

This is the only piece that lives in **CMS-Prod**, not MG-CMS.

---

## 9. Layer G — Floor War Room (NEW page)

`/production/warRoom` — TV-friendly fullscreen dashboard for the
plant supervisor's office. Read-only.

```
┌──────────────────────────────────────────────────────────────────┐
│ MG-CMS ▸ FLOOR WAR ROOM                       2026-05-05  S2     │
├──────────────────────────────────────────────────────────────────┤
│ ENGINE  ●IMPROVING                BoxDur P50  46m  P95  78m       │
│ Run #847  iter 12 408   ▼0.4 min/min                              │
│                                                                  │
│ ZONE HEATMAP (live)        ┃ TOP 5 LATE SEQUENCES                 │
│   First Article  ▓▓▓▓ 102% ┃   2026-04-20-12  +18m on dueShift   │
│   Serie          ▓▓░░  64% ┃   2026-04-20-15  +9m                 │
│   Prototype      ░░░░  31% ┃   ...                                │
│   Laser Pool     ▓▓▓░  81% ┃                                      │
│                            ┃                                     │
│ MACHINE GRID (sparkline %) ┃ ALERTS                              │
│   30 cells (10 cols × 3)   ┃   ⚠ 2 fin-coupe inférées last 30 min │
│                            ┃   ⚠ 1 sequence rejetée par chef     │
└──────────────────────────────────────────────────────────────────┘
```

Visuals:
- Engine state pill animates green/amber/red.
- Zone heatmap mirrors §4.2 but expanded.
- Sankey on a "▶" pop-out: serie flow STRICT → SHARED for the shift.
- Auto-refreshes via WebSocket — no polling.

Role gate: anyone with `ROLE_VIEWER` and above. Useful for the
chef-d'équipe office TV.

---

## 10. Layer H — KPI Dashboard

`/ordonnancement/kpi` — the read-only sister to `/process/optimizerConsole`.
Where Optimizer Console is "what is the engine doing right now", KPI
Dashboard is "what has it produced this shift / day / week".

### 10.1 Top-line KPIs (cards)

| Card | Source | Target |
|---|---|---|
| boxDur P50 | `OptimizerIndicatorSample.box_dur_median` (last sample) | < 50 min |
| boxDur P95 | derived | < 90 min |
| Variance | `box_dur_variance` | falling trend |
| First-Box-Ready | new metric | < 25 min |
| Plant Idle Ratio | `plant_idle_ratio` | < 8 % |
| Replan Churn | `replan_churn_pct` | < 5 %/min |
| Forget-to-close rate | DateInferenceAudit count / fin-coupes | < 3 % |
| Inassignables | UnassignableSerie open count | 0 |

### 10.2 Trend chart row

- 7-day trend per KPI (small Recharts area).
- Click → expanded chart with overlay of weight changes (markers
  drawn from `OptimizerRun.started_at`).

### 10.3 Comparison view

Toggle "vs last week" → overlays previous-week curve in faint dashed.

### 10.4 Drill-downs

- Click "Inassignables: 4" → Inassignables page (§4.5).
- Click "Forget-to-close: 2" → DateInferenceAudit page (§6.1).
- Click any boxDur cell → list of contributing sequences.

---

## 11. Cross-cutting — events, websockets, observability

### 11.1 Application events (Spring `ApplicationEventPublisher`)

| Event | Fired by | Consumed by |
|---|---|---|
| `ShiftZoneConfirmedEvent` | ShiftZoneConfirmationService | EngineTickService, OptimizerSnapshotRefresher, WS broadcaster |
| `ZoneMachineToggledEvent` | ShiftZoneConfirmationService | same |
| `SequenceAcceptedEvent` | DispatcherController.setAcceptance | OptimizerSnapshotRefresher, WS |
| `SequenceRejectedEvent` | same | DispatcherService (re-dispatch) |
| `SequenceDispatchedEvent` | SequenceDispatcherService.publish | ChefDeZonePage WS |
| `SequencePinnedEvent` | new pin endpoint | engine (excludes from greedy) |
| `MachineQueueChangedEvent` | OptimizerCoordinationThread | KioskService (version bump) |
| `DateInferredEvent` | CMS-Prod review job | OptimizerSnapshotRefresher |
| `OptimizerStateChangedEvent` | ContinuousOptimizerService | WS for UI |
| `OptimizerSampleEvent` | engine (every 1 s) | WS + persistence batch |

### 11.2 WebSocket topics

```
/topic/zone/{zoneName}      — sequence accepted, rejected, pulled, etc.
/topic/machine/{machineNom} — queue change for one machine (low fanout)
/topic/ordo/engine          — engine state + latest sample
/topic/dispatcher           — heatmap refresh trigger
/topic/admission            — block / override toasts for Process
```

`WebSocketConfig.java` already has the broker. Need to add the
`@SendTo` / `messagingTemplate.convertAndSend` calls at every event
listener.

### 11.3 Observability

- Every endpoint emits a Micrometer timer.
- `OptimizerIndicatorSample` writes are the de-facto engine
  trace — keep last 7 days hot, archive older into a side table.
- A new `/api/health/scheduling` endpoint returns:
  ```json
  {
    "engine": "IMPROVING",
    "lastSampleAge": 0.8,
    "dispatchedShifts": ["2026-05-05/1", "2026-05-05/2"],
    "openInassignables": 2,
    "stuckPending": 0,
    "lastInferenceAt": "2026-05-05T07:34:12"
  }
  ```
- Every audit table has a `created_at` index + 7-day retention via
  `RetentionCronService` (already in code).

---

## 12. Migrations summary

| Script | What it does |
|---|---|
| `V12_01__dispatch_audit.sql` | Adds `DispatchAudit` (§4.6) |
| `V13_01__work_calendar.sql` | Adds `WorkCalendar` (§5.1) |
| `V13_02__optimizer_run.sql` | Adds `OptimizerRun` (§5.1) |
| `V13_03__optimizer_indicator_sample.sql` | Adds `OptimizerIndicatorSample` (§5.1) |
| `V13_04__date_inference_audit.sql` | Adds `DateInferenceAudit` (§5.1) |
| `V13_05__admission_override_audit.sql` | Adds `AdmissionOverrideAudit` (§5.1) |
| `V14_01__cleanup_legacy_scheduling.sql` | Drops dead `OptimizedPlan` + `OptimizedSeriesAssignment` tables (see §15) |

All forward-only. Each has a rollback feature flag (already the
project pattern).

---

## 13. Phases — sequencing & priority

> **Hard rule.** Every phase ships behind a flag, has an acceptance
> test, and a 1-config-flip rollback. Same posture as FULL_GUIDE §1.

### Phase A — Heatmap & ZoneLoad (2 sprints)
> *Closes the dispatcher's actual goal: show `(type, zone)` overload.*

- `ZoneLoadService` + endpoints `/api/zoneLoad/matrix`, `/rows`.
- ProcessDispatcher.js heatmap panel.
- PdC heatmap upgrade in `PlanDeCharge.js`.
- `DispatchAudit` table + audit panel.
- New endpoints: `force`, `pin`, `unpin`, `rebalance`, `pull`.
- Inassignables drawer + `/process/inassignables` page.
- Acceptance: heatmap renders with live data; click a cell drills to
  the sequences; force/pin survives a tick.
- Rollback flag: `mgcms.dispatcher.heatmap.enabled`.

### Phase B — SerieTableAssigner (1 sprint)
- `SerieTableAssigner` bean + tests.
- Wire into dispatcher publish (writes virtual table on the
  projection; populates `tableIsVirtual`).
- Wire into kiosk `nextSerie` so banner shows the rack hint.
- Acceptance: 5 hand-picked series get the same table the planner
  picks by eye; one infeasible serie shows in Inassignables with the
  right reason.

### Phase C — Zone admin & ownership (1 sprint)
- `/admin/zones` page (STRICT/SHARED + invariant validation).
- `/process/zoneOwnership` matrix replaces `UserZoneAdmin.js`.
- `PUT /api/userZone/{user}/default/{zone}`.
- Acceptance: a chef logs in and lands on their default zone; setting
  a machine type into both STRICT and SHARED is blocked at save.

### Phase D — Confirmation gate lifecycle (1 sprint)
- T-60 pre-dispatch job.
- T0 auto-accept fallback job.
- `added_after_start` flag wiring.
- Acceptance: leave a chef logged-out for a shift → SYSTEM_AUTO row
  appears at T0; a machine added at T+1h is flagged in the audit.

### Phase E — Continuous Optimization Engine, skeleton (3 sprints)
- Schema for `OptimizerRun`, `OptimizerIndicatorSample`,
  `WorkCalendar`.
- `ScheduleSnapshot` + `SnapshotBuilder` (5 queries).
- `BoxDurationCalculator` + `BoxDurationObjective`.
- `ContinuousOptimizerService` lifecycle (IDLE → WARMING →
  IMPROVING → PAUSED → STOPPED) — single thread first.
- Engine REST endpoints + `/topic/ordo/engine` WS.
- `/process/optimizerConsole` page with Recharts convergence chart.
- Acceptance: Start → engine moves to IMPROVING; box-dur median
  drops over 5 min on a seeded dataset; Stop reverts to last best.

### Phase F — Engine perturbation moves (1 sprint)
- Swap, Relocate, Sequence-block move, 2-opt, Kick.
- FeasibilityGuard with the HARD rules in
  `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §2.5`.
- Replan-churn auto-throttle.
- Acceptance: convergence curve descends measurably; churn never
  exceeds 5 %/min for more than one cycle.

### Phase G — Multi-zone parallelism (1 sprint)
- `ZoneOptimizerThread` per STRICT zone.
- `SharedZoneArbiterThread` per SHARED zone.
- `CoordinationThread` writes one `MachineQueue` batch.
- Acceptance: with 3 zones seeded, three threads visible in JFR; one
  zone's stall doesn't block the others.

### Phase H — Self-healing audit (½ sprint)
- `DateInferenceAudit` schema, entity, repo.
- `/process/dateInferenceAudit` page + rollback button.
- `DateInferredEvent` consumed by snapshot refresher.
- Acceptance: a manually inserted inference appears on the page;
  rollback resets `dateFinCoupe = NULL`.

### Phase I — Admission warnings (½ sprint)
- 200+warnings path on `/api/admission/check`.
- `AdmissionOverrideAudit` schema + `/process/admissionAudit` page.
- `KioskBanner.js` yellow modal for warnings.
- Acceptance: starting serie #3 instead of head-of-queue triggers a
  yellow modal; override row appears in the audit page.

### Phase J — KPI dashboard + War Room (1 sprint)
- `/ordonnancement/kpi` page.
- `/production/warRoom` fullscreen page.
- 7-day trend persistence via `OptimizerIndicatorSample`.
- Acceptance: 7-day trend line visible; click a card drills through
  to the matching audit page.

### Phase K — Cleanup (½ sprint)
- Delete `domain/scheduling/OptimizedPlan*`,
  `services/scheduling/SchedulingOptimizationService` and their
  repos / payloads (the v1 design that v2 explicitly withdrew —
  see §15).
- Audit unused `Ordonnancement.js`, `OrdonnancementV2.js`,
  `OrdonnancementV3.js`. Confirm they are not routed.
- `V14_01__cleanup_legacy_scheduling.sql` drops dead tables.

Total: ≈ 12 sprints if executed sequentially. Phases A/B/C/D run
sequentially (each unblocks the next). Phases E and the rest of A/B/C
can branch in parallel after Phase A completes.

---

## 14. Acceptance criteria (across the program)

A reviewer should be able to verify the program shipped by running
this script against a staging copy:

1. `Flyway migrate` reports every script V2_01..V14_01 success.
2. As `ROLE_PROCESS`: open `/processDispatcher` — heatmap renders, a
   red cell tooltips with planned/cap/eta, click drills to a sequence
   list with `[Forcer]` / `[Pin]`.
3. As `ROLE_PROCESS`: open `/process/optimizerConsole` —
   `[Démarrer]` shifts engine to WARMING then IMPROVING within 30 s.
   Median boxDur falls visibly over 2 min on the seeded dataset.
4. As `ROLE_CHEF_DE_ZONE`: open `/production/chefDeZone` — machine
   strip is live, accepting a sequence flips the optimiser snapshot,
   pinning a serie survives a tick (verified via
   `OptimizerIndicatorSample` — the score at t+15 min did not drop
   below the score-at-pin).
5. As operator on Form.js: starting on the wrong queue position
   surfaces a yellow modal; on a wrong-zone machine, a red modal.
   Audit rows appear in `/process/admissionAudit`.
6. Forget to click Fin Coupe on a serie → after 10 min,
   `DateInferenceAudit` shows the inference; clicking `[Annuler]`
   resets `dateFinCoupe = NULL`.
7. Open `/production/warRoom` on a TV — page renders fullscreen,
   updates without reload via WS, no errors in the console.
8. Set every flag to `false` in `application.properties` → restart →
   the app behaves byte-for-byte as Phase 0 (the regression guard).

---

## 15. Cleanup — what gets deleted

The following are **dead code from the v1 design that v2 withdrew**.
Verified at review time as not referenced by any controller or test
that ships in this program. Phase K removes them.

```
src/main/java/com/lear/MGCMS/domain/scheduling/OptimizedPlan.java
src/main/java/com/lear/MGCMS/domain/scheduling/OptimizedSeriesAssignment.java
src/main/java/com/lear/MGCMS/domain/scheduling/MachineScheduleStatus.java
src/main/java/com/lear/MGCMS/domain/scheduling/MaterialLogistics.java
src/main/java/com/lear/MGCMS/domain/scheduling/ScheduleInterval.java
src/main/java/com/lear/MGCMS/domain/scheduling/SchedulingConfig.java
src/main/java/com/lear/MGCMS/domain/scheduling/SequenceSchedule.java
src/main/java/com/lear/MGCMS/domain/scheduling/SerieSchedule.java
src/main/java/com/lear/MGCMS/domain/scheduling/ShiftSchedule.java
src/main/java/com/lear/MGCMS/repositories/scheduling/*Repository.java
src/main/java/com/lear/MGCMS/services/scheduling/SchedulingOptimizationService.java
src/main/java/com/lear/MGCMS/services/scheduling/SchedulingService.java   (verify no callers)
src/main/js/components/Layout/Ordonnancement.js                            (commented out in Dashboard)
src/main/js/components/Layout/OrdonnancementV2.js                          (same)
src/main/js/components/Layout/OrdonnancementV3.js                          (verify)
```

A `V14_01__cleanup_legacy_scheduling.sql` migration drops the
matching tables.

> **Before merging this phase**, do a full repo grep for each class
> name to confirm no live caller. Any reference outside the listed
> files blocks the cleanup until rewired.

---

## 16. What was deliberately rejected

The following ideas were considered and rejected. Listed here so the
next person doesn't reopen them:

- **Pre-reserving rolls.** `ScanRouleau` is read live by the
  feasibility guard. Pre-reservation creates a synchronisation
  problem with the floor — the prior design owner specifically
  rejected this in `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §0`.
- **A new `ZoneEfficiency` table.** `CapaciteInstallee.efficienceTarget`
  already keys efficiency by `(date, shift, groupe)`. Adding a per-zone
  override is a future side table, not now.
- **A `User.defaultZone` column.** Chef-to-zone is many-to-many via
  `UserZone`. The default is encoded by `is_default = 1` with a
  partial unique index, not by a single column on `User`.
- **FROZEN/HARD/SOFT scheduling windows.** The plan is fully
  flexible. Frozen sets are derived from `pinnedByChef = 1` or "serie
  has started" — never from a time bucket.
- **Splitting a sequence across STRICT zones.** A sequence
  dispatches to exactly one STRICT zone. Series with unavailable
  types fan out to SHARED zones. This is the cleanest model for box
  traceability.

---

## 17. How to work this plan

1. Approve scope phase by phase (A → K). Each phase is a separate
   merge.
2. For each phase, I (or whoever is implementing) will:
   - Re-read the phase here + the cited sections of the source
     plans.
   - Produce the explicit file list before writing code.
   - Ship behind the phase's feature flag with one acceptance test.
   - Demo against the staging DB before flipping the flag.
3. The visual upgrades in §3.4, §4.2, §5.5, §9, §10 are intentionally
   uniform: SVG heatmap, Recharts time series, Sankey for flow,
   sparklines in cells. Pick a single design library the first time
   and reuse — don't mix Recharts with `react-chartjs-2`.

When ready to start a phase, paste its heading into the chat
(e.g. *"Start Phase E — Continuous Optimization Engine, skeleton"*).

---

## 18. Baseline & comparison — proving the engine's value

The user explicitly asked: *"compare the indicators of old data with
… now when they have started working with the engine to see the
improvement in production"*. The Optimizer Console (§5.5) only starts
emitting `OptimizerIndicatorSample` rows once the engine is running —
those rows alone cannot answer the question. We need a baseline.

### 18.1 The `KpiSnapshot` table — engine-independent KPI history

```sql
-- V13_06__kpi_snapshot.sql
CREATE TABLE KpiSnapshot (
    id                  BIGINT IDENTITY PRIMARY KEY,
    snapshot_date       DATE        NOT NULL,
    shift_number        INT         NOT NULL,
    captured_at         DATETIME2   NOT NULL,
    -- Box-duration KPI from completed sequences in the shift (Contract C9)
    box_dur_max         DECIMAL(10,2) NULL,
    box_dur_median      DECIMAL(10,2) NULL,
    box_dur_p95         DECIMAL(10,2) NULL,
    box_dur_variance    DECIMAL(12,4) NULL,
    -- First-box-ready (P50 across closed sequences)
    first_box_ready_p50 DECIMAL(10,2) NULL,
    -- Plant idle: gaps on active machines / shift minutes
    plant_idle_ratio    DECIMAL(5,4) NULL,
    -- Intra-zone type spread (max - min loadPct across machineTypes
    -- inside one STRICT zone, averaged over zones — uses Contract C7)
    intra_zone_spread   DECIMAL(7,4) NULL,
    -- Inter-zone spread (max - min loadPct across STRICT zones)
    inter_zone_spread   DECIMAL(7,4) NULL,
    -- Forget-to-close rate: DateInferenceAudit count / fin-coupes
    forget_to_close_pct DECIMAL(7,4) NULL,
    -- Open Inassignables at capture time
    open_inassignables  INT NULL,
    -- Engine context — null when the engine wasn't running
    engine_run_id       BIGINT NULL,
    engine_active       BIT NOT NULL DEFAULT 0,
    engine_iterations   INT NULL,
    weights_json        NVARCHAR(MAX) NULL,
    -- Bookkeeping
    source              VARCHAR(16) NOT NULL,  -- HISTORICAL_BACKFILL | LIVE_HOURLY
    INDEX IX_KS_at         (captured_at),
    INDEX IX_KS_engine     (engine_active, captured_at),
    INDEX IX_KS_date_shift (snapshot_date, shift_number)
);
```

### 18.2 Two writers — same shape

1. **Historical backfill** — a one-shot `KpiSnapshotBackfillService` that
   walks the last 90 days of `CuttingRequestSerieData` (`dateDebutMatelassage`
   / `dateFinMatelassage` / `dateFinCoupe` / `CuttingRequestBox`), groups by
   `(date, shift)` and writes one row per shift with
   `engine_active = 0`, `source = 'HISTORICAL_BACKFILL'`. Run once when
   Phase J ships. Idempotent — keyed on `(snapshot_date, shift_number,
   source)` so re-running is safe.
2. **Live hourly** — `KpiSnapshotLiveJob`, `@Scheduled(cron = "0 5 *
   * * *")`, writes one row at the top of every hour using the same
   computation against the current shift. When `ContinuousOptimizerService`
   is in `IMPROVING` state, set `engine_active = 1`,
   `engine_run_id = currentRun.id`, copy the live indicator means.

Both writers share **one** computation function — the same code path
that `BoxDurationCalculator` (Contract C9) and `ZoneLoadService`
(Contract C7) use, just over a closed shift's data instead of a live
snapshot. **No metric is computed twice in different ways.**

### 18.3 The comparison view — `/ordonnancement/kpi` "Avant / Après"

In the new KPI dashboard (§10), every top-line card carries a small
delta badge:

```
┌────────────────────────────┐
│  Box-Dur P50               │
│  46.3 min   ▼ 12.4 min     │  ← delta vs the matched baseline
│  vs 58.7 min sans moteur   │
└────────────────────────────┘
```

The baseline is **matched**: same day-of-week and shift-number from
the last 4 weeks where `engine_active = 0`. This controls for the
day-of-week effect (Mondays cut differently than Wednesdays) without
requiring the user to think about it.

A "Voir tendance" link opens the full chart — two overlaid Recharts
lines:
- **Sans moteur** (grey, dashed) — average of the matched baseline
  shifts.
- **Avec moteur** (red — Lear brand) — the active engine period.

The y-axis is the metric value; vertical markers show:
- Engine start (`OptimizerRun.started_at`) — green tick.
- Weight changes (`OptimizerRun.algo_config_json` diffs) — blue tick.
- Engine stop — red tick.

### 18.4 What the user sees in week 1

Day 0 — engine off, backfill loaded → dashboard shows 30 days of
"Sans moteur" history. Already useful: spreads + churn baselined.

Day 1 — engine started for shift 1 → after the shift closes, one
new "Avec moteur" sample appears. Card shows day-1 delta.

Day 7 — 21 shifts of "Avec moteur" data → the dashboard draws two
distinguishable curves. Manager can demonstrate the savings to plant
leadership.

### 18.5 Hard rule

`KpiSnapshot.engine_active` is the **single** discriminator. We never
fork the schema into "before" and "after" tables — same rows,
different flag. This is the row that lets us ask any future question
("show me weeks where boxDur P95 was under 80 with engine on, vs
without") with one SQL filter.

---

## 19. Configurability — every knob with a default

Every behavioural choice in this program is a property in
`application.properties`. Lear ops can tune them without a code
change. This section is the master index — when you need to know
"what flag does X", grep this section first.

### 19.1 Feature flags (boolean — default safe-off)

| Property | Default | Effect when `false` |
|---|---|---|
| `mgcms.dispatcher.enabled` | `true` | `/api/dispatcher/*` returns 404; UI shows banner |
| `mgcms.dispatcher.allow-unconfirmed-zones` | `true` | Without confirmation, dispatcher won't accept any zone |
| `mgcms.dispatcher.heatmap.enabled` | `true` | Heatmap panel hidden in `ProcessDispatcher.js`; equilibre not computed |
| `mgcms.engine.zone-aware` | `false` | EngineTickService dispatches without zone scoping |
| `mgcms.engine.auto-tick.enabled` | `false` | No cron dispatch |
| `mgcms.engine.optimizer.enabled` | `false` | ContinuousOptimizerService refuses to start |
| `mgcms.engine.self-heal.enabled` | `false` | Stuck PENDING goes unaudited |
| `mgcms.engine.self-heal.auto-redispatch` | `false` | Self-heal logs only — does not act |
| `mgcms.admission.enforce` | `false` | `/api/admission/check` always returns 200 |
| `mgcms.admission.warnings.enabled` | `false` | 200+warnings path collapses to 200 |
| `mgcms.kiosk.roll-hint.enabled` | `false` | NextSerieDto.rouleauHint is null |
| `mgcms.retention.enabled` | `false` | Audit purge cron skipped |
| `mgcms.kpi.snapshot.live.enabled` | `false` | Hourly KpiSnapshot writer disabled |
| `mgcms.kpi.snapshot.backfill.enabled` | `false` | Historical backfill skipped on startup |

### 19.2 Numeric thresholds (load colours, fairness, churn)

| Property | Default | Used by |
|---|---|---|
| `mgcms.shift.duration-minutes` | `460` | PdC + ZoneLoadService capacity denominator |
| `mgcms.shift.break-minutes` | `30` | PdC effective minutes |
| `mgcms.zoneload.warning-threshold-pct` | `80` | Heatmap amber threshold |
| `mgcms.zoneload.danger-threshold-pct` | `100` | Heatmap red threshold |
| `mgcms.zoneload.intra-zone-spread-target-pct` | `15` | Equilibre badge green threshold |
| `mgcms.zoneload.intra-zone-spread-warning-pct` | `25` | Equilibre badge amber threshold |
| `mgcms.zoneload.inter-zone-spread-target-pct` | `15` | Same, across STRICT zones |
| `mgcms.zoneload.inter-zone-spread-warning-pct` | `30` | Same, amber |
| `mgcms.assigner.weight.queue` | `1.0` | SerieTableAssigner score α |
| `mgcms.assigner.weight.length` | `10.0` | β — length mismatch penalty |
| `mgcms.assigner.weight.endstack` | `5.0` | γ — end-stack penalty |
| `mgcms.assigner.weight.specialisation` | `2.0` | δ — specialisation bonus |
| `mgcms.assigner.endstack-threshold` | `0.9` | Quadratic kick-in point |
| `mgcms.engine.churn.threshold-pct` | `5.0` | Above this, engine throttles kick / block-move |
| `mgcms.engine.snapshot.refresh-sec` | `60` | Max snapshot refresh cadence |
| `mgcms.engine.budget.cpu-ms-per-sec` | `100` | Per zone optimizer thread |

### 19.3 Engine score weights (composite objective §5.4)

These live in `OptimizerRun.algo_config_json` so each run can A/B
them, but the **defaults** come from `application.properties`:

| Property | Default | Score term |
|---|---|---|
| `mgcms.engine.weights.w-max` | `3.0` | `w_max · max(boxDur)` |
| `mgcms.engine.weights.w-med` | `2.0` | `w_med · median(boxDur)` |
| `mgcms.engine.weights.w-var` | `1.0` | `w_var · variance(boxDur)` |
| `mgcms.engine.weights.w-zone` | `5.0` | SHARED-overflow temptation penalty |
| `mgcms.engine.weights.w-due` | `4.0` | dueShift miss penalty |
| `mgcms.engine.weights.w-idle` | `0.5` | plant idle minutes |
| `mgcms.engine.weights.w-endstack` | `5.0` | end-stack penalty sum |
| `mgcms.engine.weights.w-late` | `1.0` | late-machine minutes |
| `mgcms.engine.weights.w-intra` | `3.0` | intra-zone fairness term |
| `mgcms.engine.weights.w-churn` | `2.0` | replan-churn penalty |

### 19.4 Cron expressions

| Property | Default | Meaning |
|---|---|---|
| `mgcms.engine.auto-tick.cron` | `0 */5 * * * *` | Every 5 min |
| `mgcms.retention.cron` | `0 30 2 * * *` | 02:30 daily |
| `mgcms.engine.self-heal.cron` | `0 */10 6-22 * * *` | Every 10 min, 06:00–22:00 |
| `mgcms.kpi.snapshot.live.cron` | `0 5 * * * *` | 5 min after each hour |

### 19.5 Retention windows

| Property | Default | What it deletes |
|---|---|---|
| `mgcms.retention.days` | `7` | UnassignableSerie + AdmissionBlockedAudit + AdmissionOverrideAudit + DispatchAudit |
| `mgcms.retention.kpi-days` | `90` | KpiSnapshot |
| `mgcms.retention.optimizer-sample-days` | `7` | OptimizerIndicatorSample |
| `mgcms.retention.date-inference-days` | `30` | DateInferenceAudit |

### 19.6 The "is everything safely off?" sanity check

Add a lightweight `GET /api/health/scheduling/flags` endpoint that
returns every `mgcms.*` flag's current value. The Optimizer Console
shows this as a top-strip badge so ops can see at a glance
*"engine: zone-aware ON, optimizer ON, admission: enforce OFF"*. No
need to SSH into the server.
