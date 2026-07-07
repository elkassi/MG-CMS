# Option C — Full Scheduling Engine: Implementation Plan

Author: Claude (Opus 4.7)
Status: **In progress.** See "Progress log" at bottom for slice-by-slice status.
Context: Follow-up to `docs/dispatcher-engine-review.md`. The user picked
Option C with these constraints:

- KPI = **box-duration** = `(max(plannedFinCoupe) − min(plannedDebutMatelassage)) / nb_boxes`
- Engine runs **indefinitely** (not a fixed-duration optimization round).
- Scale: **≤100 sequences / shift, ≤1000 series / shift**.
- Material check: **`ScanRouleau` (in-zone racks) only** — drop
  `StockStatusClient` HTTP path. Material status is **advisory** (alert
  logistics), not a hard scheduling constraint.
- Public read API surface: `/api/public/next-series?machine=BB2&limit=3`.
- Operator pain points to address:
  - **Machine-type starvation** inside a zone (too much Lectra IP6 work,
    Lectra idle, or vice-versa).
  - **Boxes pile up at end of shift** because too many sequences run in
    parallel — minimise box-duration.

---

## 1. What "Option C" means concretely

Three levels of optimisation, run continuously by one engine:

```
┌──────────────────────────────────────────────────────────────────────┐
│ Level 1 — DISPATCH (sequence → zone)                                │
│   Inputs:  active sequences, zones, capacities, locks, materials    │
│   Output:  bestSequenceZoneMap  (sequence → zoneNom)                │
│   Goal:    balance (zone × machineType) load                        │
└──────────────────────────────────────────────────────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│ Level 2 — ASSIGN (serie → machine, within a zone)                    │
│   Inputs:  zone's dispatched sequences, machines, calendar, locks   │
│   Output:  bestSerieMachineMap   (serieId → machineNom)             │
│   Goal:    balance load across machines of each type AND keep       │
│            sequence affinity (all series of a sequence on same      │
│            or adjacent machines → fewer parallel sequences)         │
└──────────────────────────────────────────────────────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│ Level 3 — SEQUENCE (order on each machine queue)                    │
│   Inputs:  assigned series per machine, dependencies (matelassage   │
│            before coupe), due dates, materials                       │
│   Output:  bestQueue  (machineNom → ordered List<serieId>)          │
│   Goal:    minimise mean & max box-duration per sequence            │
│            respect precedence (matelassage→coupe), due dates,        │
│            material grouping                                         │
└──────────────────────────────────────────────────────────────────────┘
                  │
                  ▼
        plannedStart / plannedEnd per (serie, machine)
                  │
                  ▼
        box-duration KPI feeds back into Level-1 cost
```

The current engine does Level 1 well and Levels 2-3 not at all. The
existing `OrdonnancementService.getNextSeriesForMachine` does a per-machine
queue ordering but **independently** of the engine — there's no shared
schedule, no global optimisation, no box-duration objective.

---

## 2. Data model — what we need vs. what exists

### Already exists (reuse)

| Need | Existing artifact |
|---|---|
| Sequence → zone | `CuttingRequest.dispatchedZone` |
| Serie → machine | `CuttingRequestSerieData.tableCoupe` (and `tableMatelassage`) |
| Real timestamps (after execution) | `dateDebutCoupe`, `dateFinCoupe`, `dateDebutMatelassage`, `dateFinMatelassage` |
| Machine queue (ordered) | `MachineQueue` (already persisted, used by ops) |
| Per-zone capacities | computed via `ProductionTable` + `ActiveMachineResolver` + `CapaciteInstalleeService` |
| Cutting-time lookup | `CuttingTimeCalculator` (uses TimingModel) |
| Spreading-time estimate | `(longueur × nbrCouche × 0.5) + 2` in `BoxDurOptimizedOrderingStrategy` |
| Material checks (in-zone) | `ScanRouleauRepository.findByLocations` |

### New entities / fields needed

| Need | Proposal |
|---|---|
| Planned timestamps from engine | New: `EngineScheduleEntry` table — `(serie_id PK, machine_nom, planned_start, planned_end, planned_phase ENUM(MATELASSAGE,COUPE), planning_run_id, planned_at)` |
| Box info (boxes per sequence) | Already exists: `CuttingRequestBoxInfo` (used in `OrdonnancementService.loadBoxCounts`). Confirm. |
| Per-machine speed / efficience override | Defer — use machine-type group efficience that already exists. |

### Where the engine's "best schedule" lives

- **Hot path** (engine loop, every iteration): in-memory
  `ScheduleSnapshot` keyed by `runId`. Defensive copy via accessor for
  read-only consumers.
- **Warm path** (every N seconds, or on improvement): write the snapshot
  to `EngineScheduleEntry` rows. Replaces the file-based JSON state.
- **Cold path** (server restart): on engine start, **recompute from
  scratch** (per user's answer to Q6 — file restore not a concern; a
  fresh greedy schedule converges in seconds at this scale).

---

## 3. Objective function for Option C

```
cost =   wLoad          * loadSpread                          (per zone × MT)
       + wMachineLoad   * intraZoneMachineSpread              (within zone, per MT)
       + wBoxDuration   * meanBoxDuration                     (KPI: this is the main one)
       + wBoxDurationMax* maxBoxDuration                      (worst-case shield)
       + wLateness      * latenessPenalty                     (due-date violation)
       + wMaterialAlert * (advisory only — count of NOT_IN_ZONE rolls)
```

Defaults (tunable via `EngineProperties`):

| Weight | Default | Rationale |
|---|---|---|
| `wLoad` | 1.0 | Existing primary objective. Keep. |
| `wMachineLoad` | 0.5 | Operator complaint #1 — solves intra-zone MT starvation. |
| `wBoxDuration` | 2.0 | Operator complaint #2 — solves box pile-up. Heaviest weight. |
| `wBoxDurationMax` | 0.5 | Prevents one sequence from being starved. |
| `wLateness` | 0.2 | Already there. |
| `wMaterialAlert` | 0.01 | Advisory only — tiny weight so it breaks ties but never dominates. |

The numbers are starting points. After we wire this up, run on a real shift
and tune from observed behavior.

---

## 4. Implementation slices

The plan is split into 7 slices. Each slice is **independently shippable**
behind a feature flag, and each slice **does not break existing
functionality** until the final flip.

### Slice 0 — Quick wins (P0 from the review, no architecture impact)

Independent of Option C, but valuable in their own right. **~1 day.**

1. Fix `chooseZone()` tiebreak to be load-aware.
2. Wire `convergenceIterations` to `doLoopIteration`. **EDIT:** per user
   answer to Q4 (engine runs indefinitely), wire it to *log* convergence
   instead of stopping. Engine stays running.
3. Drop the file-based `BEST_STATE_FILE` and its hand-rolled JSON parser
   (per user Q6 — file restore not a concern).
4. Make engine SA constants configurable via `EngineProperties.Optimizer`.
5. Remove `StockStatusClient` call from `MaterialAvailabilityChecker` —
   replace with `ScanRouleau` only + a `NOT_IN_ZONE` advisory status
   (per user Q7). Material is no longer a hard constraint.
6. Engine listens for `SequenceAcceptanceChangedEvent` and rebuilds the
   snapshot at next iteration boundary (per user Q8 — pick up at rebuild
   is fine, no immediate interruption needed).

### Slice 1 — Schedule model and snapshot

Adds the in-memory `ScheduleSnapshot` and the persistence table. The engine
still uses the current dispatch-only loop; the schedule is built but not
yet feeding the cost function. **~2 days.**

- New: `EngineScheduleEntry` JPA entity + Flyway migration
  `V<n>__create_engine_schedule_entry.sql`.
- New: `ScheduleSnapshot` class (in-memory) — keyed by `serieId`, holds
  `(machineNom, plannedStart, plannedEnd, phase)`.
- New: `ScheduleBuilderService` — given a `(zone, sequences, machines)`,
  produces a deterministic `ScheduleSnapshot` using a constructive heuristic
  (greedy machine assignment + EDF queue).
- Optimizer calls `ScheduleBuilderService.build()` after each snapshot
  rebuild; result stored alongside `bestAssignment`.
- New endpoint stub (read-only): `GET /api/dispatcher/engine/schedule`
  returns the current snapshot for debugging.

**Acceptance:** on engine start, every serie has a `(machine, plannedStart,
plannedEnd)` triple. Endpoint returns the data. Existing dispatch behavior
is unchanged.

### Slice 2 — Box-duration in the objective

Add box-duration computation and wire it into the cost function. **~2 days.**

- New: `BoxDurationCalculator` — given a `ScheduleSnapshot` and a sequence's
  series, returns `(max(plannedEnd) − min(plannedStart)) / nb_boxes`.
- Modify `computeSpread()` in the optimizer:
  ```
  cost = wLoad*loadSpread + wBoxDuration*meanBoxDuration
       + wBoxDurationMax*maxBoxDuration + wLateness*latenessPenalty
  ```
- `evaluateMove` now recomputes box-duration for affected sequences only
  (not the full schedule). Memoise per `(sequence, zone)` until next snapshot
  rebuild.

**Acceptance:** engine improvements correlate with reductions in mean
box-duration. The runs panel UI shows a new "Box-Duration" indicator.

### Slice 3 — Level 2 (serie → machine within zone) optimisation

The engine learns to swap serie-to-machine assignments inside a zone, not
just sequence-to-zone. **~3 days.**

- Move set expansion: new `applyMove` variants — `assignSerie(serieId,
  newMachineNom)` and `swapSeries(serieA, serieB)`.
- New iteration step: 50% probability assign Level-1 moves (sequence →
  zone), 50% probability Level-2 moves (serie → machine within zone).
- The constructive heuristic in Slice 1 picks an initial machine; SA at
  Level 2 refines it.
- Constraint: a serie's machine must be in the sequence's dispatched zone
  AND host the serie's machine type AND be active.

**Acceptance:** within-zone MT imbalance ("too much Lectra IP6, Lectra
idle") is observed to drop on test data. Engine adjusts within a few
hundred iterations.

### Slice 4 — Level 3 (per-machine queue order) and box-duration optimisation

The engine optimises the per-machine queue order to minimise box-duration.
**~3 days.**

- Move set expansion: `reorderQueue(machineNom, fromIdx, toIdx)` — moves a
  serie's planned slot earlier/later in its machine's queue.
- The schedule rebuild now uses `DispatchAlgorithms` comparators (wired
  via `cms.dispatcher.ordering.algorithm` property — default `SCG`) to
  seed an initial queue, then SA refines it.
- New move: `sequenceCompaction(sequenceId)` — try to pull all series of a
  sequence into a contiguous block in their respective machine queues.
  This is the direct lever against "boxes pile up".

**Acceptance:** mean box-duration drops between Slice 3 and Slice 4
runs. Sequence affinity (`max(plannedEnd) − min(plannedStart)` per sequence)
visibly tightens.

### Slice 5 — Continuous run + change detection

Engine runs indefinitely, gracefully handling production events. **~2 days.**

- Loop never exits unless `stop()` is called or a critical error occurs.
- `convergenceIterations` becomes a "throttle" — when converged, sleep for
  10s before rechecking (saves CPU when there's nothing to improve).
- New listener: `onSequenceAcceptanceChanged`, `onZoneMachineToggled`,
  `onSerieStatusChanged` → schedule a snapshot rebuild for the next
  iteration.
- New `EngineSchedulerCronService` — every 30s, write the current best
  `ScheduleSnapshot` to `EngineScheduleEntry` rows if it has changed.

**Acceptance:** engine survives a 24h run with simulated mid-run events
(machine toggle, acceptance, new sequence) and recovers each time within
one snapshot-rebuild cycle.

### Slice 6 — Public API + operator UX

The external app can ask "what should this machine work on next?" and get
a fresh answer. The frontend uses WebSocket for live updates. **~2 days.**

- New public endpoint: `GET /api/public/next-series?machine=BB2&limit=3`
  — returns the next N series for a machine from the persisted
  `EngineScheduleEntry` rows. No auth (per user — public surface).
  Includes serie metadata (sequence, partNumberMaterial, est. duration,
  plannedStart).
- Frontend: `ProcessDispatcher.js` subscribes to
  `/topic/dispatcher/engine` via STOMP. Polling demoted to 30s fallback.
- New UI block: per-zone "Box-Duration" panel showing mean/max/best/worst
  sequences with their box-duration trend.

**Acceptance:** `curl /api/public/next-series?machine=BB2&limit=3` returns
fresh data. WS pushes update the heatmap in <500ms after an engine
suggestion event.

---

## 5. Total estimate

| Slice | Effort | Cumulative |
|---|---|---|
| 0 — Quick wins | 1 day | 1 day |
| 1 — Schedule model | 2 days | 3 days |
| 2 — Box-duration objective | 2 days | 5 days |
| 3 — Level 2 moves | 3 days | 8 days |
| 4 — Level 3 moves + compaction | 3 days | 11 days |
| 5 — Continuous run + events | 2 days | 13 days |
| 6 — Public API + frontend | 2 days | 15 days |
| Buffer (testing, tuning, iteration) | 3 days | 18 days |

**Total: ~3.5 working weeks** (down from my original 6-8 estimate — the
scale and the "ScanRouleau-only" simplification cut it nearly in half).

---

## 6. Risks I want to flag now

1. **`EngineScheduleEntry` write storms.** At 1000 series + 30s
   write-cron, that's ~33 row writes/sec. With JPA dirty-checking and
   indexes, this is fine — but we should batch in chunks of 200 and
   `saveAll`, not save individually.

2. **`BoxDurationCalculator` cost per evaluateMove.** Naively recomputing
   for every move makes `evaluateMove` O(sequences × series). At our scale
   that's 100 × 1000 = 100k ops per move × 1000 moves/sec = unrealistic.
   We need an **incremental** delta calculation — only recompute the
   sequences whose series moved. Plan to do this from day 1 of Slice 2.

3. **Material as advisory vs hard constraint.** Current `evaluateMove`
   returns `Double.POSITIVE_INFINITY` for material-MISSING zones. Slice 0
   makes material advisory (a soft cost), which means the engine **can**
   propose a zone with no materials in-zone. That matches your spec
   ("just alert logistics") but it's a behavior change — chefs may need
   to be told.

4. **Public API performance.** `/api/public/next-series` will be polled
   by external apps. We must serve from `EngineScheduleEntry` (the warm
   path), NOT recompute on each request. A separate read-only cache layer
   with 5-10s TTL is needed.

5. **Snapshot rebuild contention.** While Slice 5 makes the engine
   responsive to events, two rapid events (e.g. chef accepts 5 sequences
   in 2 seconds) shouldn't trigger 5 rebuilds. Coalesce: one rebuild per
   loop iteration, regardless of how many events queued.

---

## 7. Where to start — three valid options

This is the only thing I need from you before I write code. Pick one:

**(α) Build it in order, Slice 0 → 6.** Safe, each slice shippable, each
slice de-risks the next. Estimated 3.5 weeks elapsed.

**(β) Skip Slice 0, jump straight into Slice 1 (schedule model).** Saves
~1 day, but `chooseZone` bias and the file-state shim stay broken longer.

**(γ) Slice 0 + Slice 1 + Slice 2 in one bundle, ship them together as
"v1 of Option C with box-duration objective", THEN slices 3-6.** This
gets a measurable KPI in front of operators sooner (~5 days), proves the
direction, then we iterate. **My recommendation.**

---

## 8. Open mini-questions

(Quick answers preferred. None of these block starting.)

1. **Where do I get the box count per sequence?** The code references
   `CuttingRequestBoxInfoRepository.countBoxesBySequences` — is that the
   source of truth, or is there a separate `nbrBoxes` field?

   => you need to do the group by sequence in the entity CuttingRequestBox and see the total for each one

2. **Does a serie's `matelassage` happen on a different machine than its
   `coupe`?** If yes, the schedule needs to model both phases (Slice 1
   already assumes this via `planned_phase ENUM`). If no, we collapse to
   one phase per serie.

   => they are both on the same table but the spreadding happen first then the serie keep waiting it turn to be cut at the end of the line , because at the beggining of the table there is a spreading machine and at the end of the table there is a cutting machine and this for Lectra / Lectra IP6 / Gerber / DIE, but for LASER-DXF and LASER-LSR their process it spread one layer then cut it then next layer then cut it so that the spreading and cutting happen on the same time.

3. **Is there a sequence priority** beyond `dueDate` / `dueShift`? The
   `DispatchContext.getSequenceWeight` is referenced but I haven't seen
   it populated. Default to 1.0 unless you have a source.

  => beyond `dueDate` / `dueShift` we can say that the sequence that already started need also to be finished fast

4. **For the public `/api/public/next-series` endpoint — auth or no
   auth?** External app implies no app-level auth, but maybe a static
   API key header?

   => no need , let it be able to be used by any one 

5. **Naming.** Engine is currently called `ContinuousDispatchOptimizerService`.
   After Option C it'll do scheduling too. Rename to
   `ContinuousProductionOptimizerService`? Or keep the existing name to
   minimise diff noise?

   => you can rename it.

---

## 9. Progress log

Updated 2026-05-14.

| Slice | Status | Notes |
|---|---|---|
| 0 — Quick wins | ✅ done | `chooseZone` tiebreak, `convergenceIterations` wiring, file-state shim removed, `EngineProperties.Optimizer` configurable, `StockStatusClient` replaced with direct service injection, `MaterialAvailabilityChecker` returns `NOT_IN_ZONE` advisory only, event listener for `SequenceAcceptanceChangedEvent`. |
| 1 — Schedule model | ✅ done | `ScheduleSnapshot`, `ScheduleBuilderService`, `EngineScheduleEntry` entity + repo + Flyway `V15_01__engine_schedule_entry.sql`, `GET /api/dispatcher/engine/schedule` debug endpoint. |
| 2 — Box-duration in objective | ✅ done | `BoxDurationCalculator` computes mean/max/worst per snapshot rebuild (`ContinuousDispatchOptimizerService` line ~1043); cost function adds `wBoxMean·mean + wBoxMax·max` (line ~1890); state snapshot exposes `boxDurationMean`/`Max`/`WorstSequence`/`SequencesMeasured`. |
| 3 — Level-2 (serie → machine) moves | ✅ done (feature-flagged) | **2026-05-14**: cost-term plumbing + move kind shipped. `ScheduleSnapshot.mutateSlotMachine(serieId, phase, newMachine, newZone)`. `ContinuousDispatchOptimizerService.tryLevel2Move()` picks a future-only slot, finds an alternative same-MT machine in the same zone, mutates, recomputes `currentIntraZoneSpread`, accepts if cost drops, reverts otherwise. Activate with `mgcms.engine.optimizer.level2-enabled=true` + `mgcms.engine.optimizer.intra-zone-machine-load-weight=0.5`. Move probability: `level2-move-probability` (default 0.10). |
| 4 — Level-3 queue order + compaction | ◐ done (feature-flagged; compaction deferred) | **2026-05-14**: adjacent-swap move kind shipped. `ScheduleSnapshot.swapAdjacentInQueue(machine, lowerIdx)` swaps two consecutive slots' timing — chosen because adjacent swap preserves the queue's total span, so downstream slots are untouched (cheap + revertable). `ContinuousDispatchOptimizerService.tryLevel3Move()` recomputes `currentBoxDurationAggregate` on each attempt. Activate with `mgcms.engine.optimizer.level3-enabled=true` + `mgcms.engine.optimizer.box-duration-weight=2.0`. Move probability: `level3-move-probability` (default 0.10). **Still deferred**: `sequenceCompaction(sequenceId)` — ripples across multiple machines, lands in a follow-up turn. Adjacent-swap is the primary box-duration lever; compaction is an additional optimization for sequence affinity. |
| 5 — Continuous run + change detection | ✅ done | Engine loop runs indefinitely (convergence throttle, not exit). Event listeners (`SequenceAcceptanceChangedEvent`, `ZoneMachineToggledEvent`, `ShiftZoneConfirmedEvent`) coalesced via `AtomicBoolean rebuildRequested`. DB polling fallback via `shouldRebuildSnapshot()` (countChangedSince + maxNserie). 2026-05-14: added manual `requestRebuild()` API + `POST /api/dispatcher/engine/reload` + UI "Recharger" button in `/processWorkbench` header. |
| 6 — Public API + WS push | ✅ done | `PublicRecommendationController` exposes `GET /api/public/next-series?machine=&limit=` reading from `EngineScheduleEntry` rows. `DispatchEngineWebSocketService` pushes to `/topic/dispatcher/engine` on snapshot improvements. |

### MT fairness fix + cell drill-down 2026-05-14

**Problem reported by user.** Engine balanced Lectra well (13.1pp range across zones) but left Lectra IP6 with 105.2pp range. Root cause: `computeLoadSpread()` weighted each MT's spread by its share of total load — so the high-volume MT dominated the gradient and the low-volume MT was effectively invisible to the optimizer.

**Fix.** Added a fairness term `mtFairnessWeight * max(spreadByMT)` to `computeLoadSpread()` so the engine cannot ignore the worst MT. Property defaults to `1.0` — applies immediately on next engine run. Tune via `mgcms.engine.optimizer.mt-fairness-weight=...` (set to 0 to revert to old behavior; higher = more aggressive equalization of MT spreads). Code: `ContinuousDispatchOptimizerService.computeLoadSpread()` line ~2090; property in `EngineProperties.Optimizer`.

**Cell drill-down.** `DispatchHeatmap.js` cells are now clickable. Click any Zone × MT cell to open a drawer below the table listing every sequence dispatched there, sorted by remaining minutes, with each series rendered (`serie`, `statusCoupe`, `tableCoupe`, `remainingMinutes`). No backend round-trip — data is already in `liveCharge` payload that the heatmap consumes. Pure client-side filter; close via the × button.

**Fix to drill-down 2026-05-14**: first version filtered by `liveCharge.zones[zoneNom].sequences` only, which missed cross-zone-routed series (DIE / Gerber / LASER-DXF SHARED spillover — a sequence under Z1 whose Gerber serie routes to Z3's Gerber cell). Switched to walking all zones' sequences and matching on `SerieDto.targetZoneNom` (the backend-resolved final destination per serie). Sequences from a different host zone now show a `via <host>` badge so chefs see the cross-zone routing at a glance.

**Root cause of empty modal (fixed 2026-05-14, 3rd attempt)**: `ZoneChargeDto` (LiveChargeDto.java:85-86) doesn't expose a unified `sequences` array — it splits into `lockedSequences` and `pendingSequences`. The modal was reading `z.sequences` which is `undefined` → empty filter result regardless of routing. Now reads both lists (plus `liveCharge.unassigned[]` for sequences with no home zone) and merges them. The percentage display worked the whole time because it reads `byMachineType` cell aggregates, a separate path that's not affected by the per-sequence split.

**Switched from cell to row click + minutes-per-zone**: cell click had data routing complications and didn't match the user's mental model. Now clicking a **zone row** opens a centered modal listing every sequence whose series route to that zone — including sequences hosted under a different zone (SHARED spillover). For each sequence, the modal shows the **minutes contributed to this zone** (sum of `remainingMinutes` over series with `targetZoneNom === clickedZone`, not the sequence's full total), per-MT breakdown chips, lock status (`ACCEPTÉE` / `VERROUILLÉE` / `LIBRE`) with the locking serie + table when applicable, and the filtered series table. Backend grouping at `LiveChargeService.java:412` puts sequences under their `effectiveZone`, so SHARED zones have empty `sequences[]` in the payload despite carrying load — the client-side walk closes that gap without a wire-format change.

### Full test suite status 2026-05-14

`mvn test` — 107 / 110 pass. The 3 failures are in `services.scheduling.ShiftClockTest` (pre-existing, off-by-one shift mapping unrelated to the dispatcher work). All dispatcher and material-availability tests pass.

### Stale tests — fixed 2026-05-14

All 8 dispatcher tests green. Fixes applied:

- `boxCycleCost_computed_andMaterialConstraintRejectsMove` — assertion flipped from `POSITIVE_INFINITY` to "missing-material zone costs strictly more than fully-stocked"; mocked `countNotInZone()` (the optimizer's actual call, not `check()`); added `getMaterialAlertWeight=0.01` mock so the advisory term is non-zero.
- `continuous_reachesImproving_thenStopsOnDemand` — was actually killed by a `/ by zero` in `iteration % cfg().getSampleEvery()` because the Mockito mock returned 0. Added stubs for all loop modulos (`getSampleEvery`, `getRebalanceEvery`, `getRandomizeAfter`, `getKickAfter`, etc.) plus the SA temperature knobs.
- `fixedDuration_stopsOnTimer_andSavesRunAndSuggestions` — engine batches via `saveAll` now, not single-row `save`. Updated `verify(...).save(...)` to `verify(...).saveAll(...)`.
- `cachesPerSnapshot` — assertion flipped: no longer expects `stockStatusClient.getCurrentStockCount`; now asserts `verifyNoInteractions(stockStatusClient)` + ScanRouleau is read exactly once per `(zone, refTissus)` key.
- Added missing `@Mock` collaborators that were causing NPEs inside the loop: `scheduleBuilderService`, `boxDurationCalculator`, `boxInfoRepository`, `ordonnancementService`, plus stubs returning empty values.

### Side fixes shipped 2026-05-14

- **Coupe-interval overlap normalizer.** `SerieStatusDateValidator.normalizeCoupeOverlaps()` walks each `tableCoupe` ordered by `dateDebutCoupe` and, when an `In progress` serie is followed by a `Complete` serie, sets the first's `dateFinCoupe = second.dateDebutCoupe`. Exposed as `POST /api/dispatcher/engine/normalize-coupe-overlaps` and runs implicitly inside `POST /api/dispatcher/engine/reload` so the "Recharger" UI button repairs data drift before the engine rebuilds.
- **Per-sequence reload hook.** `POST /api/dispatcher/engine/reload-sequence/{sequence}` triggers a rebuild; sequence id is logged but does not narrow the rebuild scope (cheap enough at 100 seq / 1000 series scale to always do a full rebuild).
- **Test compilation fix.** `MaterialAvailabilityCheckerTest` and `ContinuousDispatchOptimizerServiceTest` referenced removed enum values `NEEDS_TRANSFER` / `MISSING`; updated to current `NOT_IN_ZONE`.

### Slice 3 + 4 design blueprint (next session)

Anchored to current code at `ContinuousDispatchOptimizerService.java` (2498 LOC) so a fresh session can hit the ground running.

**Architectural gap to close first.** The cost function today reads `loadByType` on the per-sequence `Candidate` (line 2475). That is a *zone-aggregate* — moving a serie between two machines inside the same zone has **zero effect on it**. So Level-2 moves are silent in the current cost function. Three things must land together for Slice 3 to be meaningful:

1. **Intra-zone machine-load spread term**, weighted by `cfg().getIntraZoneMachineLoadWeight()` (already declared in `EngineProperties.java:82` but never read). Add `computeIntraZoneMachineSpread()` next to `computeSpread()` (~line 1942 area). It should walk `currentSchedule` (already volatile), bucket planned minutes per `(zoneNom, machineNom)`, and compute (max−min) per zone × MT. Add `wIntraZone * intraZoneSpread` to the cost in `computeWeightedCost()` (around line 1880).
2. **Per-serie machine mutability inside the SA loop.** `ScheduleSnapshot.PlannedSlot` is currently rebuilt only via `rebuildSchedule()`. Add a `mutateSlotMachine(serieId, newMachineNom)` method on `ScheduleSnapshot`. Critically: the recompute of `plannedStart`/`plannedEnd` must follow — push the move through `ScheduleBuilderService.reschedule(serieId)` so the queue downstream of the new machine shifts. Keep the old machine's queue valid by also re-numbering its remaining slots.
3. **`SerieRow` already carries `machineType`** (line 2456). Engine has machine→MT and machine→active info via `machinesByZoneByType` already in scope inside `buildSnapshotInternal`. Persist a `Map<String, List<String>> activeMachinesByZoneType` on the service so move-eligibility checks (line ~2160 candidate site) can run in O(1).

**Slice 3 move kinds:**

- `assignSerieToMachine(serieId, newMachineNom)` — single-serie reassignment within the dispatched zone. Eligibility: target machine is active, hosts the serie's `machineType`, and is in the sequence's `currentZone`.
- `swapSeriesBetweenMachines(serieA, serieB)` — only if same zone and each can host the other's MT.

Drop them into the SA inner loop where Level-1 moves live today (around line ~890). The simplest weighting: every iteration roll a coin — 50% pick a Level-1 candidate (existing path), 50% pick a random serie from `snapshotRichSerieRows` and try Level-2. Skip Level-2 entirely when `intraZoneMachineLoadWeight = 0` (don't waste cycles on a cost term that's disabled).

**Pinning policy** (user 2026-05-14): chef-pin blocks Level-1 zone moves only. Level-2 swaps **are allowed** on pinned sequences — chef pinned the zone, not the per-machine choice.

**Slice 4 move kinds:**

- `reorderQueue(machineNom, fromIdx, toIdx)` — mutate the queue order of `currentSchedule` for one machine. Affects `plannedStart`/`plannedEnd` for everything after `min(fromIdx, toIdx)` on that machine. Implementation: `ScheduleSnapshot.reorderQueue()` that re-runs `ScheduleBuilderService.relayoutMachine(machineNom)`.
- `sequenceCompaction(sequenceId)` — pull all of a sequence's series into contiguous slots in their respective machine queues. Hardest move because it touches multiple machines. Implementation hint: greedy — find each serie's slot, then for each, try to swap it earlier in its machine queue until adjacent to another serie of the same sequence. Bounded to one pass per call.

**Incremental box-duration delta** (risk #2 in §6). `BoxDurationCalculator.compute()` currently recomputes the full aggregate (`ContinuousDispatchOptimizerService.java:1043`). For Slice 4 efficiency, add `BoxDurationCalculator.recomputeForSequence(snapshot, sequenceId, boxCounts)` and update `currentBoxDurationAggregate` by replacing only the affected sequence's contribution. Memoise per-sequence min(plannedStart) and max(plannedEnd) in a `Map<String, double[]>` field so per-move delta is O(series in sequence) not O(all series).

**Started-sequence priority** (user 2026-05-14, Q3 of last turn = 2× boost). Add `boolean started` field to `Candidate` (line 2471). Populate from `frozenSequenceIds` semantic (legacy path already passes started sequence ids as frozen). Modify `dueDateWeight(LocalDate)` at line 1929 to `dueDateWeight(Candidate)`, returning `(1 + gain/(1+days)) * (started ? 2.0 : 1.0)`. Update both call sites (lines 1921, 2147). **One caveat**: most started sequences are also frozen (LockResolver promotes them via IMPLICIT_TABLE_STRICT), so the boost only fires in the narrow window between "first non-Waiting serie" and "lock resolver picks up the implicit table lock." Worth doing but expect modest impact.

**Test plan for Slice 3 + 4** (do all four before reporting done):

1. `mvn test -Dtest=ContinuousDispatchOptimizerServiceTest` — existing tests must still pass.
2. New test: `slice3_swapImprovesIntraZoneSpread` — build a 2-machine, same-zone, same-MT fixture loaded 80/20; run 200 iterations; assert intra-zone spread drops.
3. New test: `slice4_compactionReducesBoxDuration` — build a sequence whose 3 series are spread across 3 machine queues with gaps; run sequence compaction once; assert mean box-duration drops by ≥10%.
4. Run engine for 60s against a snapshotted real shift (fixture in `src/test/resources/`); assert cost monotonically decreases over the run (no regression from Slice 3+4 moves).

**Risks specific to Slice 3 + 4:**

- **Slot-time drift on `mutateSlotMachine`**: if the new machine's queue has slots that overlap the moved serie's prior start, we silently push everything later. The next snapshot rebuild will reconcile against DB, but for the duration of one iteration the schedule may show times beyond shift-end. Acceptable but log a warning when it happens.
- **Reorder on an in-progress serie**: any serie with `statusCoupe='In progress'` must be immovable. Add a guard in `assignSerieToMachine` / `reorderQueue` that no-ops if the slot's source serie row has `statusCoupe != 'Waiting'`.
- **Frozen sequence Level-2 moves**: per the pinning decision above, allowed. But if the sequence is `IMPLICIT_TABLE_STRICT`-locked (cutting already started on a specific table), the moved serie's table cannot change. Add a guard: if `LockResolver.lockKind(serieId) == IMPLICIT_TABLE_STRICT`, the serie's machine is frozen.
- **Engine ↔ Ordonnancement coupling**: still independent (the architectural finding from last session). Slice 3+4 produce a `ScheduleSnapshot` that the Ord page can read via `/api/dispatcher/engine/schedule`. They do NOT write `tableCoupe` to the DB; only `publishRun()` does that. Keep it that way.

### Decisions captured (from user 2026-05-14)

1. **Box count per sequence**: `GROUP BY sequence` on `CuttingRequestBox` — already what `CuttingRequestBoxInfoRepository.countBoxesBySequences` does, confirmed.
2. **Matelassage vs coupe machines**: same physical table for `Lectra / Lectra IP6 / Gerber / DIE` (spread first, then cut sequentially at end-of-line). `LASER-DXF / LASER-LSR` interleave layer-by-layer on the same machine. Implication for Slice 4: per-phase planned slots are correct for Lectra/Gerber/DIE families, but LASER series should be modeled as a single combined slot (no separate matelassage phase). To implement when wiring Slice 4.
3. **Sequence priority**: started sequences (any serie with status≠Waiting) get extra weight on top of `dueDate`/`dueShift` so they finish fast. Implement as a small boost in `getSequenceWeight()` before Slice 4 lands.
4. **Public next-series auth**: none — open endpoint.
5. **Engine rename**: approved. Defer the rename until Slices 3+4 land so the diff stays focused.

