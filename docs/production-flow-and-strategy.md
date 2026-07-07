# Production Flow & Strategy — Cutting / Matelassage

*Scope: how the floor actually runs today, what the Logistics Release + Process Workbench changes,
and the path to "give the operator the next 3 best things to work on" per machine. Written as the
narrative spine for a short walkthrough video and as a decision record. Companion to
`process-workbench-review.md` (code review) — this file is about the **process**, not the code.*

---

## 1. The physical process (video narration)

This is the real material + work flow, mapped to the entities that record each step.

```
  ┌──────────────┐   StockStatusReport  ← loaded from the R100.prn report (e.g. C:\R100-MP)
  │  MAGASIN     │   itemNumber · ref · location · qtyOnHand · status · lastUpdated
  │  (MP store)  │   GET /api/stockStatusReport/currentStock?refTissus=…
  └──────────────┘   → warehouse stock; also the location-movement history of each roll
      │  roll is taken from the magasin and placed on a production rack (scanned)
      ▼
  ┌──────────────┐   ScanRouleau (Scan_Rouleau)
  │  ON A RACK   │   serialId · reftissu · lot · metrage · emplacement(=rack) · matricule · date
  │  (on floor)  │   → every roll on the production racks, how much fabric, which rack/zone
  └──────────────┘     (rack → zone via Zone.rollLocations)
      │  logistics carries the roll to the matelassage table for a released sequence
      ▼
  ┌──────────────┐   SerieRouleauTemp
  │  SPREADING   │   tableMatelassage(=PK) · idRouleau · reftissu · lot
  │  on the table│   quantiteInitiale · estimationRest
  └──────────────┘   → roll is being spread horizontally at the start of the table
      │  operators (usually 2) spread the fabric:
      │  pull a layer from table start → spread (matelas) → cut/split → next layer …
      │  repeat until nbrCouche layers are stacked
      ▼
  ┌──────────────┐   matelas is pushed by conveyor to the cutting table;
  │  CUT QUEUE   │   waits in line behind older matelas already queued
  └──────────────┘
      │  CNC cutter cuts the oldest matelas at the head of the line
      ▼
  ┌──────────────┐   boxes are filled with cut pieces;
  │  BOXES /     │   a sequence only RELEASES its boxes when ALL its series are complete.
  │  REMNANT     │   per-serie consumption history → CuttingRequestSerieRouleau / ProdTicket
  └──────────────┘
```

**Remnant mechanic (important):** when only part of a roll is consumed, the roll is **deleted from
`SerieRouleauTemp`** and **re-saved into `ScanRouleau`** — i.e. the remnant goes back onto a rack with
its remaining quantity, ready to be picked again. Nothing is "scrapped" in the normal path.

**Roll lifecycle in four states** (this is the core insight for stock management):

| State | Entity / source | Meaning |
|-------|-----------------|---------|
| In magasin | `StockStatusReport` (from **R100.prn**) | Warehouse stock + the location-history of each roll. `qtyOnHand` per `location`/`ref`. **Preferred truth = read the R100 report directly.** |
| On a rack | `ScanRouleau` | On a production rack, full or remnant, available to pick. `emplacement` = rack → zone via `Zone.rollLocations`. |
| Spreading | `SerieRouleauTemp` | Being spread on the table right now. `estimationRest` = fabric left on it. **Not available** to anyone else. |
| Consumed | `CuttingRequestSerieRouleau` / `ProdTicket` | Per-serie consumption history (what each series actually ate). |

> Current baseline: the Release advisor and Process Workbench use rack stock (`ScanRouleau`) as the
> physically pickable layer, then subtract rolls already on a table (`SerieRouleauTemp`) and released
> allocation ledger rows. R100 / `StockStatusReport` remains the magasin replenishment view, not
> pickable production stock.

---

## 2. Why work stacks up — the four pains, restated mechanically

These are your own observations, translated into the mechanism that causes them. The fix for each
is what the strategy has to target.

1. **Incomplete boxes pile at the end of the zone.** A sequence releases its boxes only when *every*
   series in it is complete. One slow/blocked series freezes the whole sequence's boxes. → We must
   prefer sequences we can **finish**, and avoid opening new sequences while older ones sit half‑done.
2. **Operators wait on logistics for material.** Material is brought late, removed late, or there
   isn't enough. → A release decision must verify material **for the current serie *and* the next
   sequence** before it's handed out, and tell logistics exactly which roll/rack to move.
3. **Idle people / nothing to work on in the zone.** A zone has operators (or a free machine) but no
   ready, material‑backed work *in that zone*. → The engine must keep every active zone fed with the
   next workable item, not just globally balance load.
4. **Heavy‑roll churn.** Small `longueur` × high `nbrCouche` series → operators want to run several
   same‑`refTissus` series back‑to‑back so they don't keep mounting/dismounting heavy rolls. → The
   ordering must **batch by material** within a zone, not interleave fabrics.

The common thread: the old continuous-dispatch optimizer optimized *cutting spread*, but the floor is
bottlenecked on **material flow and sequence completion**. That is exactly the pivot the Logistics
Release tool makes — and it is now the live mechanism: the continuous dispatch optimizer / Ordonnancement
scheduling UI was **shelved 2026-05-31 and its menus removed 2026-06-30**, so nothing on the floor is
driven by an automated schedule anymore.

---

## 3. Does the Logistics Release work help production? — verdict

**Yes, and the foundation is now strong enough to drive operator recommendations.** Honest
assessment:

- ✅ It attacks pains **#2 and #4 head‑on**: it tells logistics *which* sequences to release, checks
  material for the current serie, groups by `refTissus`, and names the roll + rack to move. That
  directly removes "operator waiting for material" and "operator changing heavy rolls".
- ✅ It now commits the release: selected sequences move `suiviplanning.Statu` to `Released`, MG-CMS
  mirrors the lifecycle to `RELEASED`, released roll allocations are written, and a printable picklist
  snapshot is saved. The `suiviplanning` flip lands on a **second, non-XA `cms` datasource**, so
  `LogisticsReleaseService.commit()` **compensates** on any local failure raised synchronously before
  the method returns: it tracks which sequences it actually flipped (`flippedByUs`), and if the MG-CMS
  mirror / allocation / snapshot write fails it calls `compensateSuiviRelease` to revert those rows back
  to `'Non demarre'` (`SuiviPlanningRepository.revertReleasedToNonDemarreBySequences`, guarded to rows
  still at `'Released'`), `setRollbackOnly()`s the primary transaction, and rethrows.
  ⚠️ Two gaps in that guarantee (see `docs/recommendations.md`): (1) no explicit `flush()` is called
  after the primary-datasource writes, so a Hibernate flush-timing failure at the outer `@Transactional`
  commit boundary — after `commit()` already returned — escapes the catch and `compensateSuiviRelease()`
  entirely, producing exactly the `suiviplanning=Released` / local `IMPORTED` split-brain this mechanism
  is meant to prevent; (2) `flippedByUs` is tracked per **sequence**, not per row, so a legitimate revert
  can also wrongly revert rows of a multi-row sequence that this call never flipped.
- ✅ It has a real stock baseline: rack stock minus on-table rolls minus released/advised allocations.
  `materialMissingSuggested` is surfaced in the Release page and `/processWorkbench`, so production
  can see sequences that should be marked material-missing instead of silently opening them.
- ✅ It is **advisory + human‑in‑the‑loop**, which is right given the data‑quality history that made
  the old optimizer unusable. Bad input degrades a *suggestion*, not an automated action.
- ✅ It respects the floor's reality: a serie already started on a STRICT zone **pins** the sequence
  there (`SequenceDto.isLocked`), so the tool never argues with physical commitment.
- ✅ The same per-machine top-3 ranking now feeds every surface: `TableFeedRankingService` serves
  `/tableFeed` and `/processWorkbench`, and `recommendForMachine` serves the CMS-Prod operator screen
  off one contract (see §4.2). This directly attacks **#3 (idle zones)** — every UP-or-in-use table
  gets ranked next-series — and partly **#1 (box stacking)** via the `STARTED`-WIP / due-date tiers.
- ⚠️ It still only *partly* addresses **#1 (box stacking)**: the ranking has no per-series "last few
  series left" completion signal yet, so it leans on the `STARTED` WIP tier + due-date pressure.
- ⚠️ R100 reconciliation is still a replenishment signal, not a hard pick gate. A shortage should say
  "not on rack" versus "not in magasin" when that data is exposed to the operator.

Net: the release + ledger foundation **and** the shared best-3-per-machine ranking are in place. The
remaining work is the completion-signal and FIFO-age refinements to that ranking (§4.2) plus the R100
"on-rack vs in-magasin" split.

---

## 4. The recommended path — a "next 3 best options" engine, grounded

Your instinct ("take the 3 best options, considering racks, boxes, minimal sequence left, keep
people fed, batch by material") is the right target. Here is how to get there without repeating the
old optimizer's mistakes.

### 4.1 Baseline now: make stock *true enough to recommend*

Nothing downstream is trustworthy until "available fabric for material M in zone Z" is real enough.
Use the **rack** layer for what's physically pickable now, with the magasin (R100) as the upstream
replenishment view:

```
availableOnRacks(M, Z) =
      Σ ScanRouleau.metrage          where reftissu=M, emplacement∈Zone.rollLocations(Z)
    − Σ SerieRouleauTemp.estimationRest   already on a table for M in Z
    − Σ allocations released/advised-but-not-yet-consumed   (the ledger)

magasinStock(M) = Σ StockStatusReport.qtyOnHand for ref=M   (from R100 — replenishment, not pickable yet)
```

So a shortage means one of two different things, and the advisor should say which: **(a)** fabric
exists in the magasin but isn't on a rack yet → logistics must bring it down; **(b)** it's not in the
magasin either → real procurement shortage.

The **allocation/reservation ledger** now exists. It stays soft: a suggestion can be overridden, but
double-allocation becomes visible and over-consumption can be audited.

### 4.2 Now: rank the next 3 per machine

This is **built and live** in `TableFeedRankingService.rankForTable` (served to `/processWorkbench`,
`/tableFeed`, and the CMS-Prod operator screen via `recommendForMachine`). It does not rebuild a full
optimizer for the operator: it scores the *feedable* candidates and surfaces the **top 3 per
machine/table**. A transparent score beats an opaque solver here, because operators must trust it and
the input data is imperfect.

**Hard gates** (a candidate is dropped before scoring unless *all* hold):

- `statusCoupe = Waiting`
- `statusMatelassage = Waiting`
- parent `sequenceStatus IN (RELEASED, STARTED)` (so `IMPORTED`, `COMPLETED`, `INCOMPLETE`,
  `MATERIAL_MISSING` are never recommended)
- sequence is **not** `REJECTED` (`zoneAcceptanceStatus`)
- the serie's machine type matches the table's machine type, and the serie routes to that table's zone
- **physical fit**: a serie longer than the table is excluded from the returned top-N entirely (it gets
  a large internal penalty for ordering, then non-fitting candidates are filtered out — an impossible
  mount never surfaces). If every candidate is non-fit the table returns an empty list.

The score is **banded (lexicographic)**: each tier sits in its own decimal band wide enough that no sum
of strictly-lower tiers can overtake a higher tier, so a soft preference can never override a hard one
while the score stays a single comparable number. Tiers, highest first:

| Tier | Signal | Source | Weight |
|------|--------|--------|--------|
| **B** (WIP) | parent sequence already `STARTED` → finish what's open | `sequenceStatus` | +100000 |
| **B** (WIP) | completes a STRICT-table-locked sequence (must-finish-first) | `LockResolver` IMPLICIT_TABLE_STRICT | +100000 |
| **C** (date) | overdue (`dueDate` before today) | sequence due date | +10000 |
| **C** (date) | due today (exclusive with overdue) | sequence due date | +5000 |
| **D** (wait-age) | anti-starvation/FIFO: waiting past 24h gets a fairness floor + a capped per-hour ramp, so a candidate that keeps losing on affinity is eventually rescued (kill-switch `mgcms.nextserie.waitage.enabled`) | release-age proxy (`dispatchedAt` else `createdAt`) via `CuttingRequestRepository.findReleaseProxyBySequences` | +500, ramp ≤ +40 (max +540) |
| **E** (affinity) | same `refTissus` already mounted on the table (no heavy-roll change, #4) | mounted `SerieRouleauTemp` vs `refTissus` | +100 |
| **F** (material) | roll already on a rack in the target zone (#2); soft demotion when the roll is **not** on a rack in the zone | `MaterialAvailabilityChecker` AVAILABLE_IN_ZONE | +10 / −5 |
| **G** (keep busy) | tiny bonus ∝ validated cut minutes, capped so it can never reach Tier F (#3) | `0.01 × min(validatedMinutes, 100)` | ≤ +1 |

Ties break on earliest `dueDate` (nulls last), then **longer** validated minutes (mount the longer cut
so the cutter stays loaded). The STRICT pin is a hard constraint resolved by `LockResolver`, not a
score: a pinned sequence's zone is fixed and "completes-a-locked-sequence" is the WIP boost above.
Note pain **#1 (stuck-box completion)** is currently approximated by the `STARTED` WIP tier + due-date
pressure; there is no per-series "last few series left" signal yet, but the Tier D wait-age/FIFO term
above (commit f1f407d, 2026-06-30) now rescues a candidate that keeps losing on affinity even before
its due date is close.

Output per machine: *"Next: do these 3 — they are Waiting/Waiting, already released or started,
material-backed, fit the table, and ordered for it."* That is the operator-facing artifact that changes
behavior in CMS-Prod and in `/processWorkbench`.

### 4.3 Finally: close the loop (Phase 3)

- **Verify in next production**: compare actual per-serie consumption (`CuttingRequestSerieRouleau` /
  `ProdTicket` history, plus live `SerieRouleauTemp.estimationRest` deltas) against what was allocated.
- **Alert on overrun**: when production consumes more than the released amount (real, due to defects,
  re‑cuts, the variance you mentioned), flag it — that's where theoretical-vs-real material divergence
  becomes a stockout, and catching it early is the payoff of the ledger.

### 4.4 What *not* to do

- Don't resurrect the continuous dispatch optimizer as the driver. The floor is ~50% anchored and the
  input data isn't good enough to trust an automated schedule. Its scheduling/Ordonnancement UI was
  removed (2026-06-30, commit 834568d); the `/api/scheduling/**` `SchedulingController` +
  `SchedulingOptimizationService` (`optimized_plan` / `optimized_series_assignment`) are now **orphaned
  legacy** with no UI. (`OrdonnancementService` survives only as a live cache-invalidation /
  `SerieDTO` helper — not a scheduler.)
- Don't make allocations hard locks in Phase 2. Soft + visible first; harden only if double‑pick
  actually happens in practice.
- Don't let the engine override a STRICT‑started sequence's zone. Physical commitment wins.

---

## 5. Open items (carried)

- **Menu gate**: Dashboard "Logistique" section shows only for `ROLE_ADMIN` / `ROLE_VARIANCE`; the
  `/logisticsRelease` route also allows `ROLE_VALID_QN_LOGISTIQUE`. A pure logistics user can reach
  the page by URL but won't see the link. Decide whether to widen the section gate.
- **Workbench cleanup**: `/processWorkbench` should become an action board, not a control room. Keep
  the best-3-per-machine panel first, keep sequence/material focus second, and move surplus engine
  controls out of the operator path.
- **R100 signal**: expose "not on rack but exists in magasin" separately from real shortage.
- **Close the loop**: compare actual consumption against released allocations and flag overruns.

---

*Last updated: 2026-06-30. Release commit (with non-XA suiviplanning compensation), picklist snapshot,
allocation ledger, true rack stock, `materialMissingSuggested`, and CMS-Prod STARTED auth are in place.
The shared best-3-per-machine ranking (`TableFeedRankingService`, banded lexicographic — §4.2) now feeds
`/processWorkbench`, `/tableFeed`, and the CMS-Prod operator screen off one contract. The continuous
dispatch optimizer / Ordonnancement scheduling UI was shelved 2026-05-31 and its menus removed
2026-06-30 (commit 834568d); `/api/scheduling/**` is orphaned legacy. Remaining: ranking
completion/FIFO-age refinements and the R100 on-rack-vs-in-magasin split.*
