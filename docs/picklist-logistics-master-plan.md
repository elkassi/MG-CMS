# Picklist & Logistics — Master Plan

*Scope: the improved logistics Picklist (replacing the old `suiviplanning` app), the CuttingRequest
status lifecycle, per-zone material intelligence (3 transfer/return rules), feeding the matelassage
so the cutter never starves, plus two floor apps — scanCoupe (Zebra) and CMS-Prod ("Rest"). Built
from a 6-agent parallel investigation, 2026-06-01. Companion to `production-flow-and-strategy.md`.*

---

## 0. Point of view — the three things that matter most

1. **The allocation/reservation ledger is the keystone — and it is now built.**
   The ledger shipped as `logistics_allocation` (entity `domain/logistics/LogisticsAllocation.java`,
   write side `services/logistics/AllocationService.java`, migration `V16_02`). RULE 1, RULE 2, the
   picklist commit, and the over-consumption alert all share it: a persistent record of
   "roll → serie → zone, allocated" that decrements available stock.
   `AllocationService.reservedMetersByMaterialZone()` (summed over `ADVISED`+`RELEASED`) is the
   deduction `LogisticsReleaseService.buildStockIndex()` now subtracts from rack availability
   (`LogisticsReleaseService.java:1719`), so two zones are no longer told to use the same roll. The
   over-consumption alert (WS6) still consumes it as the baseline — that workstream remains future.

2. **This is an integration system over shared SQL Server tables, not one app.** The plan touches
   four apps that read/write the *same rows*:
   - `Scan_Rouleau` (racks) is written by **scanCoupe** and read by **MG-CMS** + **CMS-Prod**.
   - `suiviplanning.Statu` is currently written by an **external CMS desktop app** and only *read*
     (`GammeCMS.js:1483`) and *reverted* (`QueryService.initialiserBySequenceCMS`, `:672`) by MG-CMS.
   - `StockStatusReport` is the magasin truth, loaded from the **R100.prn** report.
   The risky parts of this plan are these seams (who owns a write, two-datasource consistency), not
   the algorithms.

3. **Don't extend the dead optimizer; add small, stateless services beside the live data.** The
   `ContinuousDispatchOptimizerService` optimizes the wrong objective (load-spread simulated
   annealing), is effectively idle in prod, and is full of Phase-8 stubs. ~9 of the 10 signals a
   "feed-the-table" recommender needs already exist as queryable fields. The new value is a thin
   scorer, not a solver.

---

## 1. Foundation A — CuttingRequest status lifecycle (parallel column) — **IMPLEMENTED**

**Goal:** `IMPORTED → RELEASED(+fixed zone) → STARTED → {COMPLETED | MATERIAL_MISSING | INCOMPLETE}`,
and sync `suiviplanning.Statu` on release.

**Shipped:** the lifecycle lives on `CuttingRequest.sequenceStatus` (enum `SequenceStatus`, the
parallel-column recommendation below was taken). Migration `V16_01__cutting_request_status_lifecycle.sql`
formalizes the lifecycle column; `V17_02__cutting_request_release_zone_source.sql` adds the persisted
release zone + source. The release transition is performed by `SequenceStatusService.transition(...)`
(called from `LogisticsReleaseService.commit`). The `suiviplanning.Statu` ↔ `sequenceStatus` sync runs
as the 20-min scheduled job (`SuiviPlanningStatusSyncService`). The historical design notes below are
kept for context — read them as "why", not "to do".

**What exists:** `CuttingRequest.sequenceStatus` (ACTIVE/PAUSED/WAITING_MATERIAL/COMPLETED/null) drives
the dispatcher/engine via the one chokepoint `CuttingRequestRepository.findActiveDueOnOrBeforeLight`
(ACTIVE/null + due window). There is **no persisted release/effective zone** — the STRICT pin is
computed live in `LockResolver` / `LiveChargeService` (precedence: lock > engine > `dispatchedZone` >
preferred `zone.nom`). Zone identity is `Zone.nom` (string).

**Decision (recommended): add a parallel column, do not replace `sequenceStatus`.** Replacing it has a
large blast radius (every hardcoded `'ACTIVE'/'COMPLETED'` filter, 3 auto-COMPLETED writers, the active
index, full row backfill) and loses `PAUSED`. The "RELEASED with zone fixed" requirement needs a new
persisted zone column *regardless*. So:

- **Flyway migration** (idempotent, mirror `V2_02` guards; camelCase names per the no-snake_case
  naming strategy):
  - `CuttingRequest.picklistStatus VARCHAR(24) NULL` — the new lifecycle.
  - `CuttingRequest.releaseZone VARCHAR(64) NULL` — the zone fixed at release (zone-name string,
    consistent with `dispatchedZone`).
  - Optionally formalize `sequenceStatus` in Flyway (it was added out-of-band; only the index
    references it today).
  - **Backfill** `picklistStatus` by joining `suiviplanning.Statu`: `Non demarre→IMPORTED`,
    `Released→RELEASED`, `En cours→STARTED`, `Complet→COMPLETED` (fallback COMPLETED where
    `sequenceStatus='COMPLETED'`).
- **`releaseZone` precedence:** decide whether, once set, it *wins* over the live
  `LockResolver`/`dispatchedZone` precedence in `LiveChargeService` (it should — release is a human
  commitment). Insert it just above `dispatchedZone` in the precedence chain.
- **Workbench/engine filter:** the new lifecycle gate belongs in
  `CuttingRequestRepository.findActiveDueOnOrBeforeLight` (and the `WorkbenchCacheService` callers).
  Decide whether `IMPORTED` (not-yet-released) rows are excluded from /processWorkbench + the engine.

**Key files:** `domain/CuttingRequest/CuttingRequest.java` (+ the write-twin `CuttingRequestData.java`),
`repositories/CuttingRequest/CuttingRequestRepository.java:71-84`,
`services/dispatcher/LiveChargeService.java:270-289`, `services/dispatcher/LockResolver.java`,
`services/dispatcher/WorkbenchCacheService.java:253,645`, new `db/migration/V<n>__cutting_request_picklist.sql`.

---

## 2. Foundation B — Allocation / reservation ledger — **IMPLEMENTED**

**Goal:** a persistent, soft, advisory record that makes stock *true* and is the baseline for
over-consumption alerts.

**Shipped (primary DB, Flyway `V16_02__logistics_allocation.sql`):** table `logistics_allocation` —
one row per advised roll: `id, sequence, serie, refTissus, serialId, sourceRack, sourceZone,
targetZone, allocatedMeters, status(ADVISED|RELEASED|CONSUMED|RETURNED|CANCELLED), picklistId,
createdAt, createdBy, updatedAt` (entity `domain/logistics/LogisticsAllocation.java`). Soft/advisory:
it records intent, it does not lock physical stock. `AllocationService` is the write side —
`reserve(...)` (defaults to `ADVISED`, written as `RELEASED` at picklist commit), `markConsumed`,
`markReturned`, `cancel`, and the read `reservedMetersByMaterialZone()` (summed over
`ADVISED`+`RELEASED`). `LogisticsReleaseService.buildStockIndex()` now subtracts BOTH that reservation
total (`applyReservations(...)`, `:1719`) AND the on-table `SerieRouleauTemp.estimationRest`, so the
advisor's `StockIndex` is TRUE stock (open decision #7 is resolved: yes, subtract on-table rolls).
Availability is:

```
availableOnRacks(M, Z) = Σ ScanRouleau.metrage(M, rack∈Zone.rollLocations(Z))
                       − Σ SerieRouleauTemp.estimationRest(M, Z)        // on a table now
                       − Σ logistics_allocation.allocatedMeters(M, Z, status∈{ADVISED,RELEASED})
magasinStock(M)        = Σ StockStatusReport.qtyOnHand(M)               // R100, replenishment only
```

A shortage then resolves to **(a)** "in magasin, not on a rack → logistics bring it down" vs **(b)**
"not in magasin → procurement". This table is written at picklist commit and updated as series consume.

---

## 3. Workstream 1 — The Picklist (select → release → print) — **IMPLEMENTED**

**What shipped:** `LogisticsReleaseService` now both advises AND commits.
`build(date, shift)` (`GET /api/logistics/release/candidates`) returns
zones/materials/sequences/series/rollPlacements/transferAfterUse/fillPlan/returnToMagasin/totals over
the IMPORTED (not-yet-released) candidates, loaded directly via
`CuttingRequestRepository.findImportedDueOnOrBeforeLight` (the workbench/liveCharge feed deliberately
excludes IMPORTED). The screen `/logisticsRelease` lives under the Dashboard "Logistique" section.
`suiviplanning` is bound to **`cmsDataSource`** (legacy CMS DB), separate from the primary DB, and
MG-CMS is now a Java writer of `Non demarre → Released` (it no longer relies only on the external app).

**Staged page load** (perf): the controller exposes three GETs the UI calls in sequence — the raw
sequence list `GET /candidates/sequences` (`sequencesPreview`, no stock/advice, instant), the full
advice `GET /candidates` (`build`), and per-material magasin (R100) lookup
`GET /candidates/magasin?materials=…` (`magasinDetailBatch`).

**Recap gate** `POST /api/logistics/release/recap` (`recap(sequences)`): per normalized material it
computes `newMeters` (Σ longueur×nbrCouche over the selected not-Complete series), `committedMeters`
(Waiting series of RELEASED/STARTED), `availableMeters` (RAW rack metrage + on-table estimate — NOT
the deducted advisor StockIndex), and `remainingMeters = available − committed − new`. A deficit is
checked against the magasin (`StockStatusReportService.getCurrentStock`) and resolves to `OK` /
`COVERED` / `SHORTAGE`; `canConfirm` is true only when no material is in SHORTAGE. (`GET /recap/magasin`
gives the per-material magasin drill-down.)

**Commit endpoint** `POST /api/logistics/release/commit` (`commit(date, shift, sequences, createdBy)`,
`@Transactional`) does, in order: (1) **stale-UI guard** — recompute `build(date, shift)` and reject
any selected sequence that is no longer a releasable candidate, or whose advice has
`materialMissingSuggested` (the material-insufficient block); (2) **flip** `suiviplanning` to
`Released` via `SuiviPlanningRepository.releaseNonDemarreBySequences` (guarded, only `Non demarre`
rows), then re-read to confirm none stayed at `Non demarre`; (3) **mirror locally** — for each
sequence `SequenceStatusService.transition(seq, SequenceStatus.RELEASED, suggestedZone)` sets the
lifecycle + fixed `releaseZone`; (4) **ledger** — `allocationService.cancel(seq)` then
`allocationService.reserve(buildAllocations(...))` writes `logistics_allocation` rows at status
`RELEASED` (`picklistId` stamped); (5) **snapshot** — persist a `LogisticsPicklist` row (id
`PL-yyyyMMdd-HHmmss-<uuid8>`, `snapshotJson` of the committed sequences + transfer/fill/return +
`magasinPull`) so a reprint is stable (entity `domain/logistics/LogisticsPicklist.java`, migration
`V16_03__logistics_picklist.sql`).

**Failure compensation (the two-datasource seam).** `cmsDataSource` and the primary datasource are
**not** XA-bound: the `suiviplanning` flip commits the instant `releaseNonDemarreBySequences` returns,
before any local write. The commit captures which sequences it actually flipped (`flippedByUs` = rows
that were `Non demarre` before the flip — so a sequence already `Released` by the external app is never
reverted by us), and on **any** later failure calls `compensateSuiviRelease(flippedByUs, …)`
(`SuiviPlanningRepository.revertReleasedToNonDemarreBySequences`, guarded to rows still at `Released`)
to put exactly those rows back to `Non demarre`. Three guarded paths: the post-flip not-fully-released
check (`notReleased`); a local-mirror transition failure (also `setRollbackOnly()` so the primary side
undoes any `sequenceStatus=RELEASED` it already wrote — avoiding the inverse split-brain); and the
catch-all `RuntimeException` around the ledger/snapshot writes (revert + rethrow so the primary
`@Transactional` rolls back). Every partial-failure path logs the affected sequences with
`log.error` for ops reconciliation. A failed revert is itself logged, never allowed to mask the
original error.

**Printable picklist** (client-side): the stable snapshot drives the print. NB the app-wide blank-print
regression (global `@media print{body *{visibility:hidden}}` leaking via react-to-print) was fixed by
scoping print CSS to a body class — see the gamme-print-rotation memo; keep any new print CSS scoped.

---

## 4. Workstream 2 — Per-zone material intelligence (the 3 rules)

**What exists:** the advisor's `StockIndex` now starts from ScanRouleau rack metrage and **deducts**
the allocation ledger (`applyReservations`) and on-table `SerieRouleauTemp.estimationRest` (TRUE stock),
exposing `meters(M,Z)`, `totalMeters(M)`, `allRolls(M)`; `DemandIndex` (cross-zone need per material);
`WorkbenchSequenceFocusService` emits `RETURN_TO_STOCK` candidates + `transferOptions`. Need =
`longueur × nbrCouche`. `retourMagasin` field already exists on the serie.

All three rules are now emitted by `LogisticsReleaseService.build()` over the deducted (TRUE) StockIndex
and ride in the advice payload as `transferAfterUse` / `fillPlan` / `returnToMagasin`:

- **RULE 1 (roll bigger than all-zone need → transfer after use) — IMPLEMENTED:** `buildTransferAfterUse`
  (`:1364`) — per material, `totalNeed = Σ_zones need`; a roll with `meters > totalNeed` is a "shared
  roll" → emit a transfer-after-use step from the earliest-due zone to the next needing zone (only when
  ≥2 zones need it).
- **RULE 2 (insufficient → fill a zone with the smallest sufficient rolls) — IMPLEMENTED:** `buildFillPlan`
  (`:1422`) fires only on a real shortage (`total < needed`); it walks zones by priority and decrements a
  largest-first roll pool per zone, so a scarce total is no longer double-counted across competing zones.
  This relies on the now-built ledger (Foundation B). *Still open (semantics):* single-zone fill vs
  fair-share; rolls atomic vs splittable.
- **RULE 3 (rack roll no serie needs, even IMPORTED → return to magasin) — IMPLEMENTED:** `buildReturnToMagasin`
  (`:1473`) builds the broad demand set from `CuttingRequestRepository.findInProductionMaterials()`
  (distinct in-production materials, no due filter); any rack roll whose material is not in that set is
  flagged for return.

**Key files:** `services/logistics/LogisticsReleaseService.java` (StockIndex/DemandIndex/pickRolls),
`services/.../WorkbenchSequenceFocusService.java` (returnCandidates/transferOptions), `domain/Zone.java`,
`repositories/ScanRouleauRepository.java`. Note: `StockIndex`/zone-resolver are **duplicated** across
the two services — extract a shared component when touching both. The `zoneForLocation` substring
fallback is fragile (one rack name a substring of another) — tighten before moving physical rolls on it.

---

## 5. Workstream 3 — Feed the matelassage (table never empty)

**Goal:** per zone/table about to go idle, recommend the next serie to mount so the downstream cutter
never starves.

**What exists:** `SerieRouleauTemp` (one row per table = what's mounted now — the live occupancy
probe), `MachineQueue` (FIFO next-3 per machine — but `saveQueues` is **manual-only → stale**),
`OrdonnancementService` schedule passes, `ActiveMachineResolver` (machines up per zone+shift),
`ProductionTable` (table roster + `tableLength`), `MaterialAvailabilityChecker`, `LockResolver`,
`CuttingTimeCalculator`. ~9/10 ranking signals exist.

**Build:** a fresh stateless `TableFeedRankingService` (request-scoped, cached like `LiveChargeService`)
that, per up-table in an active zone, computes **time-to-idle** (the one missing trigger — from
`SerieRouleauTemp.date + estimationRest` and/or `MachineQueue.estimatedEndTime`) and ranks candidate
series by: ready-vs-waiting, **same-`reftissu` already mounted** (the other missing signal — avoids
heavy roll changes), material-on-rack-in-zone, due date, completes-a-locked-sequence, table-length fit,
zone headroom. Reuse `OrdonnancementService` loaders + `CuttingTimeCalculator` + `MaterialAvailabilityChecker`
+ `LockResolver` unchanged. **Do not** build on the optimizer.

**Open:** trigger granularity (table — recommended — vs zone); time-to-idle source (live
`SerieRouleauTemp` vs fixing `MachineQueue` staleness with a `@Scheduled saveQueues`); affinity weight;
respect locks (feed only `effectiveZone==Z`, must-finish IMPLICIT_TABLE_STRICT first).

---

## 6. Workstream 4 — scanCoupe (Zebra MC3300)

**Reality:** Spring Boot + **Thymeleaf** (the React stack in `package.json` is dead/unused). Same
`Scan_Rouleau` table in the same DB as MG-CMS → **deletes here change MG-CMS logistics stock.** Save &
delete already write `Scan_RouleauHistorique` audit. No list-by-rack, no delete-all-in-rack today
(only a hidden `ADELETE/BDELETE/CDELETE` scan trick). `emplacement` = rack (A/B/C prefix).

**Build (stay Thymeleaf, no React, no migration — `ddl-auto=update`):**
- `RouleauRepository.findByEmplacement...` + `RouleauService.findByEmplacement` /
  `deleteByEmplacement` (loop through the existing `delete(serialId)` so **each** delete keeps its
  history row — no bulk SQL that skips audit).
- Render a running `rackRolls` list under the form (newest on top) after each save = "under the last
  scanned" + current-rack view.
- New POST actions `DeleteOne` / `DeleteRack` (with a `confirm()` — it removes shared stock) /
  `ViewRack` (current or any rack).

**Watch:** `emplacement` is saved as-typed but validated uppercase → normalize-on-save or use
`IgnoreCase` queries, else list/delete-all miss mixed-case rows. Consider whether "delete all" should
be a soft-delete given the shared-stock blast radius.

**Key files:** `D:\work\LEAR\scanCoupe\...\HomeController.java`, `...\RouleauService.java`,
`...\RouleauRepository.java`, `...\templates\index.html`.

---

## 7. Workstream 5 — CMS-Prod "Rest" filter

**Reality:** CRA React 17, `Form.js` (8k lines, `/formMatelassage`, spreading station). The "Rest"
button (`Form.js:6681`) opens a modal of 4–5 stacked tables, all already scoped to the current
material (`obj.partNumberMaterial`), merged from ~6 axios GETs. **No search/filter/rack selector** —
that's the "unfiltered dump" to fix.

**Build (pure front-end, no backend, no new deps):** add `restFilterText` / `restFilterRack` /
`restFilterMineOnly` state; a filter bar under the "Rest rouleau matiere" heading (Bootstrap input +
`react-select` rack dropdown + a "Ma série" toggle, default ON when the current serie has leftover
rolls); extend the existing `.filter(...)` predicate. Optionally mirror the text filter into "En
cours" and "Scan Rouleau" tables.

**Open:** rack granularity (full `location` vs zone prefix `T0R`/`T0QH`/…); which location field is
authoritative; whether the cutting station (`FormCoupeNew.js`) needs the same.

---

## 8. Sequencing & dependencies

```
Foundation A (status cols + migration) ─┐  [DONE]
Foundation B (allocation ledger)        ─┴─► WS1 Picklist (select/release/print)   [DONE]
                                            └─► WS2 Stock rules 1/2/3              [DONE]
WS3 Feed-the-table  ── mostly independent (needs the live SerieRouleauTemp probe)  [future]
WS4 scanCoupe   ── independent quick win (no deps)                                 [future]
WS5 CMS-Prod Rest ── independent quick win (no deps, front-end only)              [future]
WS6 Verification / over-consumption alerts ── needs Foundation B + consumption history  [future]
```

Status: **Foundations A+B, WS1 (picklist select/recap/release/snapshot) and WS2 (rules 1/2/3) have
shipped.** Remaining: WS3 (feed-the-table), WS4 (scanCoupe rack list/delete), WS5 (CMS-Prod Rest
filter), WS6 (over-consumption alerts off the now-built ledger).

---

## 9. Consolidated open decisions

*Decisions 1–4, 6, 7 are RESOLVED by the shipped WS1/Foundations build; struck through with the choice
taken. 5 and 8–10 remain genuinely open.*

1. ~~**Status model:**~~ RESOLVED — the lifecycle rides on `CuttingRequest.sequenceStatus`
   (IMPORTED→RELEASED→STARTED→COMPLETED + MATERIAL_MISSING/INCOMPLETE) with a persisted `releaseZone`
   (migrations `V16_01`, `V17_02`).
2. ~~**`Released` ownership:**~~ RESOLVED — MG-CMS now writes `Non demarre→Released` itself in
   `commit()`, with non-XA compensation if a later step fails; the external CMS app can still race and
   that is handled idempotently (it is never reverted by us).
3. ~~**`releaseZone` value & precedence:**~~ RESOLVED — the human's confirmed zone is persisted at
   release via `SequenceStatusService.transition(seq, RELEASED, zone)`.
4. ~~**Workbench/engine sees IMPORTED?**~~ RESOLVED — IMPORTED is excluded from the workbench/liveCharge
   feed; the picklist loads candidates directly via `findImportedDueOnOrBeforeLight`.
5. **RULE 2 semantics:** single-zone smallest-sufficient fill (shipped: largest-first pool, decremented
   per zone) vs fair-share; rolls atomic vs splittable. *Still open.*
6. ~~**Demand horizon for RULE 3:**~~ RESOLVED — `buildReturnToMagasin` uses
   `findInProductionMaterials()` (all in-production materials, no due filter).
7. ~~**Subtract `SerieRouleauTemp`** (on-table) from rack availability~~ RESOLVED — yes; the advisor
   StockIndex deducts both the ledger and on-table `estimationRest`.
8. **Feed-the-table trigger:** table-level (recommended) granularity; live `SerieRouleauTemp` vs fixing
   `MachineQueue` staleness (`@Scheduled saveQueues`). *Still open (WS3 not built).*
9. **scanCoupe deletes:** hard vs soft delete given shared `Scan_Rouleau`; emplacement case normalization.
   *Still open (WS4 not built).*
10. **CMS-Prod:** rack-prefix vs full-location filter; matelassage-only or cutting too. *Still open (WS5).*

---

## 10. Verification strategy (per CLAUDE.md risk areas: logistics, status, cross-layer)

- **Status/migration:** Flyway up on a copy; assert backfill mapping; existing dispatcher/workbench
  tests still green; `sequenceStatus` paths untouched.
- **Picklist commit:** integration test of the two-datasource write (suiviplanning Released + local
  mirror + ledger rows + snapshot); the stale-UI guard rejects non-candidates; the recap `canConfirm`
  gate and the `materialMissingSuggested` commit-block; idempotency (re-commit / external-app race is a
  no-op, `flippedByUs` never reverts an externally-released row); and the compensation paths — a forced
  local-mirror failure must revert exactly the rows this call flipped (`revertReleasedToNonDemarreBySequences`)
  AND `setRollbackOnly()` undoes any local `RELEASED` writes (no split-brain).
- **Stock rules:** unit tests on `StockIndex`/allocator with crafted rolls for each of RULE 1/2/3,
  including the double-counting case the ledger fixes.
- **Feed-the-table:** unit tests on the ranker (idle-soonest, same-reftissu affinity, lock respect).
- **scanCoupe / CMS-Prod:** manual floor steps + the shared-stock blast-radius check (a scanCoupe
  delete reflects in MG-CMS availability); CMS-Prod is a front-end-only build validation + Jest.

---

*Last updated: 2026-06-30. Foundations A+B, WS1 (picklist select/recap/release/snapshot +
non-XA compensation) and WS2 (stock rules 1/2/3) have shipped. Next gate: WS3 (feed-the-table)
trigger-source decision, and WS4/WS5/WS6.*
