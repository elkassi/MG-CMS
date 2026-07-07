# 01-DISCOVERY — Places that order/sort series in the dispatcher path

## Dispatcher controller layer
- `DispatcherController.java:67-72` — `preview` endpoint triggers `SequenceDispatcherService.compute()` which builds `ZoneBreakdown` with sequences in `LinkedHashMap` insertion order (no explicit sort).
- `DispatcherController.java:88-98` — `publish` persists the same preview order.
- `DispatcherController.java:225-233` — `rebalance` clears non-pinned zones then re-runs `publish`.

## Dispatcher service layer
- `SequenceDispatcherService.java:306-328` — `compute()` groups series by sequence, resolves zone via `SerieZoneResolver`, then adds the sequence to `ZoneBreakdown` in request-load order (no per-zone sort).
- `EngineTickService.java:187-208` — `fingerprint()` sorts zone names and sequence lists alphabetically for diff-checking only; this does not affect runtime ordering.

## Ordonnancement / MachineQueue build layer
- `OrdonnancementService.java:2039-2084` — `getNextSeriesForMachine()` is the **primary ordering decision** for `MachineQueue`. It splits series into in-progress, ready-to-cut, and waiting, then sorts:
  - `readyToCut` by `dateFinMatelassage` ascending (line 2066-2067)
  - `waiting` by `planningDate` ascending (line 2075-2076)
- `OrdonnancementService.java:1984-2033` — `saveQueues()` calls `getNextSeriesForMachine()` for every machine and persists the first 3 to `machine_queue`.

## Timeline / recommendation layer
- `OrdonnancementService.java:328-329` — `getTimelineData()` sorts `notYet` by `dateDebutMatelassage` for display only (does not affect MachineQueue).
- `OrdonnancementService.java:957-959` — `buildTimelineBlocks()` sorts `notYet` by `dateDebutMatelassage` for collision-free slot assignment.
- `OrdonnancementService.java:1133-1138` — `waitingList` SCG sort for coupe scheduling: `-doneFraction`, `waitingCount`, `sequence`, `cuttingTime`.
- `OrdonnancementService.java:1360-1367` — `buildUnassignedSeries()` sorts by `statusMatelassage priority → dateDebutMatelassage → sequence → serie`.
- `OrdonnancementService.java:1507-1524` — `autoDispatch()` uses `DispatchAlgorithms.get(algorithm, ctx)` (SCG default) which sorts by `started? → planningDate → sequence → serie`.

## Continuous Optimization Engine
- `ContinuousDispatchOptimizerService.java:441-458` — `greedyWarmStart()` assigns sequences to zones to minimize load spread; it does NOT order series within a zone.
