# Dispatcher Engine & `/processDispatcher` — Production-Readiness Review

Author: Claude (Opus 4.7) — independent walkthrough of the engine code, not a
rephrase of Kimi's note.
Scope: `ContinuousDispatchOptimizerService`, `SequenceDispatcherService`,
`SerieZoneResolver`, `EngineTickService`, the three `SeriesOrderingStrategy`
implementations, `OrdonnancementService.getNextSeriesForMachine`,
`LiveChargeService`, `LockResolver`, `MaterialAvailabilityChecker`,
`DispatchEngineController`, and the React `ProcessDispatcher` /
`DispatchEngineControl` components.

---

## 1. Bottom line

The architecture is sound and the **zone-dispatching layer is genuinely good**
— it has thoughtful constraints (material availability, locked sequences,
per-MT load splitting, SHARED-vs-STRICT routing). I'd ship it for limited
production with monitoring.

The **engine's "ordonnancement" phase is misleading**. It's not real
machine-level scheduling — it's a second dispatch pass with a due-date bias.
The actual machine-level ordering exists in two places that **don't talk to
each other**:

1. `BoxDurOptimizedOrderingStrategy` (used by
   `OrdonnancementService.getNextSeriesForMachine`) — sorts series inside a
   machine queue using a hand-rolled "longest-cut-first" heuristic.
2. `DispatchAlgorithms` (SCG / SPT / LPT / EDF / CR / WSPT / ATC /
   MATERIAL_GROUP) — a richer comparator library that is **not wired into the
   ordering strategy at all**.

So you have a sophisticated dispatcher, a placeholder "ordonnancement" phase,
and a parked algorithm library — three components that should be one
pipeline.

**My verdict:** ship the dispatcher behind a feature flag, **rename or
disable** the ordonnancement phase until it does real per-machine scheduling,
and wire `DispatchAlgorithms` into the `SeriesOrderingStrategy` so the
in-zone ordering has the algorithms it claims to have.

---

## 2. How dispatching actually works today (my read, not Kimi's)

### 2.1 Sequence → zone (the dispatcher)

`SequenceDispatcherService.compute()` →

1. Light projection of every `CuttingRequest` for `(date, shift)` (or all
   active sequences via `computeActive()`).
2. For each sequence, `SchedulableSerieFilter` resolves each serie's zone via
   `SerieZoneResolver` — STRICT-first, SHARED-fallback, gated by
   `ActiveMachineResolver` (chef-confirmed up-machines).
3. `chooseZone()` picks the request's home zone: most series wins; ties
   prefer STRICT then `b.getKey().compareTo(a.getKey())` (reverse-alpha —
   "Zone-Z" wins over "Zone-A", contrary to Kimi's note, but the systematic
   bias still exists).
4. `publish()` writes `dispatched_zone` + `zone_acceptance_status='PENDING'`,
   fires `SequenceAcceptedEvent` per zone, and writes a `DispatchAudit` row
   per move.

Strong points (verified):

- Projection queries (`findAllLight`, `findSeriesByDateShiftLight`) avoid
  N+1 entity hydration.
- `SerieZoneResolver` has TTL caches on three lookups (zonesByType,
  zoneByNom, machinesByZone) at 30s — sensible.
- `EngineTickService.autoDispatchTick` runs every 5 minutes, but fingerprint
  diffs the preview so no-op publishes don't bump kiosk versions.
- `LockResolver` correctly favours physical reality (a serie cut on a
  STRICT-zone table) over a stale `dispatched_zone`.
- `MaterialAvailabilityChecker` is a hard constraint, cached per snapshot —
  good.

### 2.2 The engine (`ContinuousDispatchOptimizerService`)

It's a 2,363-line single class. The hot path is:

```
buildSnapshot → greedyWarmStart → loadBestStateFromFile
   ↓
loop:
  doLoopIteration
    ├─ phase switch (DISPATCH 20s / ORDONNANCEMENT 40s)
    ├─ cool temperature (SA, multiplier 0.9995/iter)
    ├─ periodic rebalance (every 40 iters: most-overloaded → least-loaded)
    ├─ periodic randomize (after 120 stagnant iters: randomize 12% of candidates)
    └─ either doOrdonnancementIteration() or doOneIteration()
                                                  ├─ phase 1: greedy improving
                                                  ├─ phase 2: SA accept-worse
                                                  ├─ phase 3: 2-swap
                                                  └─ phase 4: 3-rotate
```

The objective is:

```
cost = w1 * avgBoxCycleTime + w2 * maxBoxCycleTime
     + w3 * loadSpread       (default: w3 = 1)
     + w4 * latenessPenalty  (default: w4 = 0)
```

with `w1 = w2 = 0` defaults (`EngineProperties.Optimizer`). So in the
out-of-the-box config the engine optimises **load spread only**, weighted
per machine type. The `boxCycleTime` is a lower-bound estimate (the longest
single serie's minutes) because the engine has no schedule — confirmed by
the comment at `ContinuousDispatchOptimizerService.java:1041-1044`.

### 2.3 The "ordonnancement" phase, exactly as implemented

```java
// doOrdonnancementIteration() — ContinuousDispatchOptimizerService:672-763
sorted = candidates sorted by (dueDate asc, then boxCycleTime desc)
for top 8 sorted candidates with possibleZones.size() >= 2:
    pick the zone that minimises evaluateMove(cand, zone)
    if it differs from currentZone, applyMove and publish suggestion
```

This is exactly the same `evaluateMove` used by the dispatch phase. The only
difference is the candidate selection (top 8 by due date instead of random
probe). It is not ordonnancement, it is **dispatch with a different sample
strategy**. No machine assignment, no timeline, no real cycle-time
optimisation.

### 2.4 In-machine ordering (the part operators actually see)

`OrdonnancementService.getNextSeriesForMachine` builds the per-machine queue:

1. In-progress series on this machine — added as-is.
2. Ready-to-cut series (`statusMatelassage == Complete`) — sorted by
   `seriesOrderingStrategy.sortReadyToCut`.
3. Waiting series — sorted by `seriesOrderingStrategy.sortWaiting`.

`SeriesOrderingStrategy` is `@Primary` `DelegatingSeriesOrderingStrategy` →
picks `BoxDurOptimizedOrderingStrategy` by default (`cms.dispatcher.ordering=v2`).

`BoxDurOptimizedOrderingStrategy.sortReadyToCut`:

```
sort by:
  -cutMinutes desc (longest cut first)
  then dueDate asc
  then dueShift asc
  then dateFinMatelassage asc
  then sequence, serie
```

`sortWaiting` adds spreading time and treats LASER-DXF specially
(`max(spread, cut)` not `spread + cut`).

**That's the entire production ordering logic.** No material grouping, no
critical ratio, no machine balance check, no setup-time aggregation, no
sequence affinity, no horizon-aware decisions. The rich
`DispatchAlgorithms.AVAILABLE_ALGORITHMS = ["SCG","SPT","LPT","EDF","CR",
"WSPT","ATC","MATERIAL_GROUP"]` library exists but is never invoked by the
strategy.

### 2.5 The frontend (`ProcessDispatcher.js`)

- Polls `/api/dispatcher/engine/state` at **1s during run / 3s otherwise**.
- Polls `/api/zoneLoad/matrix` at 5s once liveCharge loads.
- Polls `/api/dispatcher/liveCharge` at 10s once liveCharge loads.
- WebSocket service exists on the backend (`DispatchEngineWebSocketService`,
  called from the optimizer at 6+ sites) but **the frontend does not subscribe
  to it**. The optimizer is publishing state/sample/suggestion events to
  `/topic/...` and no one is listening.
- Engine mode UI is hardcoded to "Alterné" (`DispatchEngineControl.js:104`)
  because the backend forces `EngineMode.ALTERNATING` regardless of input
  (`DispatchEngineController:141,166` and the service constructor at line
  306).

---

## 3. Where I disagree with — or expand on — Kimi's analysis

### Kimi got these right (verified)

| Claim | Verdict |
|---|---|
| ALTERNATING mode is forced regardless of request | ✓ — two places in the controller + one in the service |
| Ordonnancement phase is too weak | ✓ — it's not scheduling, it's biased dispatch |
| Monolithic 2,363-line service | ✓ — confirmed |
| File-based engine state with hand-rolled JSON parser | ✓ — `extractJsonString/Int/Double/StringMap` at lines 1995-2101 |
| Polling at 1s is heavier than needed | ✓ — and worse, the WebSocket pipe already exists and isn't used |
| `allowUnconfirmedZones` is dangerous in prod | ✓ — `DispatcherProperties.java:28`, defaults `false`, but a single property flip lands sequences in zones with zero up-machines |
| No convergence auto-stop | ✓ — `convergenceIterations=2000` exists in properties but is **not consulted by `doLoopIteration`**. The loop only exits on stopRequested or fixedDurationSec. |
| `zoneAcceptanceStatus` is a string | ✓ — typo-prone, case-sensitive |
| Race condition on publish (compute then write) | Partially — `@Transactional` guarantees commit atomicity but pre-commit, another tx can change a CR. In practice the engine's autoTick is the only competing writer and it diff-skips, so the window is small. But yes, locking would harden it. |

### Where Kimi was wrong, oversimplified, or missed nuance

**(a) `chooseZone` tiebreak direction.** Kimi said "Zone-A always wins over
Zone-B". Actually `b.getKey().compareTo(a.getKey())` is reverse-alpha — the
**last** zone alphabetically wins on ties. The bias is real, the direction is
backwards. Either way, the fix is the same (replace alpha tiebreak with
load-percentage tiebreak).

**(b) "Wire SCG into the ordonnancement phase".** Misframed. The
ordonnancement phase as written operates on **sequences**, not series. SCG
operates on series within a machine queue. Wiring SCG into
`doOrdonnancementIteration` makes no sense — the engine has no machine
assignment in its model. The right fix is bigger:

  - Either teach the engine to model machine-level assignment (so it can run
    real scheduling), or
  - Wire `DispatchAlgorithms` into `SeriesOrderingStrategy` so the
    per-machine queue that the operator sees uses the richer comparators.

Probably both, but they're independent.

**(c) Per-zone optimizer threads.** Kimi recommends per-STRICT-zone
optimizer threads. I'd push back — the current load is ~400 sequences, the
SA loop is in-memory, and `doOneIteration` runs in <100ms (logged at line
1325 if it exceeds). Splitting into N threads adds coordination overhead and
loses the global swap/rotate moves (which are valuable — they're how the
engine escapes local optima where two zones are stuck overloaded against
each other). I'd revisit this if and only if sequence count exceeds ~1.5k
**and** iteration time exceeds 200ms.

**(d) "WebSocket replaces polling".** The WebSocket infrastructure is
already there and being published to — it's the frontend that doesn't
subscribe. Kimi's framing implies the backend needs work; the work is on
the React side and is small.

**(e) Material availability hammering the stock API.** Kimi didn't flag it
but `MaterialAvailabilityChecker.check` calls
`StockStatusClient.getCurrentStockCount` which is an HTTP call. The
snapshot-scoped cache mitigates within a snapshot, but every snapshot
rebuild (every 10s in ordonnancement phase, every phase switch) re-issues
the request for every uncached (zone, refTissus) pair. On a cold restart
with 400 sequences this is potentially 400 × N_zones HTTP calls. Worth a
look.

**(f) `lookupEfficience` machine-type mapping is hardcoded.** Only "LASER",
"LASER-DXF", "LASER-LSR" map to "Laser" group; everything else falls into
"Coupe". A new machine type would silently inherit Coupe's efficience —
quiet drift. Move to a config table or domain enum.

**(g) `dueDateWeight` formula has a magic constant.** `1.0 + 9.0 /
(1.0 + days)` — with days=0 weight=10, with days=10 weight≈1.82. The "9.0"
isn't derived from anything stated. Make it tunable via `EngineProperties`.

**(h) Acceptance race with running engine.** When a chef accepts a
sequence (`setAcceptance` → `zoneAcceptanceStatus='ACCEPTED'`), the engine's
in-memory snapshot still treats it as movable until the next rebuild (≤10s
in ordonnancement phase, on phase switch otherwise). The engine could
propose a move *for* an already-accepted sequence during that window. The
publish path would no-op the bad move (the CR is now frozen) but the WS
suggestion event has already gone out. Add an event listener that flags the
sequence frozen in the candidates list immediately.

**(i) Greedy warm start is order-sensitive.** `greedyWarmStart()` places
candidates one at a time in iteration order (the order
`buildSnapshotInternal` populates the map). The first sequence's placement
locks in before later sequences are considered. A multi-pass or load-aware
order would be marginally better — sort candidates by load contribution
descending before greedy placement.

**(j) Convergence detection exists in config but isn't checked.** The
`Optimizer.convergenceIterations=2000` property is consulted nowhere in
`doLoopIteration`. The loop only exits on `stopRequested` or
`fixedDurationSec`. This is a latent feature, not a missing one — wire it
up in `~10 lines`.

---

## 4. My honest grading

| Area | Grade | Notes |
|---|---|---|
| `SequenceDispatcherService` (zone routing) | **A−** | Solid, well-factored, projections, audit trail. |
| `SerieZoneResolver` | **A−** | TTL caches, clean failure-reason API. |
| `LockResolver` | **A** | Pure function, clear contract, physical-reality-wins. |
| `MaterialAvailabilityChecker` | **B+** | Hard constraint enforced; stock-API call still a latent risk. |
| `EngineTickService` autoDispatch | **A−** | Fingerprint-skip is exactly the right move. |
| `ContinuousDispatchOptimizerService` dispatch phase | **B** | The SA + kick + swap + rotate is reasonable; size of the class is the problem. |
| `ContinuousDispatchOptimizerService` ordonnancement phase | **D** | Cosmetic — not real scheduling. **Most important thing to fix.** |
| `SeriesOrderingStrategy` chain (Delegating/Legacy/V2) | **C+** | V2 is OK, but ignores the richer `DispatchAlgorithms` library that exists. |
| `LiveChargeService` | **A−** | Engine overlay is elegant; 30s cache keeps it cheap. |
| `ProcessDispatcher.js` frontend | **C+** | Polling-heavy, WebSocket infrastructure unused, mode UI is a lie. |
| Config / feature-flag hygiene | **B** | Properties exist but several (`allowUnconfirmedZones`, `convergenceIterations`) need tighter defaults or wiring. |

---

## 5. Recommendations, prioritised

### P0 — Fix before public production rollout

1. **Decide what the "ordonnancement phase" is for, and either remove it or
   make it real.**
   Right now it tells the chef "the engine is doing scheduling" when it
   isn't. Either:
   - **Remove it** — run dispatch-only continuous mode (rename the engine
     "Dispatcher Optimizer", drop ALTERNATING). Honest and simpler.
   - **Make it real** — produce a per-machine schedule (start/end per serie)
     during the ordonnancement phase, optimize box-duration against that
     schedule. This is weeks of work, not days. Don't half-build it.

2. **Fix `chooseZone()` tiebreak** —
   `SequenceDispatcherService.java:488`. Replace alpha tiebreak with current
   zone load-percentage tiebreak (prefer the less-loaded zone). Quoting the
   relevant change site (also flagged by Kimi):
   ```java
   return b.getKey().compareTo(a.getKey()); // alpha tiebreak
   ```
   → load-aware tiebreak (needs a load lookup or a deterministic round-robin
   counter).

3. **Wire the `convergenceIterations` property to the loop exit** —
   `ContinuousDispatchOptimizerService.doLoopIteration`. Today the loop
   ignores it. Add: if `iteration - lastBestImprovementIteration >=
   convergenceIterations && temperature < TEMPERATURE_MIN * 2` → break.
   ~10 lines.

4. **Default `allowUnconfirmedZones=false` in prod profile** and add a UI
   banner when it's on. Cheap, removes a footgun.

5. **Force-stop the engine on `SequenceAcceptanceChangedEvent` for that
   sequence** — drop it from the candidates list or mark
   `possibleZones=[currentZone]` so the engine can't move it. Avoids
   publishing a suggestion for a sequence the chef just locked.

6. **Drop the file-based engine state** (`~/.mgcms/engine_best_state.json`
   + the hand-rolled JSON parser) and either (a) keep it in memory only
   (it's fine to lose best state across restarts — the next run finds it
   again in seconds), or (b) write to a `dispatch_engine_checkpoint` table
   serialised via Jackson. The hand-rolled parser is a security/maintenance
   liability for zero functional gain.

### P1 — Before scaling past ~700 sequences

7. **Wire `DispatchAlgorithms` into `SeriesOrderingStrategy`.** The library
   exists. Add a `cms.dispatcher.ordering.algorithm` property
   (default `SCG`), and have `DelegatingSeriesOrderingStrategy` resolve the
   comparator via `DispatchAlgorithms.get(algorithm, context)`. This gives
   you EDF / CR / WSPT / MATERIAL_GROUP without rewriting the strategy. It
   does require building a `DispatchContext` (cuttingTime lookup, started
   sequences, weights) — straightforward.

8. **Replace frontend polling with WebSocket subscription.** Optimizer
   already publishes `state`, `sample`, `suggestion` events on
   `/topic/dispatcher/engine`. Add a `SockJS + STOMP` client in
   `ProcessDispatcher.js` and demote polling to a 15-30s health-check
   fallback.

9. **Materialise `zoneAcceptanceStatus` as an enum** with JPA
   `@Enumerated(EnumType.STRING)`. Removes a class of string-compare bugs.

10. **Add a per-zone "pause" / "kill switch".** The state machine has
    IDLE/WARMING/IMPROVING/PAUSED/STOPPED at the **engine** level. Add a
    per-zone exclusion list (`pausedZones: Set<String>`) so a chef whose
    zone is misbehaving can be excluded without stopping the whole engine.

11. **Make engine SA constants configurable.**
    `TEMPERATURE_INITIAL / TEMPERATURE_COOLING / KICK_AFTER / KICK_MOVES /
    REBALANCE_EVERY / RANDOMIZE_AFTER / RANDOMIZE_FRACTION` are all
    `private static final` literals. Move them to `EngineProperties.Optimizer`
    so tuning doesn't require a redeploy.

12. **Make `lookupEfficience` mapping data-driven.** Right now hardcoded
    "LASER*"→Laser, everything else→Coupe. Add a `machine_type_groupe`
    column on the `MachineType` entity (or a config table) and read from
    there.

### P2 — Nice to have, not blocking

13. **Split `ContinuousDispatchOptimizerService` along seams that already
    exist** — `SnapshotBuilder`, `MoveOperators`, `ObjectiveFunction`,
    `StateMachine`, `Checkpointing`. The class is 2,363 lines and 70% of
    it is mechanical (snapshot, JSON IO, scoring). The actual SA loop is
    ~150 lines. Refactor for readability, not threading.

14. **Multi-pass greedy warm start.** Sort candidates by total
    `loadByType` sum descending before placing — large sequences placed
    first leave more room for fitting small ones.

15. **Stop using `Double.MAX_VALUE` as the "infeasible" sentinel in
    `evaluateMove`**. Use `Double.POSITIVE_INFINITY` consistently (you
    already do for material) and add an explicit `boolean feasible(...)`
    that returns reasons. Easier to debug "why didn't the engine pick X?".

16. **Add a metrics counter for `engine.candidates.unmovable` and
    `engine.candidates.material_blocked`** — surface in the UI so the
    chef knows when a sequence is stuck for a non-engine reason.

17. **Per-machine-type weighting in the objective is currently
    load-proportional** (`computeLoadSpread` weights each MT's spread by
    its share of total load). This is fine but means a low-volume MT with
    catastrophic imbalance contributes little to the score. Consider a
    floor weight (e.g. `max(loadShare, 0.1)`) so smaller types don't
    silently drift.

---

## 6. The "ordonnancement" decision — concrete options

This is the most important call. Three realistic paths:

| Option | Effort | Value | Risk |
|---|---|---|---|
| **A. Remove ordonnancement phase, keep dispatch-only continuous engine** | 1-2 days | High (honest UX) | Low — strictly fewer features |
| **B. Replace phase with `OrdonnancementService` SCG call to produce a per-machine queue, persist it as `MachineQueue` rows** | 1-2 weeks | Medium (better operator UX, no real cycle-time optim) | Medium — `MachineQueue` writes need careful concurrency |
| **C. Full scheduling: model serie-to-machine assignment in the engine, build a timeline, optimize box-duration KPI** | 6-8 weeks | High (matches the stated KPI) | High — touches every operator screen |

**My recommendation: A now, then B in Q3 if there's operator pull, defer C
unless box-duration optimisation becomes a measured KPI with a target.**
Don't build C speculatively — it's a big surface area for a metric you're
not yet tracking.

---

## 7. A working production checklist

Things I'd want green before flipping `mgcms.dispatcher.enabled=true` for
non-shadow plant use:

- [ ] Ordonnancement phase decision made (Option A/B/C above) and shipped.
- [ ] `convergenceIterations` wired up.
- [ ] `chooseZone` tiebreak load-aware.
- [ ] `allowUnconfirmedZones=false` in prod, banner when on.
- [ ] Engine respects mid-run `SequenceAcceptanceChangedEvent`.
- [ ] File-based state replaced (in-memory or table).
- [ ] Frontend on WebSocket (or polling demoted to ≥10s).
- [ ] `zoneAcceptanceStatus` as enum (or at least a normalisation helper
      called from every write site).
- [ ] Manual verification of: a chef accepting mid-run, a machine toggling
      offline mid-run, a rejected sequence, a sequence with missing
      material, a 24h stability run with at least 3 phase switches.

---

## 8. Questions I need answered before I'd commit to a specific
implementation

These are the things the code can't tell me. **Please answer the ones that
apply — your answers change which P0/P1 items I'd actually do first.**

### About scope and priorities

1. **Is box-duration the real KPI?** The cost function has `w1`/`w2`
   cycle-time terms defaulted to 0. If box-duration is what management
   measures the dispatcher against, the whole architecture needs to know
   about serie-to-machine assignment and timestamps — that's Option C. If
   it's "load spread within X%", the engine is already aimed at that and
   Option A is fine.

   => i want option C to have a fukk scheduling, the idea it to manage the load of on each zone by each type machine , and after having the best distrubution of sequence to the zones we can managed those sequences inside their zone and do a full flexible planningfor each machine on what to work next: we can get the value by external application to /api/public/next-series?machine=BB2&limit=3 like to get what to work next 3 series.

2. **What's the current sequence volume per shift in production?** ~400 has
   been mentioned a few times in the code/comments. If real volume is
   500-700, the current monolithic engine is fine. If you expect 1,500+ in
   the next 18 months, P1 #11 (configurable SA) and a split engine become
   pressing.
   
   => we dont surpass 100 sequence per shift and we dont surpass 1000 series per shift

3. **Are you willing to remove `EngineMode.ALTERNATING` (and the phase
   tracking) entirely if we go with Option A?** Or do you need to keep the
   enum/UI for a future re-introduction?

   => i want you to do the best option of option C

4. **How long is a typical run today — minutes, an hour, all day?** This
   decides whether convergence detection is high-value (long runs) or
   cosmetic (short runs that timer out before plateau).

   => my idea is to keep the engine running illimetely and he need to keep on eye on the production rectifying what it need to be , and planning each machine what it need to be working on . to have the greatest production 

### About operations

5. **Who else writes to `CuttingRequest.dispatchedZone` outside the
   engine?** The autoDispatchTick cron, the chef force/pull/pin/unpin
   endpoints, and the manual rebalance are what I found. Is there anything
   else (sync from ERP, manual SQL, a separate batch)?

   => for now i am not forcing it, i just supposed when a serie of a sequence is start to be cut in a machine then i bloque that sequence into that zone. if all serie are waiting, the engine then can dispatch it to any zone he see best fit

6. **What happens when the engine's `BEST_STATE_FILE` exists from a
   previous shift and the engine starts for today?** The file has a date
   check so it should be a no-op, but if the server is shared across
   environments or restored from a backup, stale state could load. Is
   that a real concern?

   => no it is not a concern

7. **Is the stock service (`StockStatusClient`) inside the same DC as the
   app, or is it a remote call across networks?** Influences how aggressive
   I'd be about caching beyond the per-snapshot scope.

   => the availability of stock material we just use it to alert , so that the production can tell the logiqtics team that we are out of material in production. FYI  get only the stock form the racks which is in the entity ScanRouleau

### About people

8. **What's the chef-de-zone's expectation when they accept a sequence
   while the engine is running?** Should the engine immediately recompute
   without that sequence, or should it finish the current iteration and
   pick up the change on rebuild? (Today it's the latter, with up to 10s
   lag.)

    => hould it finish the current iteration and
   pick up the change on rebuild

9. **Are operators routinely looking at the order of series inside a
   zone**, and if so what do they currently complain about? "Wrong order"
   bug reports would tell us if we need P1 #7 (wire DispatchAlgorithms)
   immediately or if it's a Q3 nice-to-have.

   => the operator have many complanes , first and the main one is that the some time they run out of what to give to a machine because of the machine Type ,like some time they have too much LEctra and less Lectra IP6 (that way we did the dispatching of sequence to balance the load) (FYI maybe we must not balance just by type accrow the zone but all must be balance, what is did was just balancing the the type machine accros the zones). and also the box keep getting stacked on the end of the zone because they have to many sequence running on paralle, that why we need to reduce the sequence time divided by it nuber of boxes.

10. **Do you want me to start implementing now, or do you want a
    deeper design doc on whichever Option (A/B/C) you pick before any
    code lands?**

    let go on deep dive on implementing the code (i chose option C)

---

## Appendix — file:line index for the claims in this doc

| Claim | Where to verify |
|---|---|
| ALTERNATING forced | `ContinuousDispatchOptimizerService.java:306`, `DispatchEngineController.java:141,166` |
| Ordonnancement is biased dispatch, not scheduling | `ContinuousDispatchOptimizerService.java:672-763` |
| `chooseZone` reverse-alpha tiebreak | `SequenceDispatcherService.java:488` |
| BoxDur strategy is the only one wired in | `OrdonnancementService.java:90, 2644, 2652` |
| `DispatchAlgorithms` exists but unused by strategy | `DispatchAlgorithms.java:21-23` vs `BoxDurOptimizedOrderingStrategy.java:44-78` |
| `convergenceIterations` defined but unused | `EngineProperties.java:64` (defined), no reference in `doLoopIteration` |
| File-based JSON state with hand-rolled parser | `ContinuousDispatchOptimizerService.java:1921-2101` |
| Frontend polls at 1s/3s/5s/10s | `ProcessDispatcher.js:280, 341, 346` |
| WebSocket service exists, frontend doesn't consume | `DispatchEngineWebSocketService` referenced 9+ times in optimizer; `ProcessDispatcher.js` has no SockJS/STOMP imports |
| `allowUnconfirmedZones` risk | `DispatcherProperties.java:28`, `SerieZoneResolver.java:200-202` |
| Material check hits external stock API | `MaterialAvailabilityChecker.java:104` |
| `lookupEfficience` hardcoded mapping | `ContinuousDispatchOptimizerService.java:2262-2275` |
| `dueDateWeight` magic constant 9.0 | `ContinuousDispatchOptimizerService.java:1614` |
| Acceptance event doesn't update running engine | `SequenceDispatcherService.setAcceptance` invalidates only the `OrdonnancementService` timeline cache, not the engine's candidate list |
