# 00-SUMMARY

On the 50-serie test fixture, V2 pulls bottleneck series (cutting-time > 80 min) to the front of the machine queue; in a discrete-event simulation this reduces the critical-path completion time for sequences whose longest-cutting serie was previously buried behind short jobs.

## What changed
- New `SeriesOrderingStrategy` interface + 2 implementations (legacy dueDate-based sort, V2 boxDur-aware sort).
- V2 sorts ready-to-cut by cutting-time descending and waiting by total-processing-time descending, with LASER-DXF parallel-time awareness.
- Feature flag `cms.dispatcher.ordering=v2|legacy` wired into `OrdonnancementService.getNextSeriesForMachine`.
- 10 JUnit tests covering precedence, machine-type partitioning, structural ordering, delegator behaviour, and legacy zero-impact.

## Files touched
- `src/main/java/com/lear/MGCMS/services/dispatcher/OrderingProperties.java`
- `src/main/java/com/lear/MGCMS/services/dispatcher/SeriesOrderingStrategy.java`
- `src/main/java/com/lear/MGCMS/services/dispatcher/LegacySeriesOrderingStrategy.java`
- `src/main/java/com/lear/MGCMS/services/dispatcher/BoxDurOptimizedOrderingStrategy.java`
- `src/main/java/com/lear/MGCMS/services/dispatcher/DelegatingSeriesOrderingStrategy.java`
- `src/main/java/com/lear/MGCMS/services/OrdonnancementService.java`
- `src/main/resources/application.properties`
- `src/test/java/com/lear/MGCMS/services/dispatcher/SeriesOrderingStrategyTest.java`

## How to verify
1. `mvn -q compile` — zero errors.
2. `mvn -Dtest=SeriesOrderingStrategyTest test` — 10/10 pass.
3. `mvn -Dtest=SeriesOrderingStrategyTest,ContinuousDispatchOptimizerServiceTest,SchedulableSerieFilterTest,SerieZoneResolverTest test` — 16/16 pass.
4. Flip flag to `cms.dispatcher.ordering=legacy`, restart, hit `/api/ordonnancement/saveQueue` — queue order reverts to dueDate-based.

## Known risks
- V2 is a greedy per-machine heuristic; it does not guarantee global boxDur improvement on every instance (see 06-risks.md).
- `OrdonnancementService` now autowires `SeriesOrderingStrategy` (the `@Primary` delegator). If Spring context scanning is customised, verify the delegator bean is picked up.
