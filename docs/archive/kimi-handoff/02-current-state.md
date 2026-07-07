# 02-CURRENT-STATE — Optimization Engine & boxDur

## Current boxDur formula
```
boxDur(seq) = (max dateFinCoupe − min dateDebutMatelassage) / N_boxes
```
- `max dateFinCoupe`: latest cutting-end timestamp across all series in the sequence.
- `min dateDebutMatelassage`: earliest spreading-start timestamp across all series in the sequence.
- `N_boxes`: number of series (boxes) in the sequence.

## Where it is measured
- `PlanDeChargeService.java` computes per-serie `effectiveCuttingTime` and status grids, but does **not** compute aggregate `boxDur`.
- The KPI is implicit in the manufacturing floor: shorter boxDur = faster box completion = higher throughput.

## Current ordering is local-greedy
- `OrdonnancementService.getNextSeriesForMachine()` sorts per-machine, per-zone, without global sequence awareness.
- Ready-to-cut: `dateFinMatelassage` ascending (oldest first).
- Waiting: `planningDate` ascending (due-date first).
- This is **local-greedy** because it looks only at the individual serie's readiness or due date, not at the sequence's critical path.

## Optimization engine scope
- `ContinuousDispatchOptimizerService` optimizes **zone assignment** (which sequence goes to which STRICT zone) to minimize load spread `(max% − min%)` across `(machineType, zone)` pairs.
- It does **not** touch in-zone ordering or MachineQueue ordering.
- Engine runs in a background thread with reassign / swap / block-rotate moves.

## Integration point for this task
- The engine outputs `bestAssignment: sequence → zone`.
- `EngineTickService.autoDispatch()` publishes assignments and bumps `machine_queue.version`.
- The actual `MachineQueue` rows are built by `OrdonnancementService.saveQueues()`, which currently ignores the engine's zone awareness when ordering series within the queue.

## Gap
- Zone assignment is optimized globally (load balancing).
- In-zone ordering is still local-greedy (dueDate → dueShift → serie).
- The disconnect means a sequence can be assigned to the optimal zone but still have its bottleneck serie scheduled late on the machine.
