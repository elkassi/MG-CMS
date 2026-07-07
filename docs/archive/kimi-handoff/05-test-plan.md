# 05-TEST-PLAN — Manual Verification

## Prerequisites
- App running on `localhost:8086` with `cms.dispatcher.ordering=v2` in `application.properties`.
- Authenticated user with `ROLE_PROCESS`.

## 1. Verify V2 is active (saveQueue ordering)
```bash
curl -X POST http://localhost:8086/api/ordonnancement/saveQueue \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT>"
```
**Expected**: `200 OK` with JSON containing `success: true` and `totalSaved > 0`.

## 2. Verify legacy fallback
1. Stop app.
2. Change `cms.dispatcher.ordering=legacy` in `application.properties`.
3. Restart app.
4. Re-run the same `curl`.
**Expected**: Same HTTP 200, but if you inspect `machine_queue` rows (e.g. via `/api/ordonnancement/queue/all`), the serie order should differ from the V2 run for machines that have mixed cutting times.

## 3. Verify dispatcher endpoints are unbroken
```bash
curl "http://localhost:8086/api/dispatcher/preview?date=2026-05-06&shift=1" \
  -H "Authorization: Bearer <JWT>"
```
**Expected**: `200 OK` with JSON containing `byZone` map and `unassignable` list.

```bash
curl -X POST "http://localhost:8086/api/dispatcher/publish?date=2026-05-06&shift=1" \
  -H "Authorization: Bearer <JWT>"
```
**Expected**: `200 OK` with same `byZone` shape.

```bash
curl -X POST "http://localhost:8086/api/dispatcher/rebalance?date=2026-05-06&shift=1" \
  -H "Authorization: Bearer <JWT>"
```
**Expected**: `200 OK` with rebalanced `byZone` shape; pinned sequences keep their zone.

## 4. Run automated tests
```bash
mvn -Dtest=SeriesOrderingStrategyTest test
```
**Expected**: `Tests run: 10, Failures: 0`.

```bash
mvn -Dtest=SeriesOrderingStrategyTest,ContinuousDispatchOptimizerServiceTest,SchedulableSerieFilterTest,SerieZoneResolverTest test
```
**Expected**: `Tests run: 16, Failures: 0`.
