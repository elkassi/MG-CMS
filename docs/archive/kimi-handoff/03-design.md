# 03-DESIGN — Chosen Strategy: Box-Duration-Aware Ordering (V2)

## Alternative 1: Longest-Cutting-Time-First (LCTF)
- **Idea**: On each machine, schedule the serie with the longest cutting time first.
- **Pros**: Directly attacks the bottleneck that determines `max dateFinCoupe`.
- **Cons**: Ignores matelassage status; a serie with 2h cut but no finished spreading will block the machine.

## Alternative 2: Shortest-Matelassage-Tail-First (SMTF)
- **Idea**: Schedule series that will be ready to cut soonest (shortest remaining spreading time).
- **Pros**: Maximizes machine utilization by keeping the cutting pipeline full.
- **Cons**: Can delay long-cutting series, pushing out the sequence bottleneck.

## Winner: Hybrid V2 (Shortest-Ready-Longest-Cut-First)
- Combines the two: respect matelassage readiness first, then apply longest-cut-first within each readiness bucket.
- Ready-to-cut: sort by `cuttingTime descending` (bottleneck first), then `dateFinMatelassage`.
- Waiting: sort by `totalProcessingTime descending` (spread + cut for Lectra/Gerber, `max(spread,cut)` for LASER-DXF), then `planningDate`.
- **Why it wins**: It never blocks a ready machine on an unfinished spreading job, yet it always prioritises the serie most likely to determine the sequence's `max dateFinCoupe`.

## Pseudocode
```
function sortReadyToCut(series, machine):
    for each s in series:
        s.cutMinutes = resolveCuttingTime(s, machine)
    sort series by:
        - cutMinutes descending
        - dateFinMatelassage ascending
        - sequence asc, serie asc

function sortWaiting(series, machine):
    for each s in series:
        s.cutMinutes = resolveCuttingTime(s, machine)
        s.spreadMinutes = estimateSpreadingTime(s)
        if machine is LASER-DXF:
            s.totalMinutes = max(s.spreadMinutes, s.cutMinutes)
        else:
            s.totalMinutes = s.spreadMinutes + s.cutMinutes
    sort series by:
        - totalMinutes descending
        - planningDate ascending
        - sequence asc, serie asc
```

## Feature flag
- `cms.dispatcher.ordering=v2` (default) → `BoxDurOptimizedOrderingStrategy`
- `cms.dispatcher.ordering=legacy` → `LegacySeriesOrderingStrategy`
- Implemented via `@Primary DelegatingSeriesOrderingStrategy` so existing autowire sites need no change.

## No new dependencies
- Uses existing `CuttingTimeCalculator` bean (already in classpath).
- No pom.xml changes.
