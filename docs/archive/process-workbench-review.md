# Process Workbench (`/processWorkbench`) — Review & Improvement Plan

> Review date: 2026-05-31
> Scope: everything wired to the `/processWorkbench` route — frontend components, the
> `/api/workbench/*` backend, the cache + sequence-focus services, tests, and the spec docs.
> This is an analysis document only; no code was changed.

---

## 1. What `/processWorkbench` is

A single React route (`App.js:190`) that renders a tabbed "operation board" for the
process / chef-de-zone / logistics teams. It is the consolidation of three older
standalone screens (Plan de Charge, Process Dispatcher, Advanced Ordonnancement) plus
new sequence-focus and material-forecast views.

**Route guard** (`App.js:190`):
`ROLE_PROCESS, ROLE_CHEF_DE_ZONE, ROLE_CHEF_EQUIPE, ROLE_ADMIN, ROLE_VALID_QN_LOGISTIQUE`

### Tabs actually shipped (`Workbench.js:19`)

| Tab id | Label | Component |
|--------|-------|-----------|
| `sequenceFocus` (default) | Focus Séquence | `SequenceFocusView` |
| `shiftCompletion` | Shift Completion | `ShiftCompletionView` |
| `dispatching` | Dispatching of sequence | `DispatchingView` (from `ProcessDispatcher`) |
| `gantt` | Ordonnancement of serie | `GanttView` (from `AdvancedOrdonnancement`, read-only) |
| `materialForecast` | Prévision Matière | `MaterialForecastView` |

---

## 2. Architecture map

### Frontend (`src/main/js/components/Layout/`)

```
Workbench.js                 ← route component; tab state, engine action handlers
├─ WorkbenchProvider/Context ← single data fetch + adaptive polling (WorkbenchContext.js)
├─ WorkbenchHeader.js        ← date / shift / zone filter / engine badge / refresh / reload-all
├─ SequenceFocusView.js      ← default tab: chef + logistics cards per zone
├─ ShiftCompletionView.js
├─ ProcessDispatcher.js → DispatchingView
├─ AdvancedOrdonnancement.js → GanttView (readOnly, embedded)
└─ MaterialForecastView.js
```

### Backend (`src/main/java/com/lear/MGCMS/`)

```
controller/dispatcher/WorkbenchController.java   ← /api/workbench/{data,reload,validate-statuses}
services/dispatcher/WorkbenchCacheService.java   ← in-memory cache + incremental refresh + 3 scheduled loops
services/dispatcher/WorkbenchSequenceFocusService.java ← builds sequenceFocus payload
            (delegates to LiveChargeService, ZoneLoadService, OrdonnancementService,
             ContinuousDispatchOptimizerService, NonImportedChargeService, SerieStatusDateValidator)
```

### Endpoints

| Method | Path | Roles | Frontend caller |
|--------|------|-------|-----------------|
| GET | `/api/workbench/data?date=&shift=` | PROCESS, CHEF_DE_ZONE, CHEF_EQUIPE, ADMIN, VALID_QN_LOGISTIQUE | `WorkbenchContext.loadData` |
| POST | `/api/workbench/reload?date=&shift=` | PROCESS, CHEF_DE_ZONE, CHEF_EQUIPE, ADMIN | `WorkbenchContext.reloadAll` |
| POST | `/api/workbench/validate-statuses` | PROCESS, CHEF_DE_ZONE, CHEF_EQUIPE, ADMIN | **none (orphan)** |

The data payload is a large aggregate map: `liveCharge, zoneLoad, gantt, shiftCompletion,
activeSequences, series, stockRacks, nonImportedCharge, sequenceFocus, engineState, _cachedAt`.

---

## 3. What is done well (keep)

- **Single fetch + context fan-out.** One `/api/workbench/data` call feeds every tab; sections
  do not run their own polling loops (`WorkbenchContext.js`). This matches the perf guardrails in
  `process-workbench-sequence-focus-update.md`.
- **Adaptive polling.** 5 s while the engine runs, 15 s when idle (`WorkbenchContext.startPolling`).
- **In-flight guard + response de-dup.** `pendingRequest` prevents overlapping requests; the
  `_cachedAt` + engine-iteration check skips needless `setState` re-renders.
- **Incremental server cache.** `WorkbenchCacheService` reloads only added/removed/changed
  sequences, with TTL'd stock racks and a forced full rebuild every 10 cycles — and a
  `rebuildInProgress` set that prevents a thundering-herd of concurrent rebuilds on the same key.
- **Engine state is always fresh.** It is injected per-request and never cached, with client-side
  merging so best-metrics survive an IDLE/STOPPED transition.
- **Batched SQL + light projections.** `SQL_BATCH_SIZE` chunking and `*Light`/`*LightProjection`
  queries keep the heavy aggregation off the hot path.

---

## 4. Issues & improvement opportunities

Severity legend: 🔴 high · 🟠 medium · 🟡 low.

### 🔴 4.1 Cached map is mutated per-request → concurrency hazard

`WorkbenchCacheService.getData()` takes the map stored in the shared `cache` and mutates it on
every call before returning it for JSON serialization:

```java
// getData(), WorkbenchCacheService.java:131-136
Map<String, Object> engineState = buildEngineState();
data.put("engineState", engineState);                 // mutates the SHARED cached map
if (sequenceFocusService != null) {
    sequenceFocusService.updateEngineState(data.get("sequenceFocus"), engineState); // mutates cached sub-map
}
return data;                                            // ...then serialized by this thread
```

`reloadAll()` does the same (`:230` `data.put("_corrections", corrections)`). The maps are plain
`LinkedHashMap`s (not concurrent). Two HTTP worker threads hitting the same `date|shift` key will
`put` into the same map while a third serializes it → intermittent
`ConcurrentModificationException` during Jackson serialization, or a torn/lost `engineState`.
Hard to reproduce, ugly when it happens under multi-user polling.

**Fix:** build the response as a *shallow copy* before mutating:
`Map<String,Object> out = new LinkedHashMap<>(data); out.put("engineState", …); return out;`
(and copy the `sequenceFocus` sub-map before `updateEngineState`). Cheap relative to the rebuild cost.

### 🔴 / 🟠 4.2 Manual refresh can leave the header spinner stuck

`WorkbenchContext.loadData()` early-returns on the de-dup path **without clearing `loading`**:

```js
// WorkbenchContext.js:99-108
if (prevData && data && data._cachedAt === prevData._cachedAt) {
    const prevEng = prevData.engineState || {};
    const newEng = data.engineState || {};
    if (prevEng.iteration === newEng.iteration && prevEng.state === newEng.state) {
        this.startPolling(newEng);
        return;                      // loading was set true by refresh() and is never reset
    }
}
```

The header refresh button calls `refresh() → loadData(false)`, which sets `loading: true`. If the
engine is idle and the cache is unchanged (same `_cachedAt`, same iteration/state), the function
returns here and the spinner spins forever until something finally changes the payload.

**Fix:** reset `loading: false` on the de-dup branch (e.g. `this.setState({ loading: false });`
before `return`).

### 🟠 4.3 Deep-link `?section=` contract is broken

`Workbench.componentDidMount` only maps these sections (`Workbench.js:71-78`):
`focus, sequenceFocus, shiftCompletion, dispatching, gantt, materialForecast`.

But `WORKBENCH_SPEC.md §6`, the legacy redirects, and the saved test/inspection scripts still use
`?section=planDeCharge`, `?section=audit`, and `?section=material`:

- `planDeCharge` and `audit` are no longer tabs at all → silently fall back to the default tab.
- `material` is mismatched — the tab id is `materialForecast`, so `?section=material` also falls back.

Any old bookmark, dashboard link, or external deep-link lands on the wrong tab.

**Fix:** add aliases to the tab map (`material → materialForecast`, and decide where
`planDeCharge`/`audit` should resolve — likely `dispatching`/`gantt` or drop them), and update the
spec. Optionally persist the last tab in `localStorage` (the spec promised this; it is not implemented).

### 🟠 4.4 Heavy background work runs even with zero viewers

`WorkbenchCacheService.scheduledRefresh()` (`@Scheduled(fixedDelay=10000)`, `:487-528`) drives
**three** independent mutation loops regardless of whether anyone has the page open:

- production-progress normalization every 60 s (clears cache + `optimizerService.requestRebuild()`),
- `sequenceStatus` auto-correction every 5 min,
- **engine ground-up reload every 10 min** (`reloadActiveSnapshotFromGroundTruth()`),
- plus the current-slot cache refresh.

`process-workbench-sequence-focus-update.md` explicitly warns "the server has shown long-run
freezes during tests." Running a full engine ground-truth reload and cache rebuilds forever, with
no users connected, is exactly the kind of unbounded background load that contributes to that.

**Fix:** gate the expensive loops on recent activity — e.g. skip the rebuild / ground-up reload if
no `/api/workbench/data` request has arrived in the last N minutes (track `lastAccessMs`). Keep the
correctness-critical correction passes, but back them off when idle.

### 🟠 4.5 `WorkbenchCacheService` is a god-service

The class is named a *cache* but also owns: incremental diffing, production normalization,
`sequenceStatus` correction, engine ground-up reload, engine preload throttling, and error-response
shaping. ~730 lines, six collaborators, four `volatile` throttle timestamps. This concentrates a lot
of cross-cutting, production-critical behavior (status transitions, engine orchestration) behind a
name that implies it is harmless caching.

**Fix:** extract the scheduled correction/reload orchestration into a separate
`WorkbenchMaintenanceScheduler` (or move it onto the engine service it really belongs to). Leave
`WorkbenchCacheService` doing only cache build/serve/invalidate.

### 🟠 4.6 Test coverage gaps for a production-critical screen

- `WorkbenchControllerTest` covers `GET /data` happy path, 404 (disabled), 403 (role), 400 (bad
  shift) — but **not** `POST /reload`, **not** `/validate-statuses`, and it asserts only
  `liveCharge/zoneLoad/gantt/engineState`. The keys the UI actually depends on
  (`sequenceFocus`, `shiftCompletion`, `series`, `stockRacks`) are never asserted, so a regression
  in the aggregate shape passes CI.
- `WorkbenchSequenceFocusServiceTest` has a single scenario. No coverage for `SHORTAGE`/`NONE`
  material status, `RETURN_TO_STOCK` candidates, box-occupancy overflow, or the `JUST_ADDED` window.
- **No frontend Jest tests** for `Workbench`, `WorkbenchContext` (the polling/dedup/merge logic —
  see 4.2), or `SequenceFocusView`.

**Fix:** add a controller test asserting the full key set + `/reload`; add focus-service cases for
each material status; add a `WorkbenchContext` test for the de-dup/loading path.

### 🟡 4.7 Dead / orphaned code

| Item | Location | Note |
|------|----------|------|
| `handleReload()` + `reloadBusy` / `reloadMessage` state | `Workbench.js:41-65` | Defined, posts to `/api/dispatcher/engine/reload`, **never called** (no caller found). The visible reload button uses `reloadAll` from context instead. |
| `WorkbenchSection.js` | whole file | Leftover from the old collapsible-section design; **no importer** in `src/main/js`. |
| `POST /api/workbench/validate-statuses` | `WorkbenchController.java:76-86` | No frontend caller; orphan API surface. Confirm no external client before removing. |
| `engineDuration` / `onDurationChange` | `Workbench.js:171,193` | Collected and passed to `DispatchingView`, but `engineStart` always sends `durationSec: null` (`:113`), so the duration control is inert in the Workbench tab. |

**Fix:** delete the dead handlers/state/file (and the endpoint if truly unused) to cut maintenance noise.

### 🟡 4.8 Duplicated engine-merge logic (DRY)

The identical ~18-line "merge engineState + preserve best metrics" block is copy-pasted in
`loadData` (`WorkbenchContext.js:110-127`) and `reloadAll` (`:155-170`). Extract a
`mergeEngineState(prev, next)` helper so a fix to the merge rule only happens once.

### 🟡 4.9 Shift computation is forked between front and back

`computeCurrentShift()` (`WorkbenchContext.js:11-19`) re-derives the 21:50/05:50/13:50 boundaries
that the backend `ShiftClock` already owns. If plant shift boundaries ever change, these drift
apart silently. Prefer seeding the initial shift from a backend value (the payload already carries
`date`/`shift`) or a small `/api/.../currentSlot` rather than duplicating the clock.

### 🟡 4.10 Minor consistency / magic-number notes

- `SQL_BATCH_SIZE = 1000` here (`WorkbenchCacheService.java:51`) vs the documented convention of
  `2000` (`WORKBENCH_SPEC.md §9.2`). 1000 is safely under the SQL Server IN-clause limit, so this is
  a harmless inconsistency — but worth aligning or documenting why it differs.
- `maxBoxes = configuredMachines * 16` (`WorkbenchSequenceFocusService.java:339`): the "16 boxes per
  machine" capacity is a bare literal. Promote to a named constant (and ideally make it
  zone/machine-type configurable, since box capacity is a physical-floor parameter).
- `engineStateChanged()` (`:578-583`) mutates a **global** `lastCachedEngineState` and is read inside
  a short-circuit `||` (`getData :119`). When `isStale` is already true it is never evaluated, so the
  "changed" flag is unreliable, and being global it cross-talks between different `date|shift` viewers.
  At worst it triggers an occasional extra incremental refresh — low impact, but the logic is subtle
  enough to be worth tightening (make it per-key, evaluate it unconditionally).
- `StockIndex.zoneForLocation` (`:798-809`) falls back to an O(locations) substring scan for every
  rack whose location is not an exact match. With many racks × many zone locations this is
  quadratic-ish; fine today, watch it if stock volume grows.

### 🟡 4.11 `WORKBENCH_SPEC.md` is stale

The spec still describes the **old** design and no longer matches the code:

| Spec says | Reality |
|-----------|---------|
| Collapsible `WorkbenchSection` list with Plan de Charge / Audit / Dispatch Heatmap sections | Horizontal tab bar; no Plan de Charge or Audit tab; `WorkbenchSection.js` orphaned |
| Redirects `/planDeCharge → ?section=planDeCharge`, `/processDispatcher → ?section=dispatching`, `/advancedOrdonnancement → ?section=gantt` | Not implemented — those are still standalone legacy routes (`App.js:191,235,236`) |
| Aggregator returns `{ liveCharge, zoneLoad, gantt, engineState }` | Returns 11 keys incl. `sequenceFocus`, `shiftCompletion`, `series`, `stockRacks`, `nonImportedCharge` |
| Polling 10 s / 2 s | 15 s / 5 s |
| `localStorage` section persistence | Not implemented |

**Fix:** update `WORKBENCH_SPEC.md` (or mark it superseded by
`process-workbench-sequence-focus-update.md`, which reflects the current tab design).

---

## 5. Prioritized action list

| # | Action | Severity | Effort | File(s) |
|---|--------|----------|--------|---------|
| 1 | Copy the cached map before injecting `engineState`/`sequenceFocus` | 🔴 | S | `WorkbenchCacheService.java:131,230` |
| 2 | Reset `loading:false` on the de-dup early-return | 🔴/🟠 | XS | `WorkbenchContext.js:104` |
| 3 | Fix `?section=` aliases (`material→materialForecast`, planDeCharge/audit) + optional localStorage | 🟠 | S | `Workbench.js:71` |
| 4 | Gate scheduled rebuild / 10-min engine reload on recent page activity | 🟠 | M | `WorkbenchCacheService.java:487` |
| 5 | Add controller test for full payload keys + `/reload`; add `WorkbenchContext` Jest test | 🟠 | M | tests |
| 6 | Remove dead code: `handleReload`+state, `WorkbenchSection.js`, orphan endpoint | 🟡 | S | `Workbench.js`, `WorkbenchSection.js`, `WorkbenchController.java` |
| 7 | Extract `mergeEngineState` helper (DRY) | 🟡 | XS | `WorkbenchContext.js` |
| 8 | Split scheduled maintenance out of the cache service | 🟠 | M | `WorkbenchCacheService.java` |
| 9 | Name the `*16` box-capacity constant; align `SQL_BATCH_SIZE`; tighten `engineStateChanged` | 🟡 | S | services |
| 10 | Refresh `WORKBENCH_SPEC.md` to match shipped design | 🟡 | S | docs |

**Suggested first PR (low-risk, high-value):** items 1, 2, 6, 7 — all small, self-contained, and
they remove the two real defects plus the dead weight without touching the heavy aggregation paths.

---

## 6. Notes / assumptions

- I did not run the app or the test suite for this review; correctness claims are from reading the
  code paths. Items 4.1 and 4.2 are logic-level reasoning and would be worth confirming with a
  multi-user load test (4.1) and a manual "click refresh while engine idle" check (4.2).
- "No caller" / "orphan" claims (`handleReload`, `WorkbenchSection.js`, `/validate-statuses`) are
  based on a search of `src/main/js` and `src/main/java`; confirm there is no external/automation
  client before deleting the endpoint.
