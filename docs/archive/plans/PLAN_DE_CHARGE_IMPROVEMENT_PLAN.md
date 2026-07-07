# Plan de Charge — Rectification Plan

> **Project:** MG-CMS (Spring Boot + React + SQL Server) — Lear Tangier TFZ.
> **Goal:** Keep Plan de Charge as the **macro**, per-shift load view, but
> make it (a) fast on a big DB, (b) aligned with the new
> `ShiftZoneConfirmation` + `CuttingRequest.dispatchedZone` from
> `SEQUENCE_DISPATCHER_PLAN.md`, and (c) a true feedback loop that the
> Continuous Optimizer (see `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md`)
> can trust.
> **Authoritative doc read together with this plan:** [md/PLAN_DE_CHARGE.md](../md/PLAN_DE_CHARGE.md).

---

## 0. One-paragraph intent

Plan de Charge must answer **only** three questions, fast:
1. *For this shift, what is the planned load per `(zone, machineType)`, in
   minutes and in %?*
2. *What was the actual load for the previous 8 h, by the same breakdown?*
3. *Where is the mismatch?* (overload, retard, capacity lost to downtime)

It **does not** decide which zone or machine a sequence runs on —
`SEQUENCE_DISPATCHER_PLAN.md` does. It **does not** sequence series on a
machine — `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN.md` does. Keeping
Plan de Charge boring is the point.

---

## 1. What is wrong today (the things to rectify)

| # | Issue | Evidence | Fix |
|---|---|---|---|
| 1 | `getDetailedSeriesForShift()` loads **full** `CuttingRequestSerieData` (50+ fields) for every serie in the shift | `PlanDeChargeService.java` (~980 lines) | Switch to `CuttingRequestSerieDataLight` via an explicit 19-col JPQL projection, already the pattern used by `CuttingRequestSerieDataRepository.findSeriesBySequencesLight()` (L91–96) |
| 2 | Load-% formula **ignores efficiency** | `PlanDeChargeService.java:780–783` | Multiply `availableTime` by `CapaciteInstallee.efficienceTarget / 100` (existing entity, keyed by `(date, shift, groupe)` with `groupe ∈ {Coupe, Laser}`). The earlier draft proposed a new `ZoneEfficiency` table — **superseded** by reuse of `CapaciteInstallee` per `SEQUENCE_DISPATCHER_PLAN §3.4` and contract C5. |
| 3 | Formula duplicated in three places (PdC, Sequence Dispatcher draft, Optimizer draft) → drift risk | Manual inspection | Extract a single `CuttingTimeCalculator` Spring bean (contract **C1** in `SEQUENCE_DISPATCHER_PLAN §8.2`) and call it from all three |
| 4 | Active-machine set is recomputed from scratch on every PdC call by re-reading `EtatMachineHistorique` | `PlanDeChargeService` + `EtatMachineHistoriqueService` | Read via the new `ActiveMachineResolver` bean (contract **C2**) — same source as Dispatcher & Optimizer; cached per `(date, shift, zone)` for 30 s |
| 5 | No pagination / chunking anywhere — a single shift with 400 series returns a 200 kB JSON blob, and a 7-day view multiplies that by 21 | Controller layer | Paginate at the **shift level** (`GET /api/planDeCharge/shift?date=&shift=`) and require **shift granularity** for the heavy endpoint; use a lighter `/summary` endpoint for the 7-day overview |
| 6 | Dashboard re-queries on every filter click | React `PlanDeCharge.js` | Keep shift totals in React state, only re-query when `(date, shift)` changes, not when the user toggles a zone chip |
| 7 | No "finished recently" reconciliation — shift load stays planned even when series actually finished on a different machine | No code | Add a daily (and on-demand) reconciliation pass that reads `CuttingRequestSerieDataRepository.findRelevantSequences(since, now)` for the last 8 h and overlays actuals in a distinct colour |
| 8 | No banner when a `ShiftZoneConfirmation` is missing | — | Surface the "Zone X: non confirmée" banner from `SEQUENCE_DISPATCHER_PLAN §4.5` at the top of the PdC shift view |

---

## 2. Query strategy — load one thing at a time

This is the biggest user ask ("big database"). The pattern below is a
**hard rule** for every new endpoint added in this plan:

1. **Never fetch an `@Entity` if a `data.*Light` projection exists.** Repo
   queries must select only the columns the caller uses. If a projection
   doesn't exist, create it in
   `com.lear.MGCMS.domain.CuttingRequest.data` first.
2. **Never fetch across `@ManyToOne` graphs.** Resolve FK columns
   separately (one extra query) rather than loading the associated
   entity. Example: don't traverse `ProductionTable.zone.nom` — select
   the zone name directly via a `JOIN` in the projection query.
3. **One shift at a time.** A PdC call without `(date, shift)` is
   rejected with 400. The 7-day "overview" endpoint returns only
   per-shift **totals**, not per-serie rows.
4. **Streaming-friendly when it must be big.** For end-of-month
   export, use `Stream<CuttingRequestSerieDataLight>` + `@QueryHints`
   `HINT_FETCH_SIZE=1000` (pattern used by `BoxWeightController`).
5. **Time-window defaults.** Any "recent activity" query defaults to
   the `findRelevantSequences(now − 8 h, now)` window. Full-history
   queries require an explicit `from=…&to=…` pair.

Applied to the current controller surface:

| Endpoint | Current | Proposed |
|---|---|---|
| `GET /api/planDeCharge/shiftDetail` | loads all series + placements + rouleaux | returns projection: `List<ShiftCellDto>` (one row per `(zone, machineType)` with `plannedMin, capacityMin, load%, activeMachines, sequencesCount`) |
| `GET /api/planDeCharge/7day` | loads 21 shifts of full series | returns `List<ShiftSummaryDto>` (one row per shift) |
| `GET /api/planDeCharge/series?shiftDate&shift&zone&machineType` | doesn't exist | paginated, `CuttingRequestSerieDataLight`, `Page<SerieLightDto>` |

New DTO suggestions (all belong in `controller/dto/planDeCharge/`):

```java
record ShiftCellDto(
    String zone, String machineType,
    double plannedMin, double capacityMin,
    double loadPct, int activeMachines,
    int sequencesCount, boolean zoneConfirmed
){}
record ShiftSummaryDto(LocalDate date, int shift, double plannedMin,
    double capacityMin, double loadPct, int missingConfirmations){}
```

---

## 3. Formula correction — efficiency + confirmation

Replace `PlanDeChargeService.java:780–783` with:

```java
double efficience = capaciteInstalleeService
        .getEffective(date, shift, groupeOf(machineType))   // contract C5
        .getEfficienceTarget();                              // default 90.0
BigDecimal effectiveCapacity = availableTime
        .multiply(BigDecimal.valueOf(efficience))
        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
if (effectiveCapacity.compareTo(BigDecimal.ZERO) > 0) {
    loadPercentage = plannedTime.multiply(BigDecimal.valueOf(100))
            .divide(effectiveCapacity, 2, RoundingMode.HALF_UP);
}
```

Where:
- `capaciteInstalleeService.getEffective(date, shift, groupe)` reads
  the existing `CapaciteInstallee` row (NOT a new `ZoneEfficiency`
  entity — see §1 issue 2 + dispatcher §3.4). `groupeOf(machineType)`
  maps `Lectra | Lectra IP6 | Gerber → "Coupe"` and
  `LASER-DXF | LASER-LSR → "Laser"` (matches the existing
  `PlanDeChargeService` mapping). Default 90.0 when no row exists.
- `availableTime` already accounts for machine count × shift minutes,
  but the machine count must now come from `ActiveMachineResolver` —
  see §1 issue 4.
- `plannedTime` must use the shared `CuttingTimeCalculator` bean (C1) —
  which itself batches the CMS-DB `TimingModel` join by placement, so
  validated/real cutting times come from the right source even though
  they are *not* columns on `CuttingRequestSerie`.

Same adjustment goes into every place `loadPercentage` is computed
(grep for `loadPercentage`, `plannedTime.multiply(100)`).

---

## 4. Actuals overlay (the feedback loop)

A new service `PlanDeChargeActualsService`:

- Reads `findRelevantSequences(now − 8 h, now)` from
  `CuttingRequestSerieDataRepository` (already in the repo, L80–86).
- For each row whose `statusCoupe = 'Complete'`, computes
  `actualCutMin = dateFinCoupe − dateDebutCoupe`.
- Rolls up per `(zone, machineType, shift)` into `ShiftCellDto.actualMin`.
- Writes a snapshot row into an existing `ShiftLoadCalculation` record
  on every run (new nullable columns `actualMin, actualAt`) so the
  history is preserved.

Fires an `ActualsRecomputedEvent` consumed by the Continuous Optimizer
so its "plant idle" penalty stays calibrated.

Scheduled every 10 min (aligns with `reviewSerieWaiting()` in the
`ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §5`). On-demand endpoint:
`POST /api/planDeCharge/recomputeActuals?date=&shift=`.

---

## 5. UI changes (PlanDeCharge.js)

Kept small on purpose — PdC's UI is already mature. Only:

1. **Efficiency badge** per `(zone, machineType)` cell: small grey
   `η=90%` label, clickable to open the existing `CapaciteInstallee`
   edit page (admin only). The badge reads
   `CapaciteInstallee.efficienceTarget` keyed by the cell's
   `(date, shift, groupeOf(machineType))`.
2. **"Non confirmée" banner** pulled from `ShiftZoneConfirmation` — one
   per unconfirmed zone, top of the page, red.
3. **Actuals overlay toggle** — when on, each cell splits into two
   stacked bars: planned (grey outline) vs actual (blue fill).
4. **Drill-through to Sequence Dispatcher** — clicking a `(zone,
   machineType)` cell deep-links to
   `/ordonnancement/sequenceDispatcher?zone=X&machineType=Y`.
5. **Drill-through to Advanced Ordonnancement** — clicking a single
   machine inside the machine status grid deep-links to
   `/ordonnancement/advanced?machine=LECTRA-03`.
6. **Pagination control** only on the optional "all series in shift"
   sub-drawer (page size 50).

No new React libraries. No change to the menu entry.

---

## 6. Phases

| Phase | Scope | Priority | File(s) |
|---|---|---|---|
| **P1** | Extract `CuttingTimeCalculator` bean, repoint PdC, Dispatcher, Optimizer to it; unit test C1 | **HIGH** | `services/CuttingTimeCalculator.java`, `PlanDeChargeService`, new `CuttingTimeCalculatorTest` |
| **P2** | Switch `getDetailedSeriesForShift()` + cousins to `CuttingRequestSerieDataLight` projections; add `ShiftCellDto` + `ShiftSummaryDto` + three new endpoints | **HIGH** | `PlanDeChargeService`, `PlanDeChargeController`, `CuttingRequestSerieDataRepository` |
| **P3** | Efficiency in the formula (§3) — wires `CapaciteInstalleeService.getEffective(date, shift, groupe)` (existing entity, no new CRUD) | **HIGH** | `PlanDeChargeService:780–783`, reuse of existing `CapaciteInstalleeController` |
| **P4** | `PlanDeChargeActualsService` + `ShiftLoadCalculation.actualMin` columns + scheduler | **MEDIUM** | new service, migration, `PlanDeChargeController` |
| **P5** | UI: efficiency badge, non-confirmée banner, actuals toggle, drill-throughs | **MEDIUM** | `src/main/js/components/Layout/PlanDeCharge.js` |
| **P6** | Streaming export for month-end; delete or mark deprecated the old non-paginated routes | **LOW** | `PlanDeChargeExportController` (new) |

---

## 7. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Changing the formula changes every historical report | Add `formulaVersion` column to `ShiftLoadCalculation`; old rows stay readable under v1; new writes go under v2 |
| Shared `CuttingTimeCalculator` bean becomes a bottleneck | It's a pure function (no IO) — keep it stateless; if it grows cache, put the cache in a dedicated `CachedCuttingTimeCalculator` that wraps it |
| Projections lose a field someone was relying on in PdC | Freeze projection field list per endpoint; any add goes through a JIRA-like PR review — in practice: add a new projection, don't bloat the existing one |
| 10-min actuals job misses the exact shift boundary | Fire an extra one at `Tend + 2 min` for each shift via `@Scheduled(cron=…)` on 05:47, 13:47, 21:47 |

---

## 8. Open questions — resolved

The 10 open questions have been answered. Decisions are folded into the
plan body; this section is a changelog.

| # | Question | Decision |
|---|---|---|
| 1 | Formula v2 (with efficiency) — feature flag? | **No flag.** v2 ships as the default — efficiency is part of the formula from day one. |
| 2 | Efficiency granularity — per machine type inside a zone? | **No.** Keep it at `groupe = Coupe / Laser` level (matches the existing `CapaciteInstallee` schema). The earlier example "Lectra IP6 vs Lectra inside the same STRICT zone" is moot since `CapaciteInstallee` already aggregates them under `Coupe`. |
| 3 | Actuals rollup window — extend to 16 h? | **No.** 8 h is enough. |
| 4 | Partial-shift snapshot — show actuals *so far*? | **Yes.** Keep the draft (show so-far progress). |
| 5 | Mid-shift machine removal — prorate or keep full capacity? | **Keep full** for the current shift (assume it was running full). For the next shift, the dropped status in `EtatMachineHistorique` already reduces capacity automatically until the interval end. No proration logic. |
| 6 | Downtime breakdown (P / O / PN) | **Collapse to one "down" bucket** with a sub-label = the longest-interval status code inside the shift. Avoids UI clutter when statuses flap. |
| 7 | `SHIFT_DURATION_MINUTES = 460` — constant or configurable? | **Configurable.** Move to `application.yml` as `mgcms.shift.durationMinutes` (default 460). Read once at startup; future multi-plant support can override per `Plant`. |
| 8 | Actuals-overlay visibility | Visible to `ROLE_PROCESS`, `ROLE_ADMIN`, `ROLE_CHEF_DE_ZONE`, `ROLE_CHEF_EQUIPE`, `ROLE_MAINTENANCE`. (Not visible to plain operators on the floor.) |
| 9 | "Recalculer actuals" button authorisation | **`ROLE_PROCESS` only** can trigger; `ROLE_ADMIN` is read-only on this button. (Different from view authorisation in #8.) |
| 10 | Delete the four obsolete md files now? | **Yes.** Delete in P1. **Also delete** the React entities under `src/main/js/components/Layout/ordonnancement` that reference the dropped scheduling entities, and any controller / service code wired to them. Track this as a dedicated cleanup task in P1 so reviewers can confirm nothing live depends on the removed pieces. |

### 8.1 Phases impact

Folding the answers above:

- **P1** picks up an extra cleanup task: delete the four obsolete md
  files from `md/` (per `ADVANCED_ORDONNANCEMENT_IMPROVEMENT_PLAN §11`)
  AND delete the obsolete `src/main/js/components/Layout/ordonnancement`
  React entities + dead Java code that referenced them. This must be a
  separate commit so the diff is easy to review.
- **P2** adds the `mgcms.shift.durationMinutes` property + repoint
  every `SHIFT_DURATION_MINUTES = 460` constant in `PlanDeChargeService`
  + `PlanDeCharge.js` to read from a single source (the existing
  `getShiftProductiveCapacityMinutes` helper already centralises the
  React side).
- **P3** keeps the `CapaciteInstallee` reuse — no new CRUD page, no
  `ZoneEfficiency` table.
- **P4** writes the `actualMin` columns + the recompute button gated to
  `ROLE_PROCESS` only.
- **P5** adjusts page-level visibility to the role list above.
