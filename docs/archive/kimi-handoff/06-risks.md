# 06-RISKS

## What could regress
- `OrdonnancementService.saveQueues()` now depends on `SeriesOrderingStrategy` bean. If the Spring context excludes `com.lear.MGCMS.services.dispatcher` from component scanning, the bean will be missing and the app will fail to start.
- `getNextSeriesForMachine()` signature changed (added 3 params). Any external caller (there is none in-repo) would break.
- If `CuttingTimeCalculator.resolveMinutes()` throws for an unexpected placement/machineType combo, `BoxDurOptimizedOrderingStrategy` will propagate the exception during queue save.

## What's untested
- End-to-end boxDur measurement against real production data. The tests verify structural ordering properties (longest-cut-first) and delegator routing, not a full discrete-event simulation with real timing-model values.
- Performance at scale: with >500 series per shift, the per-machine sorting is still O(n log n), but the `resolveMinutes` call inside the comparator is pre-computed in a batch, so it should be fine.
- Interaction with chef pins and `EngineTickService.autoDispatch()`: pinned sequences are skipped by the dispatcher, but `saveQueues()` still builds queues from `tableCoupe`/`tableMatelassage` regardless of pin state. This is pre-existing behaviour, not changed by this PR.

## What's deferred
- Global boxDur optimisation across all machines simultaneously. V2 is per-machine greedy; a future iteration could replace it with a sequence-compaction solver that looks at the whole zone.
- Dynamic re-sorting mid-shift as matelassage completes. Today `saveQueues` is triggered manually or by cron; real-time re-sorting would require a listener on matelassage status changes.
- Integration with the Continuous Optimizer to feed boxDur as a secondary objective. The engine currently optimises load spread only.
