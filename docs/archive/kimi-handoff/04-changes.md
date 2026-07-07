# 04-CHANGES

## New files
- `src/main/java/com/lear/MGCMS/services/dispatcher/OrderingProperties.java:1` — Feature-flag bean bound to `cms.dispatcher.*`.
- `src/main/java/com/lear/MGCMS/services/dispatcher/SeriesOrderingStrategy.java:1` — Interface contract for in-zone serie ordering.
- `src/main/java/com/lear/MGCMS/services/dispatcher/LegacySeriesOrderingStrategy.java:1` — Preserves original `dateFinMatelassage` / `planningDate` sorts.
- `src/main/java/com/lear/MGCMS/services/dispatcher/BoxDurOptimizedOrderingStrategy.java:1` — V2: longest-cut-first + LASER-DXF parallel awareness.
- `src/main/java/com/lear/MGCMS/services/dispatcher/DelegatingSeriesOrderingStrategy.java:1` — `@Primary` router that switches impl based on `OrderingProperties`.
- `src/test/java/com/lear/MGCMS/services/dispatcher/SeriesOrderingStrategyTest.java:1` — 10 JUnit tests (precedence, partitioning, ordering, delegator, legacy parity).

## Modified files
- `src/main/java/com/lear/MGCMS/services/OrdonnancementService.java:63` — Added `@Autowired SeriesOrderingStrategy seriesOrderingStrategy`.
- `src/main/java/com/lear/MGCMS/services/OrdonnancementService.java:1998` — `getNextSeriesForMachine` now passes `cuttingTimeMap`, `laserDxf`, `gerber` down.
- `src/main/java/com/lear/MGCMS/services/OrdonnancementService.java:2039-2084` — Replaced hard-coded `Comparator` sorts with `seriesOrderingStrategy.sortReadyToCut` / `sortWaiting`.
- `src/main/resources/application.properties:53` — Added `cms.dispatcher.ordering=v2`.
